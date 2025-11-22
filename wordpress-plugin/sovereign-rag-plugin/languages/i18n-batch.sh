#!/bin/bash
# Batch script to add i18n wrappers to settings page
# This is a reference for the strings that need translation

cat << 'EOF'
The following are key strings that need __() or esc_html__() wrappers in settings-page.php:

LABELS (use inside <label> tags):
- API Server URL (Backend)
- API Server URL (Frontend)
- Minimum Confidence Threshold
- Enable AI Answers
- Allow General Knowledge
- AI Personality
- Default Response Language
- Enable Chat Widget
- Chat Widget Name
- Session Inactivity Timeout
- Initial Greeting Message
- Auto-Sync Content
- Manual Sync
- Primary Color
- Secondary Color (Gradient)
- Message Font Size (px)
- Header Font Size (px)
- Preview

BUTTONS:
- Sync All Published Content Now
- Clear Sync Status
- Widget Preview

OPTION VALUES (persona select):
- Customer Service (Friendly & Helpful)
- Professional Expert (Polished & Knowledgeable)
- Casual Friend (Warm & Relatable)
- Technical Specialist (Detailed & Thorough)
- Concise Assistant (Brief & Direct)
- Educational Tutor (Clear & Encouraging)

OPTION VALUES (language select):
- Auto-detect from user input
- English
- Dutch (Nederlands)
- German (Deutsch)
- French (Français)
- Spanish (Español)
- Italian (Italiano)
- Portuguese (Português)

All description texts in <p class="description"> tags also need esc_html__() wrappers.

COMPLETE i18n would require wrapping approximately 60+ strings.
Due to file size, this is best done with a PHP script or manual editing in batches.
EOF
