<?php
// Prevent direct access
if (!defined('ABSPATH')) {
    exit;
}
?>

<div class="compilot-ai-search-widget">
    <form class="graphiti-search-form" data-nonce="<?php echo wp_create_nonce('compilot-ai-nonce'); ?>">
        <div class="search-input-wrapper">
            <input
                type="text"
                name="query"
                class="graphiti-search-input"
                placeholder="<?php echo esc_attr($atts['placeholder']); ?>"
                required
            >
            <button type="submit" class="graphiti-search-button">
                <?php echo esc_html($atts['button_text']); ?>
            </button>
        </div>
        <div class="graphiti-search-spinner" style="display:none;">
            <span class="spinner is-active"></span> <?php echo esc_html__('Searching...', 'compilot-ai'); ?>
        </div>
    </form>

    <div class="graphiti-search-results" style="display:none;">
        <h3 class="results-title"><?php echo esc_html__('Search Results', 'compilot-ai'); ?></h3>
        <div class="results-container"></div>
    </div>
</div>
