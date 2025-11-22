#!/usr/bin/env python3
"""
Helper script to generate internationalized strings for Compilot AI plugin
Outputs translation entries for .po files
"""

strings = {
    # Success messages
    "Settings saved successfully!": "Settings saved successfully!",

    # Section headers
    "AI Answer Generation (RAG)": "AI Answer Generation (RAG)",
    "Chat Widget Settings": "Chat Widget Settings",
    "Content Synchronization": "Content Synchronization",
    "Chat Widget Look & Feel": "Chat Widget Look & Feel",
    "API Connection Test": "API Connection Test",

    # Field labels
    "API Server URL (Backend)": "API Server URL (Backend)",
    "API Server URL (Frontend)": "API Server URL (Frontend)",
    "Minimum Confidence Threshold": "Minimum Confidence Threshold",
    "Enable AI Answers": "Enable AI Answers",
    "Allow General Knowledge": "Allow General Knowledge",
    "AI Personality": "AI Personality",
    "Default Response Language": "Default Response Language",
    "Enable Chat Widget": "Enable Chat Widget",
    "Chat Widget Name": "Chat Widget Name",
    "Session Inactivity Timeout": "Session Inactivity Timeout",
    "Initial Greeting Message": "Initial Greeting Message",
    "Auto-Sync Content": "Auto-Sync Content",
    "Manual Sync": "Manual Sync",
    "Primary Color": "Primary Color",
    "Secondary Color (Gradient)": "Secondary Color (Gradient)",
    "Message Font Size (px)": "Message Font Size (px)",
    "Header Font Size (px)": "Header Font Size (px)",
    "Preview": "Preview",

    # Buttons
    "Sync All Published Content Now": "Sync All Published Content Now",
    "Clear Sync Status": "Clear Sync Status",
    "Save Settings": "Save Settings",
    "Test API Connection": "Test API Connection",
    "Widget Preview": "Widget Preview",

    # Time
    "minutes": "minutes",

    # Personas
    "Customer Service (Friendly & Helpful)": "Customer Service (Friendly & Helpful)",
    "Professional Expert (Polished & Knowledgeable)": "Professional Expert (Polished & Knowledgeable)",
    "Casual Friend (Warm & Relatable)": "Casual Friend (Warm & Relatable)",
    "Technical Specialist (Detailed & Thorough)": "Technical Specialist (Detailed & Thorough)",
    "Concise Assistant (Brief & Direct)": "Concise Assistant (Brief & Direct)",
    "Educational Tutor (Clear & Encouraging)": "Educational Tutor (Clear & Encouraging)",

    # Languages
    "Auto-detect from user input": "Auto-detect from user input",
    "English": "English",
    "Dutch (Nederlands)": "Dutch (Nederlands)",
    "German (Deutsch)": "German (Deutsch)",
    "French (Français)": "French (Français)",
    "Spanish (Español)": "Spanish (Español)",
    "Italian (Italiano)": "Italian (Italiano)",
    "Portuguese (Português)": "Portuguese (Português)",
}

# Generate .po entries
for english, msgstr in strings.items():
    print(f'msgid "{english}"')
    print(f'msgstr "{msgstr}"')
    print()
