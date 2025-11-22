<?php
/**
 * Chat Widget Template
 */
if (!defined('ABSPATH')) {
    exit;
}
?>

<!-- Custom Widget Styling -->
<style>
    /* Dynamic colors from settings */
    .graphiti-chat-toggle {
        background: linear-gradient(135deg, <?php echo esc_attr($widget_primary_color); ?> 0%, <?php echo esc_attr($widget_secondary_color); ?> 100%) !important;
    }

    .graphiti-chat-header {
        background: linear-gradient(135deg, <?php echo esc_attr($widget_primary_color); ?> 0%, <?php echo esc_attr($widget_secondary_color); ?> 100%) !important;
    }

    .graphiti-chat-header h3 {
        font-size: <?php echo esc_attr($widget_header_font_size); ?>px !important;
    }

    .graphiti-message-user .graphiti-message-bubble {
        background: linear-gradient(135deg, <?php echo esc_attr($widget_primary_color); ?> 0%, <?php echo esc_attr($widget_secondary_color); ?> 100%) !important;
    }

    .graphiti-chat-send {
        background: linear-gradient(135deg, <?php echo esc_attr($widget_primary_color); ?> 0%, <?php echo esc_attr($widget_secondary_color); ?> 100%) !important;
    }

    .graphiti-chat-input {
        font-size: <?php echo esc_attr($widget_message_font_size); ?>px !important;
    }

    .graphiti-chat-input:focus {
        border-color: <?php echo esc_attr($widget_primary_color); ?> !important;
    }

    .graphiti-message-bubble {
        font-size: <?php echo esc_attr($widget_message_font_size); ?>px !important;
    }

    .graphiti-result-item {
        border-left-color: <?php echo esc_attr($widget_primary_color); ?> !important;
    }

    .graphiti-message-bubble a {
        color: <?php echo esc_attr($widget_primary_color); ?> !important;
    }

    .graphiti-ai-answer {
        background: linear-gradient(135deg, <?php echo esc_attr($widget_primary_color); ?> 0%, <?php echo esc_attr($widget_secondary_color); ?> 100%) !important;
    }

    .graphiti-supporting-facts {
        border-left-color: <?php echo esc_attr($widget_primary_color); ?> !important;
    }

    .graphiti-facts-header {
        color: <?php echo esc_attr($widget_primary_color); ?> !important;
    }

    /* Button colors */
    .graphiti-btn-escalate-yes,
    .graphiti-btn-continue,
    .graphiti-btn-still-here,
    .graphiti-btn-submit-email {
        background: <?php echo esc_attr($widget_primary_color); ?> !important;
    }

    .graphiti-btn-escalate-yes:hover,
    .graphiti-btn-continue:hover,
    .graphiti-btn-still-here:hover,
    .graphiti-btn-submit-email:hover {
        background: <?php echo esc_attr($widget_secondary_color); ?> !important;
    }

    .graphiti-email-input:focus {
        border-color: <?php echo esc_attr($widget_primary_color); ?> !important;
        box-shadow: 0 0 0 3px <?php echo esc_attr($widget_primary_color); ?>33 !important;
    }

    .graphiti-agent-initializing::before {
        border-top-color: <?php echo esc_attr($widget_primary_color); ?> !important;
    }
</style>

