<?php
// Prevent direct access
if (!defined('ABSPATH')) {
    exit;
}

// Save settings
if (isset($_POST['compilot_ai_save_settings'])) {
    check_admin_referer('compilot_ai_settings');

    update_option('compilot_ai_api_url', esc_url_raw($_POST['api_url']));
    update_option('compilot_ai_tenant_id', sanitize_text_field($_POST['tenant_id']));
    // Note: API key is NOT saved here - it's only updated via the "Regenerate API Key" button
    update_option('compilot_ai_min_confidence', floatval($_POST['min_confidence']));
    update_option('compilot_enable_chat_widget', isset($_POST['enable_chat_widget']) ? 1 : 0);
    update_option('compilot_chat_widget_name', sanitize_text_field($_POST['chat_widget_name']));
    update_option('compilot_ai_assistant_name', sanitize_text_field($_POST['ai_assistant_name']));

    // RAG settings
    update_option('compilot_ai_enable_general_knowledge', isset($_POST['enable_general_knowledge']) ? 1 : 0);
    update_option('compilot_ai_show_gk_disclaimer', isset($_POST['show_gk_disclaimer']) ? 1 : 0);
    update_option('compilot_ai_gk_disclaimer_text', sanitize_textarea_field($_POST['gk_disclaimer_text']));
    update_option('compilot_ai_rag_persona', sanitize_text_field($_POST['rag_persona']));
    update_option('compilot_ai_default_language', sanitize_text_field($_POST['default_language']));
    update_option('compilot_ai_show_sources', isset($_POST['show_sources']) ? 1 : 0);

    // Content sync settings
    update_option('compilot_auto_sync', isset($_POST['auto_sync']) ? 1 : 0);

    // Widget styling settings
    update_option('compilot_widget_primary_color', sanitize_hex_color($_POST['widget_primary_color']));
    update_option('compilot_widget_secondary_color', sanitize_hex_color($_POST['widget_secondary_color']));
    update_option('compilot_widget_message_font_size', intval($_POST['widget_message_font_size']));
    update_option('compilot_widget_header_font_size', intval($_POST['widget_header_font_size']));

    // Widget behavior settings
    update_option('compilot_session_timeout_minutes', intval($_POST['session_timeout_minutes']));
    update_option('compilot_chat_greeting_message', sanitize_textarea_field($_POST['chat_greeting_message']));
    update_option('compilot_chat_greeting_hint', sanitize_text_field($_POST['chat_greeting_hint']));

    echo '<div class="notice notice-success"><p>' . esc_html__('Settings saved successfully!', 'compilot-ai') . '</p></div>';
}

$api_url = get_option('compilot_ai_api_url', 'http://localhost:8000');
$tenant_id = get_option('compilot_ai_tenant_id', 'dev');
$api_key = get_option('compilot_ai_api_key', 'Cea88tZgz7U8ri5mqH3LF2MiD3hidTp_IaY-7_dwH4M');
$min_confidence = get_option('compilot_ai_min_confidence', 0.5);
$enable_chat_widget = get_option('compilot_enable_chat_widget', true);
$chat_widget_name = get_option('compilot_chat_widget_name', 'Compilot AI Assistant');
$ai_assistant_name = get_option('compilot_ai_assistant_name', 'Friendly AI Bot');

// RAG settings
$enable_general_knowledge = get_option('compilot_ai_enable_general_knowledge', true);
$show_gk_disclaimer = get_option('compilot_ai_show_gk_disclaimer', false);
$gk_disclaimer_text = get_option('compilot_ai_gk_disclaimer_text', '*Let op: Dit antwoord is gebaseerd op algemene kennis, niet uit onze kennisbank.*');
$rag_persona = get_option('compilot_ai_rag_persona', 'customer_service');
$default_language = get_option('compilot_ai_default_language', 'nl');
$show_sources = get_option('compilot_ai_show_sources', true);

// Content sync settings
$auto_sync = get_option('compilot_auto_sync', 1);

// Widget styling settings
$widget_primary_color = get_option('compilot_widget_primary_color', '#667eea');
$widget_secondary_color = get_option('compilot_widget_secondary_color', '#764ba2');
$widget_message_font_size = get_option('compilot_widget_message_font_size', 14);
$widget_header_font_size = get_option('compilot_widget_header_font_size', 18);

