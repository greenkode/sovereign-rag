<?php
/**
 * Compilot AI Chat Widget
 *
 * Floating chat widget with autocomplete and search functionality
 */

if (!defined('ABSPATH')) {
    exit;
}

class Compilot_Chat_Widget {

    public function __construct() {
        add_action('wp_footer', array($this, 'render_widget'));
        add_action('wp_enqueue_scripts', array($this, 'enqueue_scripts'));

        // AJAX endpoint for refreshing JWT token
        add_action('wp_ajax_compilot_refresh_token', array($this, 'ajax_refresh_token'));
        add_action('wp_ajax_nopriv_compilot_refresh_token', array($this, 'ajax_refresh_token'));
    }

    /**
     * Get JWT token from backend API (server-side authentication)
     *
     * This method authenticates with the backend using tenant credentials
     * and returns a JWT token. The token is cached for 90% of its lifetime
     * to minimize authentication requests while ensuring freshness.
     *
     * @return string|null JWT token or null if authentication fails
     */
    private function get_jwt_token() {
        // Check if we have a cached valid token (cache for 90% of lifetime)
        $cached_token = get_transient('compilot_jwt_token');
        if ($cached_token) {
            return $cached_token;
        }

        // Authenticate with backend to get new token
        $api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');
        $tenant_id = get_option('compilot_ai_tenant_id', '');
        $api_key = get_option('compilot_ai_api_key', '');

        if (empty($tenant_id) || empty($api_key)) {
            error_log('Compilot AI: Missing tenant credentials for JWT authentication');
            return null;
        }

        $response = wp_remote_post($api_url . '/api/auth/authenticate', array(
            'headers' => array('Content-Type' => 'application/json'),
            'body' => json_encode(array(
                'tenantId' => $tenant_id,
                'apiKey' => $api_key
            )),
            'timeout' => 10
        ));

        if (is_wp_error($response)) {
            error_log('Compilot AI: JWT authentication failed - ' . $response->get_error_message());
            return null;
        }

        $status_code = wp_remote_retrieve_response_code($response);
        if ($status_code !== 200) {
            error_log('Compilot AI: JWT authentication failed with status ' . $status_code);
            return null;
        }

        $body = json_decode(wp_remote_retrieve_body($response), true);
        $token = $body['token'] ?? null;
        $expires_in = $body['expiresIn'] ?? 3600;

        // Cache token for 90% of its lifetime to ensure it doesn't expire during use
        if ($token) {
            $cache_duration = (int) ($expires_in * 0.9);
            set_transient('compilot_jwt_token', $token, $cache_duration);
            error_log('Compilot AI: JWT token cached for ' . $cache_duration . ' seconds');
        }

        return $token;
    }

    /**
     * AJAX handler to refresh JWT token
     * Called from JavaScript when token expires (403 error)
     */
    public function ajax_refresh_token() {
        // Clear the cached token to force a refresh
        delete_transient('compilot_jwt_token');

        // Get a new token
        $new_token = $this->get_jwt_token();

        if ($new_token) {
            wp_send_json_success(array('token' => $new_token));
        } else {
            wp_send_json_error(array('message' => 'Failed to refresh JWT token'));
        }
    }

