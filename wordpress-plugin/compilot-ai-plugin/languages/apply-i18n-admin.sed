# SED script to add i18n wrappers to admin-page.php

# Section headers
s/<h2>Compilot AI Assistant Management<\/h2>/<h2><?php echo esc_html__('Compilot AI Assistant Management', 'compilot-ai'); ?><\/h2>/g
s/<h2>Search Compilot AI Assistant<\/h2>/<h2><?php echo esc_html__('Search Compilot AI Assistant', 'compilot-ai'); ?><\/h2>/g

# Description paragraphs
s/Content is automatically synchronized to the knowledge graph when you publish or update posts and pages\./<?php echo esc_html__('Content is automatically synchronized to the knowledge graph when you publish or update posts and pages.', 'compilot-ai'); ?>/g
s/You can also manually sync all content from the Settings page\./<?php echo esc_html__('You can also manually sync all content from the Settings page.', 'compilot-ai'); ?>/g

# Button text
s/>Go to Settings</>><?php echo esc_html__('Go to Settings', 'compilot-ai'); ?></g
s/>Search</>><?php echo esc_html__('Search', 'compilot-ai'); ?></g

# Form labels
s/<label for="search-query">Query<\/label>/<label for="search-query"><?php echo esc_html__('Query', 'compilot-ai'); ?><\/label>/g
s/<label for="num-results">Number of Results<\/label>/<label for="num-results"><?php echo esc_html__('Number of Results', 'compilot-ai'); ?><\/label>/g
s/<label for="min-confidence">Minimum Confidence<\/label>/<label for="min-confidence"><?php echo esc_html__('Minimum Confidence', 'compilot-ai'); ?><\/label>/g

# Description text
s/Results with lower confidence will be filtered out\./<?php echo esc_html__('Results with lower confidence will be filtered out.', 'compilot-ai'); ?>/g
