<?php
/**
 * Compilot AI Content Synchronization
 *
 * Syncs WordPress posts and pages to the knowledge graph automatically
 * via hooks and provides REST API endpoint for manual synchronization.
 */

if (!defined('ABSPATH')) {
    exit;
}

class Compilot_Content_Sync {

    private $jwt_token = null;
    private $jwt_expires_at = null;

    public function __construct() {
        // Register hooks for automatic sync
        add_action('save_post', array($this, 'sync_post_to_graph'), 10, 3);
        add_action('delete_post', array($this, 'delete_post_from_graph'));

        // Register REST API endpoint for manual sync
        add_action('rest_api_init', array($this, 'register_rest_routes'));

        // Register AJAX endpoints for admin bulk sync
        add_action('wp_ajax_compilot_bulk_sync', array($this, 'ajax_bulk_sync'));
        add_action('wp_ajax_compilot_process_next', array($this, 'ajax_process_next'));
        add_action('wp_ajax_compilot_sync_status', array($this, 'ajax_sync_status'));
        add_action('wp_ajax_compilot_background_sync', array($this, 'ajax_background_sync'));
        add_action('wp_ajax_compilot_clear_sync', array($this, 'ajax_clear_sync'));

        // Register WP-Cron action for background sync
        add_action('compilot_run_background_sync', array($this, 'cron_background_sync'));
    }

    /**
     * Get or refresh JWT authentication token
     * Caches the token until it expires, then automatically refreshes
     */
    private function get_auth_token() {
        $tenant_id = get_option('compilot_ai_tenant_id', '');
        $api_key = get_option('compilot_ai_api_key', '');

        if (empty($tenant_id) || empty($api_key)) {
            error_log('Compilot: Missing tenant ID or API key configuration');
            return null;
        }

        // Check if we have a valid cached token
        if ($this->jwt_token && $this->jwt_expires_at && time() < $this->jwt_expires_at - 60) {
            return $this->jwt_token;
        }

        // Try to get cached token from transient (survives across requests)
        $cached_token = get_transient('compilot_jwt_token');
        $cached_expires = get_transient('compilot_jwt_expires');

        if ($cached_token && $cached_expires && time() < $cached_expires - 60) {
            $this->jwt_token = $cached_token;
            $this->jwt_expires_at = $cached_expires;
            return $this->jwt_token;
        }

        // Need to authenticate and get a new token
        $api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');

        $response = wp_remote_post($api_url . '/api/auth/authenticate', array(
            'headers' => array('Content-Type' => 'application/json'),
            'body' => json_encode(array(
                'tenantId' => $tenant_id,
                'apiKey' => $api_key
            )),
            'timeout' => 30
        ));

        if (is_wp_error($response)) {
            error_log('Compilot: Authentication error: ' . $response->get_error_message());
            return null;
        }

        $status_code = wp_remote_retrieve_response_code($response);
        if ($status_code !== 200) {
            $body = wp_remote_retrieve_body($response);
            error_log('Compilot: Authentication failed (Status: ' . $status_code . '): ' . $body);
            return null;
        }

        $body = json_decode(wp_remote_retrieve_body($response), true);
        if (!isset($body['token'])) {
            error_log('Compilot: Authentication response missing token');
            return null;
        }

        // Cache the token
        $this->jwt_token = $body['token'];
        $this->jwt_expires_at = time() + $body['expiresIn'];

        // Store in transient for 24 hours (it will be refreshed before expiration)
        set_transient('compilot_jwt_token', $this->jwt_token, 24 * HOUR_IN_SECONDS);
        set_transient('compilot_jwt_expires', $this->jwt_expires_at, 24 * HOUR_IN_SECONDS);

        error_log('Compilot: Successfully authenticated, token expires in ' . $body['expiresIn'] . ' seconds');

        return $this->jwt_token;
    }

