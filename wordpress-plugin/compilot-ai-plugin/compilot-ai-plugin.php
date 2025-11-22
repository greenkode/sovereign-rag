<?php
/**
 * Plugin Name: Compilot AI Assistant
 * Plugin URI: https://github.com/compilot/compilot-ai
 * Description: Interface to query and search a knowledge graph powered by LangChain4j with AI-powered chat widget.
 * Version: 2.0.14
 * Author: Compilot
 * Author URI: https://compilot.ai
 * License: GPL v2 or later
 * License URI: https://www.gnu.org/licenses/gpl-2.0.html
 * Text Domain: compilot-ai
 */

// Prevent direct access
if (!defined('ABSPATH')) {
    exit;
}

// Define plugin constants
define('COMPILOT_AI_VERSION', '2.0.14');  // Fixed inactivity prompt button handlers
define('COMPILOT_AI_PLUGIN_DIR', plugin_dir_path(__FILE__));
define('COMPILOT_AI_PLUGIN_URL', plugin_dir_url(__FILE__));

/**
 * Main plugin class
 */
class Compilot_AI_Plugin {

    private static $instance = null;
    private $api_url = null;

    public static function get_instance() {
        if (null === self::$instance) {
            self::$instance = new self();
        }
        return self::$instance;
    }

    private function __construct() {
        // Cache API URL
        $this->api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');

        // Initialize plugin
        add_action('init', array($this, 'init'));
        add_action('admin_menu', array($this, 'add_admin_menu'));
        add_action('admin_enqueue_scripts', array($this, 'enqueue_admin_scripts'));
        add_action('wp_enqueue_scripts', array($this, 'enqueue_frontend_scripts'));

        // Update cached API URL when settings are saved
        add_action('update_option_compilot_ai_api_url', array($this, 'update_cached_api_url'), 10, 2);

        // AJAX handlers
        add_action('wp_ajax_compilot_search', array($this, 'ajax_search'));
        add_action('wp_ajax_nopriv_compilot_search', array($this, 'ajax_search'));
        add_action('wp_ajax_update_api_key', array($this, 'ajax_update_api_key'));
        add_action('wp_ajax_compilot_get_jwt_token', array($this, 'ajax_get_jwt_token'));
        add_action('wp_ajax_compilot_test_connection', array($this, 'ajax_test_connection'));

        // Shortcodes
        add_shortcode('compilot_search', array($this, 'search_shortcode'));
        add_shortcode('compilot_submit', array($this, 'submit_shortcode'));

        // Activation/Deactivation hooks
        register_activation_hook(__FILE__, array($this, 'activate'));
        register_deactivation_hook(__FILE__, array($this, 'deactivate'));
    }

    public function init() {
        // Plugin initialization
        load_plugin_textdomain('compilot-ai', false, dirname(plugin_basename(__FILE__)) . '/languages');

        // Load chat widget
        if (get_option('compilot_enable_chat_widget', true)) {
            require_once COMPILOT_AI_PLUGIN_DIR . 'includes/chat-widget.php';
        }

        // Load content synchronization
        require_once COMPILOT_AI_PLUGIN_DIR . 'includes/content-sync.php';
    }

    public function activate() {
        // Set default options
        add_option('compilot_ai_api_url', 'http://localhost:8000');
        add_option('compilot_ai_tenant_id', 'dev');  // Default development tenant
        add_option('compilot_ai_api_key', 'dev-api-key-12345');    // Default development API key
        add_option('compilot_ai_min_confidence', 0.5);
        add_option('compilot_ai_low_confidence_threshold', 0.5);
        add_option('compilot_enable_chat_widget', true);
        add_option('compilot_auto_sync', 1);  // Enable automatic content sync by default
        add_option('compilot_chat_greeting_hint', 'Begin met typen om suggesties te zien...');

        // RAG settings defaults
        add_option('compilot_ai_enable_general_knowledge', true);
        add_option('compilot_ai_show_gk_disclaimer', false);
        add_option('compilot_ai_gk_disclaimer_text', '*Let op: Dit antwoord is gebaseerd op algemene kennis, niet uit onze kennisbank.*');
        add_option('compilot_ai_rag_model', 'llama3.1');  // Default AI model
        add_option('compilot_ai_rag_persona', 'customer_service');  // Default persona
        add_option('compilot_ai_show_sources', true);  // Show source citations by default

        flush_rewrite_rules();
    }

    public function deactivate() {
        flush_rewrite_rules();
    }

    /**
     * Update cached API URL when option changes
     */
    public function update_cached_api_url($old_value, $new_value) {
        $this->api_url = $new_value;
    }

