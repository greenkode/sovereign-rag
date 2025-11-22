# SED script to add i18n wrappers to review-page.php

# Main heading
s/<h1>Review & Add Content<\/h1>/<h1><?php echo esc_html__('Review & Add Content', 'compilot-ai'); ?><\/h1>/g

# Tab navigation
s/<a href="#unanswered-queries" class="nav-tab nav-tab-active">Unanswered Queries<\/a>/<a href="#unanswered-queries" class="nav-tab nav-tab-active"><?php echo esc_html__('Unanswered Queries', 'compilot-ai'); ?><\/a>/g
s/<a href="#ingest-content" class="nav-tab">Add Content<\/a>/<a href="#ingest-content" class="nav-tab"><?php echo esc_html__('Add Content', 'compilot-ai'); ?><\/a>/g

# Section headers
s/<h2>Queries Needing Review<\/h2>/<h2><?php echo esc_html__('Queries Needing Review', 'compilot-ai'); ?><\/h2>/g
s/<h2>Add New Content<\/h2>/<h2><?php echo esc_html__('Add New Content', 'compilot-ai'); ?><\/h2>/g
s/<h3 style="margin-top: 0;">Manual Text Entry<\/h3>/<h3 style="margin-top: 0;"><?php echo esc_html__('Manual Text Entry', 'compilot-ai'); ?><\/h3>/g
s/<h3 style="margin-top: 0;">File Upload<\/h3>/<h3 style="margin-top: 0;"><?php echo esc_html__('File Upload', 'compilot-ai'); ?><\/h3>/g

# Table headers
s/<th style="width: 25%;">Query<\/th>/<th style="width: 25%;"><?php echo esc_html__('Query', 'compilot-ai'); ?><\/th>/g
s/<th style="width: 30%;">AI Response<\/th>/<th style="width: 30%;"><?php echo esc_html__('AI Response', 'compilot-ai'); ?><\/th>/g
s/<th style="width: 10%;">Timestamp<\/th>/<th style="width: 10%;"><?php echo esc_html__('Timestamp', 'compilot-ai'); ?><\/th>/g
s/<th style="width: 8%;">Confidence<\/th>/<th style="width: 8%;"><?php echo esc_html__('Confidence', 'compilot-ai'); ?><\/th>/g
s/<th style="width: 10%;">Used General Knowledge<\/th>/<th style="width: 10%;"><?php echo esc_html__('Used General Knowledge', 'compilot-ai'); ?><\/th>/g
s/<th style="width: 17%;">Actions<\/th>/<th style="width: 17%;"><?php echo esc_html__('Actions', 'compilot-ai'); ?><\/th>/g

# Labels
s/<label for="manual-title">Title<\/label>/<label for="manual-title"><?php echo esc_html__('Title', 'compilot-ai'); ?><\/label>/g
s/<label for="manual-content">Content<\/label>/<label for="manual-content"><?php echo esc_html__('Content', 'compilot-ai'); ?><\/label>/g
s/<label for="manual-url">Source URL (optional)<\/label>/<label for="manual-url"><?php echo esc_html__('Source URL (optional)', 'compilot-ai'); ?><\/label>/g
s/<label for="ingest-file">Select File<\/label>/<label for="ingest-file"><?php echo esc_html__('Select File', 'compilot-ai'); ?><\/label>/g

# Button values
s/value="Add to Knowledge Base"/value="<?php echo esc_attr__('Add to Knowledge Base', 'compilot-ai'); ?>"/g
s/value="Upload File"/value="<?php echo esc_attr__('Upload File', 'compilot-ai'); ?>"/g

# Messages
s/<p>Loading unanswered queries\.\.\.<\/p>/<p><?php echo esc_html__('Loading unanswered queries...', 'compilot-ai'); ?><\/p>/g
s/<p>No unanswered queries found\. Great job!<\/p>/<p><?php echo esc_html__('No unanswered queries found. Great job!', 'compilot-ai'); ?><\/p>/g