    /**
     * Prepare authentication headers with JWT Bearer token
     */
    private function get_auth_headers() {
        $token = $this->get_auth_token();
        if (!$token) {
            return array('Content-Type' => 'application/json');
        }

        return array(
            'Content-Type' => 'application/json',
            'Authorization' => 'Bearer ' . $token
        );
    }

    /**
     * Register REST API routes for content synchronization
     */
    public function register_rest_routes() {
        register_rest_route('compilot/v1', '/sync', array(
            'methods' => 'POST',
            'callback' => array($this, 'rest_sync_content'),
            'permission_callback' => function() {
                return current_user_can('manage_options');
            }
        ));

        register_rest_route('compilot/v1', '/sync/all', array(
            'methods' => 'POST',
            'callback' => array($this, 'rest_bulk_sync'),
            'permission_callback' => function() {
                return current_user_can('manage_options');
            }
        ));
    }

    /**
     * Sync a single post to the knowledge graph (triggered by WordPress hooks)
     * Uses non-blocking request so publishing isn't delayed
     */
    public function sync_post_to_graph($post_id, $post, $update) {
        // Check if auto-sync is enabled
        if (!get_option('compilot_auto_sync', 1)) {
            return;
        }

        // Skip autosaves, revisions, and drafts
        if (wp_is_post_autosave($post_id) ||
            wp_is_post_revision($post_id) ||
            $post->post_status !== 'publish') {
            return;
        }

        // Only sync posts and pages (can be extended)
        if (!in_array($post->post_type, array('post', 'page'))) {
            return;
        }

        // Use non-blocking sync for real-time publishing
        return $this->send_post_to_compilot_async($post);
    }

    /**
     * Send post to Compilot asynchronously (non-blocking)
     * Returns immediately without waiting for API response
     */
    private function send_post_to_compilot_async($post) {
        $api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');
        $data = $this->prepare_post_data($post);

        // Prepare headers with JWT Bearer authentication
        $headers = $this->get_auth_headers();

        // Fire non-blocking request - returns immediately
        wp_remote_post($api_url . '/api/ingest', array(
            'headers' => $headers,
            'body' => json_encode($data),
            'timeout' => 0.01,  // Minimal timeout
            'blocking' => false  // Don't wait for response
        ));

        error_log('Compilot async sync initiated for: ' . $post->post_title);
        return true;  // Return immediately
    }

    /**
     * Delete post from knowledge graph when deleted in WordPress
     */
    public function delete_post_from_graph($post_id) {
        $post = get_post($post_id);

        // Skip if not a post or page
        if (!$post || !in_array($post->post_type, array('post', 'page'))) {
            return;
        }

        $url = get_permalink($post_id);
        if (!$url) {
            error_log("Compilot: Could not get permalink for post {$post_id}");
            return;
        }

        $api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');

        // Prepare headers with JWT Bearer authentication
        $headers = $this->get_auth_headers();

        // Send delete request (non-blocking)
        wp_remote_request($api_url . '/api/ingest?' . http_build_query(array('url' => $url)), array(
            'method' => 'DELETE',
            'headers' => $headers,
            'timeout' => 0.01,
            'blocking' => false
        ));

        error_log("Compilot: Delete request sent for post {$post_id} ({$post->post_title})");
    }

