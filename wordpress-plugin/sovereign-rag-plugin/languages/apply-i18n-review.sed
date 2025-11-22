# SED script to add i18n wrappers to review-page.php

# Main heading
s/<h1>Review & Add Content<\/h1>/<h1><?php echo esc_html__('Review & Add Content', 'sovereign-rag'); ?><\/h1>/g

# Tab navigation
s/<a href="#unanswered-queries" class="nav-tab nav-tab-active">Unanswered Queries<\/a>/<a href="#unanswered-queries" class="nav-tab nav-tab-active"><?php echo esc_html__('Unanswered Queries', 'sovereign-rag'); ?><\/a>/g
s/<a href="#ingest-content" class="nav-tab">Add Content<\/a>/<a href="#ingest-content" class="nav-tab"><?php echo esc_html__('Add Content', 'sovereign-rag'); ?><\/a>/g

# Section headers
s/<h2>Queries Needing Review<\/h2>/<h2><?php echo esc_html__('Queries Needing Review', 'sovereign-rag'); ?><\/h2>/g
s/<h2>Add New Content<\/h2>/<h2><?php echo esc_html__('Add New Content', 'sovereign-rag'); ?><\/h2>/g
s/<h3 style="margin-top: 0;">Manual Text Entry<\/h3>/<h3 style="margin-top: 0;"><?php echo esc_html__('Manual Text Entry', 'sovereign-rag'); ?><\/h3>/g
s/<h3 style="margin-top: 0;">File Upload<\/h3>/<h3 style="margin-top: 0;"><?php echo esc_html__('File Upload', 'sovereign-rag'); ?><\/h3>/g

# Table headers
s/<th style="width: 25%;">Query<\/th>/<th style="width: 25%;"><?php echo esc_html__('Query', 'sovereign-rag'); ?><\/th>/g
s/<th style="width: 30%;">AI Response<\/th>/<th style="width: 30%;"><?php echo esc_html__('AI Response', 'sovereign-rag'); ?><\/th>/g
s/<th style="width: 10%;">Timestamp<\/th>/<th style="width: 10%;"><?php echo esc_html__('Timestamp', 'sovereign-rag'); ?><\/th>/g
s/<th style="width: 8%;">Confidence<\/th>/<th style="width: 8%;"><?php echo esc_html__('Confidence', 'sovereign-rag'); ?><\/th>/g
s/<th style="width: 10%;">Used General Knowledge<\/th>/<th style="width: 10%;"><?php echo esc_html__('Used General Knowledge', 'sovereign-rag'); ?><\/th>/g
s/<th style="width: 17%;">Actions<\/th>/<th style="width: 17%;"><?php echo esc_html__('Actions', 'sovereign-rag'); ?><\/th>/g

# Labels
s/<label for="manual-title">Title<\/label>/<label for="manual-title"><?php echo esc_html__('Title', 'sovereign-rag'); ?><\/label>/g
s/<label for="manual-content">Content<\/label>/<label for="manual-content"><?php echo esc_html__('Content', 'sovereign-rag'); ?><\/label>/g
s/<label for="manual-url">Source URL (optional)<\/label>/<label for="manual-url"><?php echo esc_html__('Source URL (optional)', 'sovereign-rag'); ?><\/label>/g
s/<label for="ingest-file">Select File<\/label>/<label for="ingest-file"><?php echo esc_html__('Select File', 'sovereign-rag'); ?><\/label>/g

# Button values
s/value="Add to Knowledge Base"/value="<?php echo esc_attr__('Add to Knowledge Base', 'sovereign-rag'); ?>"/g
s/value="Upload File"/value="<?php echo esc_attr__('Upload File', 'sovereign-rag'); ?>"/g

# Messages
s/<p>Loading unanswered queries\.\.\.<\/p>/<p><?php echo esc_html__('Loading unanswered queries...', 'sovereign-rag'); ?><\/p>/g
s/<p>No unanswered queries found\. Great job!<\/p>/<p><?php echo esc_html__('No unanswered queries found. Great job!', 'sovereign-rag'); ?><\/p>/g