// Widget behavior settings
$session_timeout_minutes = get_option('compilot_session_timeout_minutes', 5);
$chat_greeting_message = get_option('compilot_chat_greeting_message', '👋 Hallo! Hoe kan ik u vandaag helpen?');
$chat_greeting_hint = get_option('compilot_chat_greeting_hint', 'Begin met typen om suggesties te zien...');
?>

<div class="wrap compilot-ai-settings">
    <h1><?php echo esc_html(get_admin_page_title()); ?></h1>

    <!-- Tab Navigation -->
    <h2 class="nav-tab-wrapper">
        <a href="#connection" class="nav-tab nav-tab-active" data-tab="connection"><?php echo esc_html__('Connection', 'compilot-ai'); ?></a>
        <a href="#ai-settings" class="nav-tab" data-tab="ai-settings"><?php echo esc_html__('AI Settings', 'compilot-ai'); ?></a>
        <a href="#chat-widget" class="nav-tab" data-tab="chat-widget"><?php echo esc_html__('Chat Widget', 'compilot-ai'); ?></a>
        <a href="#styling" class="nav-tab" data-tab="styling"><?php echo esc_html__('Appearance', 'compilot-ai'); ?></a>
        <a href="#content-sync" class="nav-tab" data-tab="content-sync"><?php echo esc_html__('Content Sync', 'compilot-ai'); ?></a>
    </h2>

    <form method="post" action="">
        <?php wp_nonce_field('compilot_ai_settings'); ?>

        <!-- Connection Tab -->
        <div id="tab-connection" class="tab-content" style="display: block;">
            <h2><?php echo esc_html__('API Connection', 'compilot-ai'); ?></h2>
            <table class="form-table">
                <tr>
                    <th scope="row">
                        <label for="api_url"><?php echo esc_html__('API Server URL', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="url" id="api_url" name="api_url" value="<?php echo esc_attr($api_url); ?>" class="regular-text" required>
                        <p class="description">
                            <?php echo esc_html__('The URL where the Compilot AI backend API is running', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
            </table>

            <h2><?php echo esc_html__('Tenant Authentication', 'compilot-ai'); ?></h2>
            <table class="form-table">
                <tr>
                    <td colspan="2">
                        <div class="notice notice-info inline">
                            <p>
                                <strong><?php echo esc_html__('Development Mode:', 'compilot-ai'); ?></strong> <?php echo esc_html__('The default credentials connect to the auto-created development tenant.', 'compilot-ai'); ?><br>
                                <strong><?php echo esc_html__('Production:', 'compilot-ai'); ?></strong> <?php echo esc_html__('Replace these with your production tenant credentials from the Compilot AI backend startup logs.', 'compilot-ai'); ?>
                            </p>
                        </div>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="tenant_id"><?php echo esc_html__('Tenant ID', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="text" id="tenant_id" name="tenant_id" value="<?php echo esc_attr($tenant_id); ?>" class="regular-text" required>
                        <p class="description">
                            <?php echo esc_html__('Your unique tenant identifier provided by Compilot AI', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="api_key"><?php echo esc_html__('API Key', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="text" id="api_key" name="api_key" value="<?php echo !empty($api_key) ? '••••••••••••••••••••••••••••••••' : ''; ?>" class="regular-text" readonly style="background-color: #f0f0f1; cursor: not-allowed;">
                        <button type="button" id="regenerate-api-key" class="button" style="margin-left: 10px;">
                            <?php echo esc_html__('Regenerate API Key', 'compilot-ai'); ?>
                        </button>
                        <div id="api-key-result" style="margin-top: 15px;"></div>
                        <p class="description">
                            <?php echo esc_html__('Your secret API key is hidden for security. Only changed via "Regenerate API Key" button.', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
            </table>

            <h2><?php echo esc_html__('Connection Test', 'compilot-ai'); ?></h2>
            <button type="button" id="test-connection" class="button"><?php echo esc_html__('Test API Connection', 'compilot-ai'); ?></button>
            <div id="connection-test-result" style="margin-top: 15px;"></div>
        </div>

        <!-- AI Settings Tab -->
        <div id="tab-ai-settings" class="tab-content" style="display: none;">
            <h2><?php echo esc_html__('AI Assistant Identity', 'compilot-ai'); ?></h2>
            <table class="form-table">
                <tr>
                    <th scope="row">
                        <label for="ai_assistant_name"><?php echo esc_html__('AI Assistant Name', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="text" id="ai_assistant_name" name="ai_assistant_name" value="<?php echo esc_attr($ai_assistant_name); ?>" class="regular-text" required>
                        <p class="description">
                            <?php echo esc_html__('The name the AI identifies itself as when users ask "Who are you?" - used in system prompts', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="rag_persona"><?php echo esc_html__('AI Personality', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <select id="rag_persona" name="rag_persona" class="regular-text">
                            <option value="customer_service" <?php selected($rag_persona, 'customer_service'); ?>><?php echo esc_html__('Customer Service (Friendly & Helpful)', 'compilot-ai'); ?></option>
                            <option value="professional" <?php selected($rag_persona, 'professional'); ?>><?php echo esc_html__('Professional Expert (Polished & Knowledgeable)', 'compilot-ai'); ?></option>
                            <option value="casual" <?php selected($rag_persona, 'casual'); ?>><?php echo esc_html__('Casual Friend (Warm & Relatable)', 'compilot-ai'); ?></option>
                            <option value="technical" <?php selected($rag_persona, 'technical'); ?>><?php echo esc_html__('Technical Specialist (Detailed & Thorough)', 'compilot-ai'); ?></option>
                            <option value="concise" <?php selected($rag_persona, 'concise'); ?>><?php echo esc_html__('Concise Assistant (Brief & Direct)', 'compilot-ai'); ?></option>
                            <option value="educational" <?php selected($rag_persona, 'educational'); ?>><?php echo esc_html__('Educational Tutor (Clear & Encouraging)', 'compilot-ai'); ?></option>
                        </select>
                        <p class="description">
                            <?php echo esc_html__('Choose the AI\'s communication style and personality', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="default_language"><?php echo esc_html__('Default Language', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <select id="default_language" name="default_language" class="regular-text">
                            <option value="auto" <?php selected($default_language, 'auto'); ?>><?php echo esc_html__('Auto-detect from user input', 'compilot-ai'); ?></option>
                            <option value="en" <?php selected($default_language, 'en'); ?>><?php echo esc_html__('English', 'compilot-ai'); ?></option>
                            <option value="nl" <?php selected($default_language, 'nl'); ?>><?php echo esc_html__('Dutch (Nederlands)', 'compilot-ai'); ?></option>
                            <option value="de" <?php selected($default_language, 'de'); ?>><?php echo esc_html__('German (Deutsch)', 'compilot-ai'); ?></option>
                            <option value="fr" <?php selected($default_language, 'fr'); ?>><?php echo esc_html__('French (Français)', 'compilot-ai'); ?></option>
                            <option value="es" <?php selected($default_language, 'es'); ?>><?php echo esc_html__('Spanish (Español)', 'compilot-ai'); ?></option>
                            <option value="it" <?php selected($default_language, 'it'); ?>><?php echo esc_html__('Italian (Italiano)', 'compilot-ai'); ?></option>
                            <option value="pt" <?php selected($default_language, 'pt'); ?>><?php echo esc_html__('Portuguese (Português)', 'compilot-ai'); ?></option>
                        </select>
                        <p class="description">
                            <?php echo esc_html__('Choose the default language for AI responses', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
            </table>

            <h2><?php echo esc_html__('Knowledge & Responses', 'compilot-ai'); ?></h2>
            <table class="form-table">
                <tr>
                    <th scope="row">
                        <label for="min_confidence"><?php echo esc_html__('Minimum Confidence Threshold', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="number" id="min_confidence" name="min_confidence" value="<?php echo esc_attr($min_confidence); ?>" min="0" max="1" step="0.05" required>
                        <p class="description"><?php echo esc_html__('Results with lower confidence will be filtered out (0-1, default: 0.5)', 'compilot-ai'); ?></p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="enable_general_knowledge"><?php echo esc_html__('Allow General Knowledge', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <label>
                            <input type="checkbox" id="enable_general_knowledge" name="enable_general_knowledge" value="1" <?php checked($enable_general_knowledge, 1); ?>>
                            <?php echo esc_html__('Answer questions outside the knowledge graph using AI\'s general knowledge', 'compilot-ai'); ?>
                        </label>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="show_gk_disclaimer"><?php echo esc_html__('Show General Knowledge Disclaimer', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <label>
                            <input type="checkbox" id="show_gk_disclaimer" name="show_gk_disclaimer" value="1" <?php checked($show_gk_disclaimer, 1); ?>>
                            <?php echo esc_html__('Add disclaimer when using general knowledge', 'compilot-ai'); ?>
                        </label>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="gk_disclaimer_text"><?php echo esc_html__('Disclaimer Text', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <textarea id="gk_disclaimer_text" name="gk_disclaimer_text" class="large-text" rows="2"><?php echo esc_textarea($gk_disclaimer_text); ?></textarea>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="show_sources"><?php echo esc_html__('Show Sources', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <label>
                            <input type="checkbox" id="show_sources" name="show_sources" value="1" <?php checked($show_sources, 1); ?>>
                            <?php echo esc_html__('Display source links in AI responses', 'compilot-ai'); ?>
                        </label>
                    </td>
                </tr>
            </table>
        </div>

        <!-- Chat Widget Tab -->
        <div id="tab-chat-widget" class="tab-content" style="display: none;">
            <h2><?php echo esc_html__('Widget Settings', 'compilot-ai'); ?></h2>
            <table class="form-table">
                <tr>
                    <th scope="row">
                        <label for="enable_chat_widget"><?php echo esc_html__('Enable Chat Widget', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <label>
                            <input type="checkbox" id="enable_chat_widget" name="enable_chat_widget" value="1" <?php checked($enable_chat_widget, 1); ?>>
                            <?php echo esc_html__('Show floating chat widget on frontend pages', 'compilot-ai'); ?>
                        </label>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="chat_widget_name"><?php echo esc_html__('Chat Widget Title', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="text" id="chat_widget_name" name="chat_widget_name" value="<?php echo esc_attr($chat_widget_name); ?>" class="regular-text" required>
                        <p class="description">
                            <?php echo esc_html__('The title shown in the chat widget header (e.g., "Support Chat", "Ask AI")', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="session_timeout_minutes"><?php echo esc_html__('Session Timeout', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="number" id="session_timeout_minutes" name="session_timeout_minutes" value="<?php echo esc_attr($session_timeout_minutes); ?>" min="1" max="60" required>
                        <span><?php echo esc_html__('minutes', 'compilot-ai'); ?></span>
                        <p class="description">
                            <?php echo esc_html__('Inactivity timeout before prompting user (default: 5 minutes)', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="chat_greeting_message"><?php echo esc_html__('Greeting Message', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <textarea id="chat_greeting_message" name="chat_greeting_message" class="large-text" rows="2" required><?php echo esc_textarea($chat_greeting_message); ?></textarea>
                        <p class="description">
                            <?php echo esc_html__('The greeting shown when users first open the chat', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="chat_greeting_hint"><?php echo esc_html__('Greeting Hint', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="text" id="chat_greeting_hint" name="chat_greeting_hint" value="<?php echo esc_attr($chat_greeting_hint); ?>" class="large-text" required>
                        <p class="description">
                            <?php echo esc_html__('Hint text shown below the greeting', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
            </table>
        </div>

        <!-- Styling Tab -->
        <div id="tab-styling" class="tab-content" style="display: none;">
            <h2><?php echo esc_html__('Widget Colors', 'compilot-ai'); ?></h2>
            <table class="form-table">
                <tr>
                    <th scope="row">
                        <label for="widget_primary_color"><?php echo esc_html__('Primary Color', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="color" id="widget_primary_color" name="widget_primary_color" value="<?php echo esc_attr($widget_primary_color); ?>" required>
                        <p class="description">
                            <?php echo esc_html__('Main color for header and buttons (default: #667eea)', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="widget_secondary_color"><?php echo esc_html__('Secondary Color', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="color" id="widget_secondary_color" name="widget_secondary_color" value="<?php echo esc_attr($widget_secondary_color); ?>" required>
                        <p class="description">
                            <?php echo esc_html__('Secondary color for gradient effects (default: #764ba2)', 'compilot-ai'); ?>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <?php echo esc_html__('Preview', 'compilot-ai'); ?>
                    </th>
                    <td>
                        <div id="widget-preview" style="display: inline-block; padding: 20px; border-radius: 12px; color: white; font-weight: 600;">
                            <?php echo esc_html__('Widget Preview', 'compilot-ai'); ?>
                        </div>
                    </td>
                </tr>
            </table>

            <h2><?php echo esc_html__('Typography', 'compilot-ai'); ?></h2>
            <table class="form-table">
                <tr>
                    <th scope="row">
                        <label for="widget_message_font_size"><?php echo esc_html__('Message Font Size', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="number" id="widget_message_font_size" name="widget_message_font_size" value="<?php echo esc_attr($widget_message_font_size); ?>" min="10" max="24" required>
                        <span><?php echo esc_html__('px', 'compilot-ai'); ?></span>
                        <p class="description"><?php echo esc_html__('Font size for chat messages (default: 14px)', 'compilot-ai'); ?></p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label for="widget_header_font_size"><?php echo esc_html__('Header Font Size', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <input type="number" id="widget_header_font_size" name="widget_header_font_size" value="<?php echo esc_attr($widget_header_font_size); ?>" min="14" max="28" required>
                        <span><?php echo esc_html__('px', 'compilot-ai'); ?></span>
                        <p class="description"><?php echo esc_html__('Font size for widget header (default: 18px)', 'compilot-ai'); ?></p>
                    </td>
                </tr>
            </table>
        </div>

        <!-- Content Sync Tab -->
        <div id="tab-content-sync" class="tab-content" style="display: none;">
            <h2><?php echo esc_html__('Content Synchronization', 'compilot-ai'); ?></h2>
            <table class="form-table">
                <tr>
                    <th scope="row">
                        <label for="auto_sync"><?php echo esc_html__('Auto-Sync Content', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <label>
                            <input type="checkbox" id="auto_sync" name="auto_sync" value="1" <?php checked($auto_sync, 1); ?>>
                            <?php echo esc_html__('Automatically sync posts/pages to knowledge graph when published or updated', 'compilot-ai'); ?>
                        </label>
                    </td>
                </tr>
                <tr>
                    <th scope="row">
                        <label><?php echo esc_html__('Manual Sync', 'compilot-ai'); ?></label>
                    </th>
                    <td>
                        <button type="button" id="compilot-sync-all" class="button button-secondary">
                            <?php echo esc_html__('Sync All Published Content Now', 'compilot-ai'); ?>
                        </button>
                        <button type="button" id="compilot-clear-sync" class="button button-secondary" style="margin-left: 10px;">
                            <?php echo esc_html__('Clear Sync Status', 'compilot-ai'); ?>
                        </button>
                        <p class="description">
                            <?php echo esc_html__('Click to manually sync all published posts and pages to the knowledge graph.', 'compilot-ai'); ?>
                        </p>
                        <div id="sync-progress" style="display:none; margin-top: 10px;">
                            <progress id="sync-progress-bar" max="100" value="0" style="width: 100%; height: 25px;"></progress>
                            <p id="sync-status" style="margin-top: 5px;"></p>
                        </div>
                        <div id="sync-result" style="margin-top: 10px;"></div>
                    </td>
                </tr>
            </table>
        </div>

        <p class="submit">
            <input type="submit" name="compilot_ai_save_settings" class="button button-primary" value="<?php echo esc_attr__('Save Settings', 'compilot-ai'); ?>">
        </p>
    </form>
</div>

<style>
.compilot-ai-settings .nav-tab-wrapper {
    margin-bottom: 20px;
}
.compilot-ai-settings .tab-content {
    background: #fff;
    border: 1px solid #ccd0d4;
    border-top: none;
    padding: 20px;
}
</style>

<script>
jQuery(document).ready(function($) {
    // Tab switching
    $('.nav-tab').on('click', function(e) {
        e.preventDefault();
        var target = $(this).data('tab');

        // Update active tab
        $('.nav-tab').removeClass('nav-tab-active');
        $(this).addClass('nav-tab-active');

        // Show corresponding content
        $('.tab-content').hide();
        $('#tab-' + target).show();
    });

    // Color preview update
    function updatePreview() {
        var primary = $('#widget_primary_color').val();
        var secondary = $('#widget_secondary_color').val();
        var preview = $('#widget-preview');
        preview.css('background', 'linear-gradient(135deg, ' + primary + ' 0%, ' + secondary + ' 100%)');
    }
    $('#widget_primary_color, #widget_secondary_color').on('input', updatePreview);
    updatePreview(); // Initialize on load
});
</script>