    public function add_admin_menu() {
        add_menu_page(
            __('Compilot AI Assistant', 'compilot-ai'),
            __('Compilot AI Assistant', 'compilot-ai'),
            'manage_options',
            'compilot-ai',
            array($this, 'admin_page'),
            'dashicons-networking',
            30
        );

        add_submenu_page(
            'compilot-ai',
            __('Settings', 'compilot-ai'),
            __('Settings', 'compilot-ai'),
            'manage_options',
            'compilot-ai-settings',
            array($this, 'settings_page')
        );

        add_submenu_page(
            'compilot-ai',
            __('Review & Add Content', 'compilot-ai'),
            __('Review & Add Content', 'compilot-ai'),
            'manage_options',
            'compilot-ai-review',
            array($this, 'review_page')
        );
    }

    public function enqueue_admin_scripts($hook) {
        if (strpos($hook, 'compilot-ai') === false) {
            return;
        }

        wp_enqueue_style(
            'compilot-ai-admin',
            COMPILOT_AI_PLUGIN_URL . 'assets/css/admin.css',
            array(),
            COMPILOT_AI_VERSION
        );

        // Enqueue Marked.js for Markdown rendering
        wp_enqueue_script(
            'marked-js',
            'https://cdn.jsdelivr.net/npm/marked@11.1.0/marked.min.js',
            array(),
            '11.1.0',
            true
        );

        wp_enqueue_script(
            'compilot-ai-admin',
            COMPILOT_AI_PLUGIN_URL . 'assets/js/admin.js',
            array('jquery', 'marked-js'),
            COMPILOT_AI_VERSION,
            true
        );

        wp_localize_script('compilot-ai-admin', 'compilotAI', array(
            'ajax_url' => admin_url('admin-ajax.php'),
            'nonce' => wp_create_nonce('compilot-ai-nonce'),
            'api_url' => get_option('compilot_ai_api_url', 'http://localhost:8000'),
            'tenantId' => get_option('compilot_ai_tenant_id', ''),
            'apiKey' => get_option('compilot_ai_api_key', '')
        ));
    }

    public function enqueue_frontend_scripts() {
        wp_enqueue_style(
            'compilot-ai-frontend',
            COMPILOT_AI_PLUGIN_URL . 'assets/css/frontend.css',
            array(),
            COMPILOT_AI_VERSION
        );

        wp_enqueue_script(
            'compilot-ai-frontend',
            COMPILOT_AI_PLUGIN_URL . 'assets/js/frontend.js',
            array('jquery'),
            COMPILOT_AI_VERSION,
            true
        );

        wp_localize_script('compilot-ai-frontend', 'compilotAI', array(
            'ajax_url' => admin_url('admin-ajax.php'),
            'nonce' => wp_create_nonce('compilot-ai-nonce'),
            'api_url' => get_option('compilot_ai_api_url', 'http://localhost:8000'),
            'tenantId' => get_option('compilot_ai_tenant_id', ''),
            'apiKey' => get_option('compilot_ai_api_key', '')
        ));
    }

    public function admin_page() {
        include COMPILOT_AI_PLUGIN_DIR . 'templates/admin-page.php';
    }

    public function settings_page() {
        include COMPILOT_AI_PLUGIN_DIR . 'templates/settings-page.php';
    }

    public function review_page() {
        include COMPILOT_AI_PLUGIN_DIR . 'templates/review-page.php';
    }

    // AJAX Handlers

    public function ajax_update_api_key() {
        check_ajax_referer('compilot-ai-nonce', '_wpnonce');

        if (!current_user_can('manage_options')) {
            wp_send_json_error(array('message' => 'Unauthorized'));
            return;
        }

        $api_key = sanitize_text_field($_POST['api_key']);
        update_option('compilot_ai_api_key', $api_key);

        wp_send_json_success(array('message' => 'API key updated'));
    }

    /**
     * Simple API connection test - just pings the health endpoint
     */
    public function ajax_test_connection() {
        check_ajax_referer('compilot-ai-nonce', 'nonce');

        if (!current_user_can('manage_options')) {
            wp_send_json_error(array('message' => 'Unauthorized'));
            return;
        }

        $api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');

        // Try to ping the Spring Boot Actuator health endpoint
        $response = wp_remote_get($api_url . '/actuator/health', array(
            'timeout' => 5
        ));

        if (is_wp_error($response)) {
            wp_send_json_error(array(
                'message' => 'Connection failed: ' . $response->get_error_message()
            ));
            return;
        }

        $status_code = wp_remote_retrieve_response_code($response);
        if ($status_code !== 200) {
            wp_send_json_error(array(
                'message' => 'API returned status code: ' . $status_code
            ));
            return;
        }

        $body = json_decode(wp_remote_retrieve_body($response), true);
        $status = $body['status'] ?? 'UNKNOWN';

        wp_send_json_success(array(
            'status' => $status,
            'message' => 'API is reachable and healthy'
        ));
    }

