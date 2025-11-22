# SED script to add i18n wrappers to admin-page.php

# Section headers
s/<h2>Sovereign RAG Assistant Management<\/h2>/<h2><?php echo esc_html__('Sovereign RAG Assistant Management', 'sovereign-rag'); ?><\/h2>/g
s/<h2>Search Sovereign RAG Assistant<\/h2>/<h2><?php echo esc_html__('Search Sovereign RAG Assistant', 'sovereign-rag'); ?><\/h2>/g

# Description paragraphs
s/Content is automatically synchronized to the knowledge graph when you publish or update posts and pages\./<?php echo esc_html__('Content is automatically synchronized to the knowledge graph when you publish or update posts and pages.', 'sovereign-rag'); ?>/g
s/You can also manually sync all content from the Settings page\./<?php echo esc_html__('You can also manually sync all content from the Settings page.', 'sovereign-rag'); ?>/g

# Button text
s/>Go to Settings</>><?php echo esc_html__('Go to Settings', 'sovereign-rag'); ?></g
s/>Search</>><?php echo esc_html__('Search', 'sovereign-rag'); ?></g

# Form labels
s/<label for="search-query">Query<\/label>/<label for="search-query"><?php echo esc_html__('Query', 'sovereign-rag'); ?><\/label>/g
s/<label for="num-results">Number of Results<\/label>/<label for="num-results"><?php echo esc_html__('Number of Results', 'sovereign-rag'); ?><\/label>/g
s/<label for="min-confidence">Minimum Confidence<\/label>/<label for="min-confidence"><?php echo esc_html__('Minimum Confidence', 'sovereign-rag'); ?><\/label>/g

# Description text
s/Results with lower confidence will be filtered out\./<?php echo esc_html__('Results with lower confidence will be filtered out.', 'sovereign-rag'); ?>/g