    /**
     * Prepare post data for sending to Compilot
     */
    private function prepare_post_data($post) {
        // Get clean content (strip shortcodes, HTML, etc.)
        $content = wp_strip_all_tags(
            strip_shortcodes(
                apply_filters('the_content', $post->post_content)
            )
        );

        // Get excerpt or auto-generate from content
        $excerpt = $post->post_excerpt;
        if (empty($excerpt)) {
            $excerpt = wp_trim_words($content, 55, '...');
        }

        // Get category info (primary category)
        $category = null;
        $category_description = null;
        if ($post->post_type === 'post') {
            $category_objects = get_the_category($post->ID);
            if (!empty($category_objects)) {
                $primary_cat = $category_objects[0];
                $category = $primary_cat->name;
                $category_description = category_description($primary_cat->term_id);
                // Remove HTML tags from category description
                $category_description = wp_strip_all_tags($category_description);
            }
        }

        // Get tags
        $tags = array();
        if ($post->post_type === 'post') {
            $tag_objects = get_the_tags($post->ID);
            if ($tag_objects) {
                foreach ($tag_objects as $tag) {
                    $tags[] = $tag->name;
                }
            }
        }

        // Get author info
        $author_id = $post->post_author;
        $author_name = get_the_author_meta('display_name', $author_id);
        $author_bio = get_the_author_meta('description', $author_id);

        // Get related posts (in same category)
        $related_posts = array();
        if (!empty($category)) {
            $related_query = new WP_Query(array(
                'category_name' => $category,
                'posts_per_page' => 5,
                'post__not_in' => array($post->ID),
                'post_status' => 'publish'
            ));
            while ($related_query->have_posts()) {
                $related_query->the_post();
                $related_posts[] = get_permalink();
            }
            wp_reset_postdata();
        }

        // Build breadcrumb (for pages)
        $breadcrumb = null;
        if ($post->post_type === 'page' && $post->post_parent) {
            $ancestors = get_post_ancestors($post->ID);
            $breadcrumb_parts = array();
            foreach (array_reverse($ancestors) as $ancestor_id) {
                $breadcrumb_parts[] = get_the_title($ancestor_id);
            }
            $breadcrumb_parts[] = $post->post_title;
            $breadcrumb = implode(' > ', $breadcrumb_parts);
        }

        // Prepare data with enriched metadata
        $data = array(
            'title' => $post->post_title,
            'content' => $content,
            'url' => get_permalink($post->ID),
            'post_type' => $post->post_type,
            'date' => get_the_date('c', $post->ID), // ISO 8601 format

            // Site-level context
            'site_title' => get_bloginfo('name'),
            'site_tagline' => get_bloginfo('description'),

            // Content metadata
            'excerpt' => $excerpt,

            // Taxonomies
            'category' => $category,
            'category_description' => $category_description,
            'tags' => $tags,

            // Author info
            'author' => $author_name,
            'author_bio' => $author_bio,

            // Related content
            'related_posts' => $related_posts,

            // Navigation context
            'breadcrumb' => $breadcrumb
        );

        // Remove null values to reduce payload size
        return array_filter($data, function($value) {
            return $value !== null && $value !== '';
        });
    }

    /**
     * Poll job status until completion
     */
    private function poll_job_status($api_url, $job_id, $max_attempts = 60) {
        // Prepare headers with JWT Bearer authentication
        $headers = $this->get_auth_headers();

        $attempt = 0;
        while ($attempt < $max_attempts) {
            $response = wp_remote_get($api_url . '/api/ingest/status/' . $job_id, array(
                'headers' => $headers,
                'timeout' => 10
            ));

            if (is_wp_error($response)) {
                return array('status' => 'error', 'message' => $response->get_error_message());
            }

            $body = json_decode(wp_remote_retrieve_body($response), true);

            if (isset($body['status'])) {
                if ($body['status'] === 'completed') {
                    return array('status' => 'success', 'data' => $body);
                } elseif ($body['status'] === 'failed') {
                    return array('status' => 'failed', 'error' => $body['error'] ?? 'Unknown error');
                }
                // Still processing, wait and retry
            }

            sleep(2);  // Wait 2 seconds before next poll
            $attempt++;
        }

        return array('status' => 'timeout', 'message' => 'Job status check timed out');
    }