    /**
     * AJAX handler to get JWT token for admin panel
     * Authenticates with backend using tenant credentials (server-side)
     */
    public function ajax_get_jwt_token() {
        check_ajax_referer('compilot-ai-nonce', 'nonce');

        if (!current_user_can('manage_options')) {
            wp_send_json_error(array('message' => 'Unauthorized'));
            return;
        }

        // Get credentials from settings
        $api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');
        $tenant_id = get_option('compilot_ai_tenant_id', '');
        $api_key = get_option('compilot_ai_api_key', '');

        if (empty($tenant_id) || empty($api_key)) {
            wp_send_json_error(array('message' => 'Missing tenant credentials'));
            return;
        }

        // Authenticate with backend to get JWT token
        $response = wp_remote_post($api_url . '/api/auth/authenticate', array(
            'headers' => array('Content-Type' => 'application/json'),
            'body' => json_encode(array(
                'tenantId' => $tenant_id,
                'apiKey' => $api_key
            )),
            'timeout' => 10
        ));

        if (is_wp_error($response)) {
            wp_send_json_error(array('message' => $response->get_error_message()));
            return;
        }

        $status_code = wp_remote_retrieve_response_code($response);
        if ($status_code !== 200) {
            $body = wp_remote_retrieve_body($response);
            $error_data = json_decode($body, true);
            $error_message = $error_data['message'] ?? 'Authentication failed with status ' . $status_code;
            wp_send_json_error(array('message' => $error_message));
            return;
        }

        $body = json_decode(wp_remote_retrieve_body($response), true);
        $token = $body['token'] ?? null;
        $expires_in = $body['expiresIn'] ?? 3600;

        if (!$token) {
            wp_send_json_error(array('message' => 'No token received from server'));
            return;
        }

        wp_send_json_success(array(
            'token' => $token,
            'expiresIn' => $expires_in
        ));
    }

    public function ajax_search() {
        check_ajax_referer('compilot-ai-nonce', 'nonce');

        $query = sanitize_text_field($_POST['query']);

        // Get RAG settings
        $use_general_knowledge = get_option('compilot_ai_enable_general_knowledge', true);
        $persona = get_option('compilot_ai_rag_persona', 'customer_service');

        // Get min_confidence from request or settings
        $min_confidence = isset($_POST['min_confidence'])
            ? floatval($_POST['min_confidence'])
            : floatval(get_option('compilot_ai_min_confidence', 0.5));

        $response = $this->api_request('POST', '/api/ask', array(
            'query' => $query,
            'persona' => $persona,
            'use_general_knowledge' => (bool) $use_general_knowledge,
            'min_confidence' => $min_confidence
        ));

        if (is_wp_error($response)) {
            wp_send_json_error(array('message' => $response->get_error_message()));
        } else {
            wp_send_json_success($response);
        }
    }

    // Shortcodes

    public function search_shortcode($atts) {
        $atts = shortcode_atts(array(
            'placeholder' => 'Ask Compilot AI...',
            'button_text' => 'Search'
        ), $atts);

        ob_start();
        include COMPILOT_AI_PLUGIN_DIR . 'templates/search-form.php';
        return ob_get_clean();
    }

    public function submit_shortcode($atts) {
        if (!current_user_can('edit_posts')) {
            return '<p>You must be logged in to submit information.</p>';
        }

        $atts = shortcode_atts(array(
            'button_text' => 'Submit to Compilot AI'
        ), $atts);

        ob_start();
        include COMPILOT_AI_PLUGIN_DIR . 'templates/submit-form.php';
        return ob_get_clean();
    }

    // Helper methods

    private function api_request($method, $endpoint, $data = array()) {
        $url = $this->api_url . $endpoint;

        $headers = array(
            'Content-Type' => 'application/json'
        );

        // Add tenant authentication headers if configured
        $tenant_id = get_option('compilot_ai_tenant_id', '');
        $api_key = get_option('compilot_ai_api_key', '');

        if (!empty($tenant_id)) {
            $headers['X-Tenant-Id'] = $tenant_id;
        }

        if (!empty($api_key)) {
            $headers['X-API-Key'] = $api_key;
        }

        $args = array(
            'method' => $method,
            'headers' => $headers,
            'timeout' => 60
        );

        if (!empty($data) && $method !== 'GET') {
            $args['body'] = json_encode($data);
        }

        $response = wp_remote_request($url, $args);

        if (is_wp_error($response)) {
            return $response;
        }

        $body = wp_remote_retrieve_body($response);
        $decoded = json_decode($body, true);

        if (json_last_error() !== JSON_ERROR_NONE) {
            return new WP_Error('json_error', 'Invalid JSON response');
        }

        return $decoded;
    }
}

// Initialize the plugin
function compilot_ai_init() {
    return Compilot_AI_Plugin::get_instance();
}

add_action('plugins_loaded', 'compilot_ai_init');
