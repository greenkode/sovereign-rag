# Compilot AI Assistant - Translation Reference

This document shows all available translations for the Compilot AI Assistant plugin menu items.

## Translated Menu Items

| Language | Code | "Compilot AI Assistant" | "Settings" | "Review & Add Content" |
|----------|------|------------------------|------------|------------------------|
| **English** | en_US | Compilot AI Assistant | Settings | Review & Add Content |
| **Dutch** | nl_NL | Compilot AI Assistent | Instellingen | Controleer & Voeg Inhoud Toe |
| **German** | de_DE | Compilot KI-Assistent | Einstellungen | Überprüfen & Inhalt Hinzufügen |
| **French** | fr_FR | Assistant IA Compilot | Paramètres | Examiner & Ajouter du Contenu |
| **Spanish** | es_ES | Asistente IA Compilot | Configuración | Revisar y Añadir Contenido |
| **Italian** | it_IT | Assistente IA Compilot | Impostazioni | Rivedi e Aggiungi Contenuto |
| **Portuguese** | pt_PT | Assistente IA Compilot | Definições | Rever e Adicionar Conteúdo |

## How to Use

1. Go to **WordPress Admin → Settings → General**
2. Set **Site Language** to your preferred language
3. The plugin menu items will automatically display in that language

## File Structure

Each language has two files:
- **`.po` file**: Human-readable translation source
- **`.mo` file**: Compiled binary file used by WordPress

Example for Dutch:
```
languages/
├── compilot-ai-nl_NL.po  (source)
└── compilot-ai-nl_NL.mo  (compiled)
```

## Default Language

The plugin's **default AI response language** is set to **Dutch (nl)**.

You can change this in:
**WordPress Admin → Compilot AI Assistant → Settings → Default Response Language**

## Adding More Translations

To add a new language or update existing translations, see the [README.md](README.md) file in this directory.
