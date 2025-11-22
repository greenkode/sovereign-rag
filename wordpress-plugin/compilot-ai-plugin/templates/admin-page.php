<?php
// Prevent direct access
if (!defined('ABSPATH')) {
    exit;
}
?>

<div class="wrap compilot-ai-admin">
    <h1><?php echo esc_html(get_admin_page_title()); ?></h1>

    <div class="compilot-ai-dashboard">
        <!-- Info Section -->
        <div class="compilot-ai-card">
            <h2><?php echo esc_html__('Compilot AI Assistant Management', 'compilot-ai'); ?></h2>
            <p>
                <?php echo esc_html__('Content is automatically synchronized to the knowledge graph when you publish or update posts and pages.', 'compilot-ai'); ?>
                <?php echo esc_html__('You can also manually sync all content from the Settings page.', 'compilot-ai'); ?>
            </p>
            <p>
                <a href="<?php echo admin_url('admin.php?page=compilot-ai-settings'); ?>" class="button button-primary">
                    <?php echo esc_html__('Go to Settings', 'compilot-ai'); ?>
                </a>
            </p>
        </div>

        <!-- Search Section -->
        <div class="compilot-ai-search-section">
            <h2><?php echo esc_html__('Search Compilot AI Assistant', 'compilot-ai'); ?></h2>
            <form id="admin-search-form">
                <table class="form-table">
                    <tr>
                        <th scope="row">
                            <label for="search-query"><?php echo esc_html__('Query', 'compilot-ai'); ?></label>
                        </th>
                        <td>
                            <input type="text" id="search-query" name="query" class="regular-text" required>
                        </td>
                    </tr>
                    <tr>
                        <th scope="row">
                            <label for="num-results"><?php echo esc_html__('Number of Results', 'compilot-ai'); ?></label>
                        </th>
                        <td>
                            <input type="number" id="num-results" name="num_results" value="10" min="1" max="50">
                        </td>
                    </tr>
                    <tr>
                        <th scope="row">
                            <label for="min-confidence"><?php echo esc_html__('Minimum Confidence', 'compilot-ai'); ?></label>
                        </th>
                        <td>
                            <input type="number" id="min-confidence" name="min_confidence" value="0.5" min="0" max="1" step="0.1">
                            <p class="description"><?php echo esc_html__('Results with lower confidence will be filtered out.', 'compilot-ai'); ?></p>
                        </td>
                    </tr>
                </table>

                <p class="submit">
                    <button type="submit" class="button button-primary"><?php echo esc_html__('Search', 'compilot-ai'); ?></button>
                    <span class="spinner"></span>
                </p>

                <div id="search-results"></div>
            </form>
        </div>
    </div>
</div>