    /**
     * Send post data to Compilot API and wait for completion (sequential processing)
     */
    private function send_post_to_graphiti($post) {
        $api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');
        $data = $this->prepare_post_data($post);

        error_log('Compilot: Sending POST to ' . $api_url . '/api/ingest for post: ' . $post->post_title);

        // Prepare headers with JWT Bearer authentication
        $headers = $this->get_auth_headers();

        // Send ingest request with extended timeout for ingestion to complete
        $response = wp_remote_post($api_url . '/api/ingest', array(
            'headers' => $headers,
            'body' => json_encode($data),
            'timeout' => 60  // 60 seconds should be enough for most posts
        ));

        if (is_wp_error($response)) {
            $error_msg = 'Compilot sync error for "' . $post->post_title . '": ' . $response->get_error_message();
            error_log($error_msg);
            return false;
        }

        $status_code = wp_remote_retrieve_response_code($response);
        $body_raw = wp_remote_retrieve_body($response);
        $body = json_decode($body_raw, true);

        // Check if job was accepted
        if ($status_code !== 200) {
            error_log('Compilot sync failed for "' . $post->post_title . '" (Status: ' . $status_code . '): ' . $body_raw);
            return false;
        }

        $task_id = $body['task_id'] ?? $body['job_id'] ?? 'unknown';
        error_log('Compilot sync started for "' . $post->post_title . '" (Task: ' . $task_id . ')');

        // Poll for completion with short intervals
        $max_attempts = 30;  // 30 attempts * 2 seconds = 60 seconds max
        $attempt = 0;

        while ($attempt < $max_attempts) {
            sleep(2);  // Wait 2 seconds between checks
            $attempt++;

            $status_response = wp_remote_get($api_url . '/api/ingest/status/' . $task_id, array(
                'headers' => $headers,
                'timeout' => 10
            ));

            if (is_wp_error($status_response)) {
                error_log('Compilot: Status check error for ' . $post->post_title . ': ' . $status_response->get_error_message());
                continue;  // Try again
            }

            $status_body = json_decode(wp_remote_retrieve_body($status_response), true);

            if (isset($status_body['status'])) {
                if ($status_body['status'] === 'completed') {
                    error_log('Compilot: Successfully completed sync for: ' . $post->post_title);
                    return true;
                } elseif ($status_body['status'] === 'failed') {
                    $error = $status_body['error'] ?? 'Unknown error';
                    error_log('Compilot: Sync failed for "' . $post->post_title . '": ' . $error);
                    return false;
                }
                // Still processing, continue polling
            }
        }

        // Timeout
        error_log('Compilot: Sync timeout for "' . $post->post_title . '" after ' . ($max_attempts * 2) . ' seconds');
        return false;
    }

    /**
     * REST API endpoint: Sync a single post
     */
    public function rest_sync_content($request) {
        $post_id = $request->get_param('post_id');

        if (!$post_id) {
            return new WP_REST_Response(array(
                'success' => false,
                'message' => 'Post ID is required'
            ), 400);
        }

        $post = get_post($post_id);
        if (!$post) {
            return new WP_REST_Response(array(
                'success' => false,
                'message' => 'Post not found'
            ), 404);
        }

        $result = $this->send_post_to_graphiti($post);

        return new WP_REST_Response(array(
            'success' => $result,
            'message' => $result ? 'Post synced successfully' : 'Failed to sync post'
        ), $result ? 200 : 500);
    }

    /**
     * REST API endpoint: Bulk sync all published posts/pages
     */
    public function rest_bulk_sync($request) {
        $args = array(
            'post_type' => array('post', 'page'),
            'post_status' => 'publish',
            'posts_per_page' => -1,
            'orderby' => 'modified',
            'order' => 'DESC'
        );

        $posts = get_posts($args);
        $success_count = 0;
        $fail_count = 0;

        foreach ($posts as $post) {
            if ($this->send_post_to_graphiti($post)) {
                $success_count++;
            } else {
                $fail_count++;
            }

            // Small delay to avoid overwhelming the API
            usleep(100000); // 0.1 second
        }

        return new WP_REST_Response(array(
            'success' => true,
            'message' => sprintf(
                'Synced %d posts successfully, %d failed',
                $success_count,
                $fail_count
            ),
            'total' => count($posts),
            'success_count' => $success_count,
            'fail_count' => $fail_count
        ), 200);
    }

