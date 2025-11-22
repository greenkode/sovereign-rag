# Compilot AI Assistant Translations

This directory contains translation files for the Compilot AI Assistant WordPress plugin.

## Translation Files

The plugin uses the standard WordPress internationalization (i18n) system with the text domain `compilot-ai`.

### Available Translations

- **English (en_US)**: `compilot-ai-en_US.po` / `compilot-ai-en_US.mo`
- **Dutch (nl_NL)**: `compilot-ai-nl_NL.po` / `compilot-ai-nl_NL.mo`
- **German (de_DE)**: `compilot-ai-de_DE.po` / `compilot-ai-de_DE.mo`
- **French (fr_FR)**: `compilot-ai-fr_FR.po` / `compilot-ai-fr_FR.mo`
- **Spanish (es_ES)**: `compilot-ai-es_ES.po` / `compilot-ai-es_ES.mo`
- **Italian (it_IT)**: `compilot-ai-it_IT.po` / `compilot-ai-it_IT.mo`
- **Portuguese (pt_PT)**: `compilot-ai-pt_PT.po` / `compilot-ai-pt_PT.mo`

## How to Add a New Translation

### 1. Create a .po File

Create a new `.po` file for your language using the format `compilot-ai-{locale}.po`. For example:
- German: `compilot-ai-de_DE.po`
- French: `compilot-ai-fr_FR.po`
- Spanish: `compilot-ai-es_ES.po`

You can use the `compilot-ai-nl_NL.po` file as a template.

### 2. Translate the Strings

Edit the `.po` file and translate the `msgstr` values:

```po
msgid "Settings"
msgstr "Instellingen"  # Your translation here
```

### 3. Compile to .mo File

WordPress requires compiled `.mo` files. Use `msgfmt` to compile:

```bash
msgfmt -o compilot-ai-{locale}.mo compilot-ai-{locale}.po
```

For example:
```bash
msgfmt -o compilot-ai-de_DE.mo compilot-ai-de_DE.po
```

### 4. Test Your Translation

1. Place both `.po` and `.mo` files in this `languages/` directory
2. Set your WordPress site language to match your translation locale in `Settings > General`
3. The plugin menu items and interface will automatically use your translation

## Translation Tools

You can use these tools to create and edit translations:

- **Poedit**: https://poedit.net/ (GUI application, free)
- **Loco Translate**: WordPress plugin for in-dashboard translation
- **msgfmt**: Command-line tool (part of gettext package)

## Current Translatable Strings

The main menu items that are currently translatable:

- "Compilot AI Assistant" (main menu)
- "Settings" (submenu)
- "Review & Add Content" (submenu)

More strings can be added by wrapping text in `__()` or `_e()` functions with the `'compilot-ai'` text domain.

## Contributing Translations

If you'd like to contribute a translation:

1. Create the `.po` and `.mo` files for your language
2. Test them in WordPress
3. Submit a pull request to the repository

## Language Code Reference

Common WordPress language codes:

- Dutch (Netherlands): `nl_NL`
- German (Germany): `de_DE`
- French (France): `fr_FR`
- Spanish (Spain): `es_ES`
- Italian (Italy): `it_IT`
- Portuguese (Portugal): `pt_PT`
- English (UK): `en_GB`
- English (US): `en_US`

For more locale codes, see: https://make.wordpress.org/polyglots/teams/
