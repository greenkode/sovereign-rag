<?php
// Prevent direct access
if (!defined('ABSPATH')) {
    exit;
}
?>

<div class="compilot-ai-submit-widget">
    <form class="graphiti-submit-form" data-nonce="<?php echo wp_create_nonce('compilot-ai-nonce'); ?>">
        <div class="form-group">
            <label for="submit-content"><?php echo esc_html__('Information to Share', 'compilot-ai'); ?></label>
            <textarea
                id="submit-content"
                name="content"
                class="graphiti-submit-content"
                rows="8"
                placeholder="<?php echo esc_attr__('Enter information you\'d like to add to the knowledge graph...', 'compilot-ai'); ?>"
                required
            ></textarea>
        </div>

        <div class="form-group">
            <label for="submit-description"><?php echo esc_html__('Description', 'compilot-ai'); ?></label>
            <input
                type="text"
                id="submit-description"
                name="description"
                class="graphiti-submit-description"
                placeholder="<?php echo esc_attr__('Brief description of this information', 'compilot-ai'); ?>"
                value="<?php echo esc_attr__('User submission', 'compilot-ai'); ?>"
                required
            >
        </div>

        <div class="form-group">
            <button type="submit" class="graphiti-submit-button">
                <?php echo esc_html($atts['button_text']); ?>
            </button>
            <span class="graphiti-submit-spinner" style="display:none;">
                <span class="spinner is-active"></span> <?php echo esc_html__('Submitting...', 'compilot-ai'); ?>
            </span>
        </div>

        <div class="graphiti-submit-message" style="display:none;"></div>
    </form>
</div>