    /**
     * Get or create sync job ID
     */
    private function get_sync_job_id() {
        return 'compilot_sync_' . time();
    }

    /**
     * Get sync status from transient
     */
    private function get_sync_status($job_id = null) {
        if (!$job_id) {
            $job_id = get_transient('compilot_current_sync_job');
        }
        if (!$job_id) {
            return null;
        }
        return get_transient($job_id);
    }

    /**
     * Update sync status in transient
     */
    private function update_sync_status($job_id, $data) {
        set_transient($job_id, $data, HOUR_IN_SECONDS);
        set_transient('compilot_current_sync_job', $job_id, HOUR_IN_SECONDS);
    }

    /**
     * Clear sync status
     */
    private function clear_sync_status($job_id) {
        delete_transient($job_id);
        delete_transient('compilot_current_sync_job');
    }

    /**
     * AJAX handler to create sync job with queue
     * This just creates the queue and returns immediately - no long processing
     */
    public function ajax_bulk_sync() {
        error_log('Compilot: ajax_bulk_sync called');
        check_ajax_referer('compilot-ai-nonce', 'nonce');

        if (!current_user_can('manage_options')) {
            error_log('Compilot: ajax_bulk_sync - Insufficient permissions');
            wp_send_json_error(array('message' => 'Insufficient permissions'));
        }

        // Check if sync is already running
        $current_job = get_transient('compilot_current_sync_job');
        if ($current_job) {
            $status = $this->get_sync_status($current_job);
            if ($status && $status['status'] === 'running') {
                error_log('Compilot: ajax_bulk_sync - Sync already running: ' . $current_job);
                wp_send_json_error(array(
                    'message' => 'Sync is already running',
                    'job_id' => $current_job
                ));
            }
        }

        // Create new sync job
        $job_id = $this->get_sync_job_id();
        error_log('Compilot: ajax_bulk_sync - Created job ID: ' . $job_id);

        // Get all posts to sync
        $args = array(
            'post_type' => array('post', 'page'),
            'post_status' => 'publish',
            'posts_per_page' => -1,
            'fields' => 'ids'  // Only get IDs for efficiency
        );
        $post_ids = get_posts($args);
        $total = count($post_ids);

        error_log('Compilot: Created sync queue with ' . $total . ' posts');

        // Initialize sync job with queue
        $this->update_sync_status($job_id, array(
            'status' => 'running',
            'queue' => $post_ids,  // Queue of post IDs to process
            'total' => $total,
            'processed' => 0,
            'success' => 0,
            'failed' => 0,
            'failed_posts' => array(),  // Track failed posts
            'started' => current_time('mysql'),
            'current_post' => null
        ));

        // Return immediately - JavaScript will process posts one by one
        wp_send_json_success(array(
            'message' => 'Sync queue created',
            'job_id' => $job_id,
            'total' => $total
        ));
    }