<!-- Compilot AI Chat Widget -->
<div id="compilot-chat-widget" class="graphiti-chat-widget">
    <!-- Toggle Button -->
    <button id="compilot-chat-toggle" class="graphiti-chat-toggle" aria-label="<?php echo esc_attr__('Open Compilot AI Chat', 'compilot-ai'); ?>">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M21 15C21 15.5304 20.7893 16.0391 20.4142 16.4142C20.0391 16.7893 19.5304 17 19 17H7L3 21V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H19C19.5304 3 20.0391 3.21071 20.4142 3.58579C20.7893 3.96086 21 4.46957 21 5V15Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span class="graphiti-chat-badge" id="compilot-chat-badge" style="display:none;">0</span>
    </button>

    <!-- Chat Window -->
    <div id="compilot-chat-window" class="graphiti-chat-window" style="display: none;">
        <!-- Header -->
        <div class="graphiti-chat-header">
            <h3><?php echo esc_html($chat_widget_name); ?></h3>
            <button id="compilot-chat-close" class="graphiti-chat-close" aria-label="<?php echo esc_attr__('Close chat', 'compilot-ai'); ?>">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M13.5 4.5L4.5 13.5M4.5 4.5L13.5 13.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
            </button>
        </div>

        <!-- Floating Language Selector -->
        <div class="graphiti-language-selector">
            <!-- Dropdown Menu -->
            <div id="compilot-language-dropdown" class="graphiti-language-dropdown" style="display: none;">
                <button class="graphiti-language-option" data-lang="auto">
                    <div class="graphiti-language-flag">🌐</div>
                    <div class="graphiti-language-details">
                        <span class="graphiti-language-code"><?php echo esc_html__('AUTO', 'compilot-ai'); ?></span>
                        <span class="graphiti-language-name"><?php echo esc_html__('Auto-detect', 'compilot-ai'); ?></span>
                    </div>
                    <svg class="graphiti-language-check" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                </button>
                <button class="graphiti-language-option" data-lang="en">
                    <div class="graphiti-language-flag">🇬🇧</div>
                    <div class="graphiti-language-details">
                        <span class="graphiti-language-code"><?php echo esc_html__('EN', 'compilot-ai'); ?></span>
                        <span class="graphiti-language-name"><?php echo esc_html__('English', 'compilot-ai'); ?></span>
                    </div>
                    <svg class="graphiti-language-check" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                </button>
                <button class="graphiti-language-option" data-lang="nl">
                    <div class="graphiti-language-flag">🇳🇱</div>
                    <div class="graphiti-language-details">
                        <span class="graphiti-language-code"><?php echo esc_html__('NL', 'compilot-ai'); ?></span>
                        <span class="graphiti-language-name"><?php echo esc_html__('Nederlands', 'compilot-ai'); ?></span>
                    </div>
                    <svg class="graphiti-language-check" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                </button>
                <button class="graphiti-language-option" data-lang="de">
                    <div class="graphiti-language-flag">🇩🇪</div>
                    <div class="graphiti-language-details">
                        <span class="graphiti-language-code"><?php echo esc_html__('DE', 'compilot-ai'); ?></span>
                        <span class="graphiti-language-name"><?php echo esc_html__('Deutsch', 'compilot-ai'); ?></span>
                    </div>
                    <svg class="graphiti-language-check" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                </button>
                <button class="graphiti-language-option" data-lang="fr">
                    <div class="graphiti-language-flag">🇫🇷</div>
                    <div class="graphiti-language-details">
                        <span class="graphiti-language-code"><?php echo esc_html__('FR', 'compilot-ai'); ?></span>
                        <span class="graphiti-language-name"><?php echo esc_html__('Français', 'compilot-ai'); ?></span>
                    </div>
                    <svg class="graphiti-language-check" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                </button>
                <button class="graphiti-language-option" data-lang="es">
                    <div class="graphiti-language-flag">🇪🇸</div>
                    <div class="graphiti-language-details">
                        <span class="graphiti-language-code"><?php echo esc_html__('ES', 'compilot-ai'); ?></span>
                        <span class="graphiti-language-name"><?php echo esc_html__('Español', 'compilot-ai'); ?></span>
                    </div>
                    <svg class="graphiti-language-check" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                </button>
                <button class="graphiti-language-option" data-lang="it">
                    <div class="graphiti-language-flag">🇮🇹</div>
                    <div class="graphiti-language-details">
                        <span class="graphiti-language-code"><?php echo esc_html__('IT', 'compilot-ai'); ?></span>
                        <span class="graphiti-language-name"><?php echo esc_html__('Italiano', 'compilot-ai'); ?></span>
                    </div>
                    <svg class="graphiti-language-check" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                </button>
                <button class="graphiti-language-option" data-lang="pt">
                    <div class="graphiti-language-flag">🇵🇹</div>
                    <div class="graphiti-language-details">
                        <span class="graphiti-language-code"><?php echo esc_html__('PT', 'compilot-ai'); ?></span>
                        <span class="graphiti-language-name"><?php echo esc_html__('Português', 'compilot-ai'); ?></span>
                    </div>
                    <svg class="graphiti-language-check" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                </button>
            </div>

            <!-- Toggle Button -->
            <button id="compilot-language-toggle" class="graphiti-language-toggle">
                <div class="graphiti-language-toggle-flag" id="compilot-language-current-flag">🌐</div>
                <div class="graphiti-language-toggle-badge" id="compilot-language-current-badge"><?php echo esc_html__('AUTO', 'compilot-ai'); ?></div>
                <div class="graphiti-language-tooltip"><?php echo esc_html__('Auto-detect', 'compilot-ai'); ?></div>
            </button>
        </div>

        <!-- Messages Area -->
        <div id="compilot-chat-messages" class="graphiti-chat-messages">
            <div class="graphiti-chat-welcome">
                <p><?php echo esc_html($chat_greeting_message); ?></p>
                <p class="graphiti-chat-hint"><?php echo esc_html($chat_greeting_hint); ?></p>
            </div>
        </div>

        <!-- Input Area -->
        <div class="graphiti-chat-input-container">
            <!-- Autocomplete Suggestions -->
            <div id="compilot-autocomplete" class="graphiti-autocomplete" style="display: none;"></div>

            <!-- Satisfaction Prompt -->
            <div id="compilot-satisfaction-prompt" class="graphiti-satisfaction-prompt" style="display: none;">
                <p id="compilot-satisfaction-message"></p>
                <div class="graphiti-satisfaction-buttons">
                    <button class="graphiti-btn graphiti-btn-yes" data-feedback="yes">
                        👍 <?php echo esc_html__('Yes, helpful', 'compilot-ai'); ?>
                    </button>
                    <button class="graphiti-btn graphiti-btn-no" data-feedback="no">
                        👎 <?php echo esc_html__('No, not helpful', 'compilot-ai'); ?>
                    </button>
                </div>
            </div>

            <!-- Input Field -->
            <div class="graphiti-chat-input-wrapper">
                <textarea
                    id="compilot-chat-input"
                    class="graphiti-chat-input"
                    placeholder="<?php echo esc_attr__('Ask a question...', 'compilot-ai'); ?>"
                    autocomplete="off"
                    rows="1"
                ></textarea>
                <button id="compilot-chat-send" class="graphiti-chat-send" aria-label="<?php echo esc_attr__('Send message', 'compilot-ai'); ?>">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M18 2L9 11M18 2L12 18L9 11M18 2L2 8L9 11" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </button>
            </div>

            <!-- Typing Indicator -->
            <div id="compilot-typing" class="graphiti-typing" style="display: none;">
                <span></span>
                <span></span>
                <span></span>
            </div>
        </div>
    </div>
</div>
