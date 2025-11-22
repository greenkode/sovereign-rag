# SED script to add i18n wrappers to settings page labels
# Usage: sed -i.bak -f apply-i18n.sed settings-page.php

# Labels in <label for="..."> tags
s/<label for="enable_general_knowledge">Allow General Knowledge<\/label>/<label for="enable_general_knowledge"><?php echo esc_html__('Allow General Knowledge', 'compilot-ai'); ?><\/label>/g
s/<label for="rag_persona">AI Personality<\/label>/<label for="rag_persona"><?php echo esc_html__('AI Personality', 'compilot-ai'); ?><\/label>/g
s/<label for="default_language">Default Response Language<\/label>/<label for="default_language"><?php echo esc_html__('Default Response Language', 'compilot-ai'); ?><\/label>/g
s/<label for="enable_chat_widget">Enable Chat Widget<\/label>/<label for="enable_chat_widget"><?php echo esc_html__('Enable Chat Widget', 'compilot-ai'); ?><\/label>/g
s/<label for="chat_widget_name">Chat Widget Name<\/label>/<label for="chat_widget_name"><?php echo esc_html__('Chat Widget Name', 'compilot-ai'); ?><\/label>/g
s/<label for="session_timeout_minutes">Session Inactivity Timeout<\/label>/<label for="session_timeout_minutes"><?php echo esc_html__('Session Inactivity Timeout', 'compilot-ai'); ?><\/label>/g
s/<label for="chat_greeting_message">Initial Greeting Message<\/label>/<label for="chat_greeting_message"><?php echo esc_html__('Initial Greeting Message', 'compilot-ai'); ?><\/label>/g
s/<label for="auto_sync">Auto-Sync Content<\/label>/<label for="auto_sync"><?php echo esc_html__('Auto-Sync Content', 'compilot-ai'); ?><\/label>/g
s/<label>Manual Sync<\/label>/<label><?php echo esc_html__('Manual Sync', 'compilot-ai'); ?><\/label>/g
s/<label for="widget_primary_color">Primary Color<\/label>/<label for="widget_primary_color"><?php echo esc_html__('Primary Color', 'compilot-ai'); ?><\/label>/g
s/<label for="widget_secondary_color">Secondary Color (Gradient)<\/label>/<label for="widget_secondary_color"><?php echo esc_html__('Secondary Color (Gradient)', 'compilot-ai'); ?><\/label>/g
s/<label for="widget_message_font_size">Message Font Size (px)<\/label>/<label for="widget_message_font_size"><?php echo esc_html__('Message Font Size (px)', 'compilot-ai'); ?><\/label>/g
s/<label for="widget_header_font_size">Header Font Size (px)<\/label>/<label for="widget_header_font_size"><?php echo esc_html__('Header Font Size (px)', 'compilot-ai'); ?><\/label>/g
s/<th scope="row">Preview<\/th>/<th scope="row"><?php echo esc_html__('Preview', 'compilot-ai'); ?><\/th>/g

# Button texts
s/>Sync All Published Content Now</>><?php echo esc_html__('Sync All Published Content Now', 'compilot-ai'); ?></g
s/>Clear Sync Status</>><?php echo esc_html__('Clear Sync Status', 'compilot-ai'); ?></g
s/>Widget Preview</>><?php echo esc_html__('Widget Preview', 'compilot-ai'); ?></g