    /**
     * AJAX handler to process next post in queue
     * Processes ONE post and returns immediately
     */
    public function ajax_process_next() {
        check_ajax_referer('compilot-ai-nonce', 'nonce');

        if (!current_user_can('manage_options')) {
            wp_send_json_error(array('message' => 'Insufficient permissions'));
        }

        $job_id = isset($_POST['job_id']) ? sanitize_text_field($_POST['job_id']) : null;
        if (!$job_id) {
            wp_send_json_error(array('message' => 'Job ID required'));
        }

        $status = $this->get_sync_status($job_id);
        if (!$status) {
            wp_send_json_error(array('message' => 'Job not found'));
        }

        // Check if queue is empty
        if (empty($status['queue'])) {
            // All posts processed - mark as complete
            $status['status'] = 'completed';
            $status['completed'] = current_time('mysql');
            $status['current_post'] = null;

            $message = sprintf(
                'Sync complete! Successfully synced %d of %d posts.',
                $status['success'],
                $status['total']
            );

            if ($status['failed'] > 0) {
                $message .= sprintf(' %d failed.', $status['failed']);
            }

            $status['message'] = $message;
            $this->update_sync_status($job_id, $status);

            error_log('Compilot: Sync job completed - ' . $message);

            // Return status data directly (not nested in 'data')
            $status['status'] = 'completed';
            $status['message'] = $message;
            wp_send_json_success($status);
        }

        // Get next post ID from queue
        $post_id = array_shift($status['queue']);  // Remove first item
        $post = get_post($post_id);

        if (!$post) {
            error_log('Compilot: Post not found: ' . $post_id);
            // Skip this post and continue
            $status['failed']++;
            $status['processed']++;
            $status['failed_posts'][] = array(
                'id' => $post_id,
                'title' => 'Unknown (Post ' . $post_id . ')',
                'error' => 'Post not found'
            );
            $this->update_sync_status($job_id, $status);

            // Return status data directly
            $status['status'] = 'next';
            $status['message'] = 'Post not found, continuing...';
            wp_send_json_success($status);
        }

        error_log('Compilot: Processing post ' . ($status['processed'] + 1) . '/' . $status['total'] . ': ' . $post->post_title);

        // Update current post info
        $status['current_post'] = $post->post_title;
        $this->update_sync_status($job_id, $status);

        // Process this ONE post
        $success = $this->send_post_to_graphiti($post);

        // Update status based on result
        $status['processed']++;
        if ($success) {
            $status['success']++;
            error_log('Compilot: Successfully synced post: ' . $post->post_title);
        } else {
            $status['failed']++;
            $status['failed_posts'][] = array(
                'id' => $post->ID,
                'title' => $post->post_title,
                'url' => get_permalink($post->ID),
                'error' => 'Sync failed'
            );
            error_log('Compilot: Failed to sync post: ' . $post->post_title);
        }

        $this->update_sync_status($job_id, $status);

        // Return status data directly for next iteration
        $status['status'] = 'next';
        $status['message'] = 'Post processed';
        wp_send_json_success($status);
    }

    /**
     * WP-Cron handler for background sync
     */
    public function cron_background_sync($job_id) {
        error_log("Compilot: Cron background sync worker started for job: {$job_id}");

        // Increase time limit for background processing
        set_time_limit(0);
        ignore_user_abort(true);

        $this->run_background_sync($job_id);
    }

    /**
     * AJAX handler for background sync worker (legacy - kept for compatibility)
     */
    public function ajax_background_sync() {
        error_log("Compilot: AJAX background sync worker started");

        $job_id = isset($_POST['job_id']) ? sanitize_text_field($_POST['job_id']) : '';

        if (!$job_id || !check_ajax_referer('graphiti-bg-sync-' . $job_id, 'nonce', false)) {
            error_log("Compilot: Background sync - Invalid request or nonce check failed");
            wp_die('Invalid request');
        }

        error_log("Compilot: Background sync - Job ID: {$job_id}");

        // Increase time limit for background processing
        set_time_limit(0);
        ignore_user_abort(true);

        $this->run_background_sync($job_id);

        wp_die();
    }