    /**
     * Enqueue widget styles and scripts
     */
    public function enqueue_scripts() {
        // Only load on frontend
        if (is_admin()) {
            return;
        }

        wp_enqueue_style(
            'compilot-chat-widget',
            plugins_url('../assets/css/chat-widget.css', __FILE__),
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
            'compilot-chat-widget',
            plugins_url('../assets/js/chat-widget.js', __FILE__),
            array('jquery', 'marked-js'),
            COMPILOT_AI_VERSION,
            true
        );

        // Get JWT token from backend (server-side authentication)
        // SECURITY: API key never exposed to browser - only short-lived JWT token
        $jwt_token = $this->get_jwt_token();

        // Debug: Log token fetch result
        if ($jwt_token) {
            error_log('Compilot AI: JWT token successfully fetched');
        } else {
            error_log('Compilot AI: JWT token fetch FAILED - widget will not work');
        }

        // Pass API URL and settings to JavaScript
        $show_sources_raw = get_option('compilot_ai_show_sources', true);
        // wp_localize_script doesn't handle boolean false well (converts to empty string)
        // So we pass as integer (0 or 1) which JavaScript can reliably check
        $show_sources_setting = $show_sources_raw ? 1 : 0;

        wp_localize_script('compilot-chat-widget', 'compilotChat', array(
            'apiUrl' => get_option('compilot_ai_api_url', 'http://localhost:8000'),
            'ajaxUrl' => admin_url('admin-ajax.php'),
            'nonce' => wp_create_nonce('compilot_chat_nonce'),
            // JWT authentication - SECURE: Only pass JWT token, not API key
            'jwtToken' => $jwt_token,
            // REMOVED: tenantId and apiKey - no longer exposed to browser
            'lowConfidenceThreshold' => floatval(get_option('compilot_ai_low_confidence_threshold', 0.5)),
            'minConfidence' => floatval(get_option('compilot_ai_min_confidence', 0.5)),
            'returnMode' => get_option('compilot_ai_return_mode', 'multiple'),
            // RAG settings
            'enableGeneralKnowledge' => (int) (bool) get_option('compilot_ai_enable_general_knowledge', true),
            'showGeneralKnowledgeDisclaimer' => (int) (bool) get_option('compilot_ai_show_gk_disclaimer', false),
            'generalKnowledgeDisclaimerText' => get_option('compilot_ai_gk_disclaimer_text', '*Let op: Dit antwoord is gebaseerd op algemene kennis, niet uit onze kennisbank.*'),
            'ragModel' => get_option('compilot_ai_rag_model', 'llama3.1'),
            'ragPersona' => get_option('compilot_ai_rag_persona', 'customer_service'),
            'showSources' => $show_sources_setting,
            // Widget behavior settings
            'sessionTimeoutMinutes' => intval(get_option('compilot_session_timeout_minutes', 5)),
            'greetingMessage' => get_option('compilot_chat_greeting_message', '👋 Hallo! Hoe kan ik u vandaag helpen?'),
            'greetingHint' => get_option('compilot_chat_greeting_hint', 'Begin met typen om suggesties te zien...'),
            // Language settings
            'language' => get_locale(),  // e.g., 'en_US', 'nl_NL', 'de_DE', 'fr_FR'
            'defaultLanguage' => get_option('compilot_ai_default_language', 'nl')  // Default response language
        ));
    }

    /**
     * Render the chat widget HTML
     */
    public function render_widget() {
        // Only show on frontend
        if (is_admin()) {
            return;
        }

        // Check if widget is enabled
        if (!get_option('compilot_enable_chat_widget', true)) {
            return;
        }

        // Get chat widget name from settings
        $chat_widget_name = get_option('compilot_chat_widget_name', 'Compilot AI Assistant');

        // Get greeting message from settings
        $chat_greeting_message = get_option('compilot_chat_greeting_message', '👋 Hallo! Hoe kan ik u vandaag helpen?');
        $chat_greeting_hint = get_option('compilot_chat_greeting_hint', 'Begin met typen om suggesties te zien...');

        // Get widget styling settings
        $widget_primary_color = get_option('compilot_widget_primary_color', '#667eea');
        $widget_secondary_color = get_option('compilot_widget_secondary_color', '#764ba2');
        $widget_message_font_size = get_option('compilot_widget_message_font_size', 14);
        $widget_header_font_size = get_option('compilot_widget_header_font_size', 18);

        include plugin_dir_path(__FILE__) . '../templates/chat-widget.php';
    }
}

// Initialize widget
new Compilot_Chat_Widget();