    /**
     * Run the actual background sync process
     */
    private function run_background_sync($job_id) {
        error_log("Compilot: run_background_sync executing for job: {$job_id}");

        // Increase time limit for large sites
        set_time_limit(0);
        ignore_user_abort(true);

        $args = array(
            'post_type' => array('post', 'page'),
            'post_status' => 'publish',
            'posts_per_page' => -1
        );

        $posts = get_posts($args);
        $total = count($posts);
        $success = 0;
        $failed = 0;
        $failed_posts = array();  // Track which posts failed

        error_log("Compilot: Background sync - Found {$total} posts to sync");

        foreach ($posts as $index => $post) {
            // Update current status BEFORE processing
            $this->update_sync_status($job_id, array(
                'status' => 'running',
                'total' => $total,
                'processed' => $success + $failed,
                'success' => $success,
                'failed' => $failed,
                'started' => current_time('mysql'),
                'current_post' => $post->post_title
            ));

            $current_number = $index + 1;
            error_log("Compilot: Processing post {$current_number}/{$total}: {$post->post_title}");

            if ($this->send_post_to_graphiti($post)) {
                $success++;
                error_log("Compilot: Successfully synced: {$post->post_title}");
            } else {
                $failed++;
                $failed_posts[] = array(
                    'title' => $post->post_title,
                    'id' => $post->ID,
                    'url' => get_permalink($post->ID)
                );
                error_log("Compilot: Failed to sync: {$post->post_title}");
            }

            // Update status AFTER processing so progress bar reflects completion
            $this->update_sync_status($job_id, array(
                'status' => 'running',
                'total' => $total,
                'processed' => $success + $failed,
                'success' => $success,
                'failed' => $failed,
                'started' => current_time('mysql'),
                'current_post' => ($success + $failed) < $total ? 'Processing next post...' : 'Finalizing...'
            ));
        }

        // Build completion message
        $message = sprintf(
            'Sync complete: %d successful, %d failed out of %d total',
            $success,
            $failed,
            $total
        );

        // Add failed posts details if any
        if (!empty($failed_posts)) {
            $message .= "\n\nFailed posts:\n";
            foreach ($failed_posts as $failed_post) {
                $message .= "- {$failed_post['title']} (ID: {$failed_post['id']})\n";
            }
        }

        // Mark as complete
        $this->update_sync_status($job_id, array(
            'status' => 'completed',
            'total' => $total,
            'processed' => $total,
            'success' => $success,
            'failed' => $failed,
            'failed_posts' => $failed_posts,  // Include failed posts list
            'started' => current_time('mysql'),
            'completed' => current_time('mysql'),
            'message' => $message
        ));

        error_log("Compilot: Background sync completed for job: {$job_id}");
    }

    /**
     * AJAX handler to check sync status
     */
    public function ajax_sync_status() {
        // Close session immediately to avoid blocking concurrent requests
        if (session_status() === PHP_SESSION_ACTIVE) {
            session_write_close();
        }

        check_ajax_referer('compilot-ai-nonce', 'nonce');

        if (!current_user_can('manage_options')) {
            wp_send_json_error(array('message' => 'Insufficient permissions'));
        }

        $job_id = isset($_POST['job_id']) ? sanitize_text_field($_POST['job_id']) : null;
        $status = $this->get_sync_status($job_id);

        if (!$status) {
            wp_send_json_error(array('message' => 'No sync job found'));
        }

        // Auto-clear stuck jobs older than 5 minutes
        if ($status['status'] === 'running' && isset($status['started'])) {
            $started_time = strtotime($status['started']);
            $now = current_time('timestamp');
            $elapsed = $now - $started_time;

            if ($elapsed > 300) { // 5 minutes
                error_log("Compilot: Clearing stuck sync job (running for {$elapsed} seconds)");
                $status['status'] = 'failed';
                $status['error'] = 'Sync job timed out after 5 minutes';
                $this->update_sync_status($job_id ?: get_transient('compilot_current_sync_job'), $status);
            }
        }

        wp_send_json_success($status);
    }

    /**
     * AJAX handler to clear sync status
     */
    public function ajax_clear_sync() {
        check_ajax_referer('compilot-ai-nonce', 'nonce');

        if (!current_user_can('manage_options')) {
            wp_send_json_error(array('message' => 'Insufficient permissions'));
        }

        // Get current job ID
        $job_id = get_transient('compilot_current_sync_job');

        if ($job_id) {
            // Delete the job status
            delete_transient($job_id);
            delete_transient('compilot_current_sync_job');
            error_log("Compilot: Manually cleared sync job: {$job_id}");
        }

        wp_send_json_success(array('message' => 'Sync status cleared'));
    }
}

// Initialize content sync
new Compilot_Content_Sync();
