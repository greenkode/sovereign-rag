# Graphiti Knowledge Graph WordPress Plugin

A WordPress plugin that interfaces with the Graphiti knowledge graph system, allowing users to add information, search the knowledge base, and provide feedback on result accuracy.

## Features

- ✅ **Add Information**: Submit text or JSON content to the knowledge graph
- ✅ **Semantic Search**: Search using natural language queries
- ✅ **Confidence Scoring**: See reliability scores for each result
- ✅ **Feedback System**: Mark results as accurate/inaccurate
- ✅ **Manual Review**: Admin interface for reviewing flagged results
- ✅ **Statistics Dashboard**: Monitor graph growth and activity
- ✅ **Shortcodes**: Easy integration with pages and posts
- ✅ **Temporal Awareness**: See when facts were valid

## Quick Start

### 1. Install the Plugin

Copy this folder to your WordPress plugins directory:

```bash
cp -r graphiti-knowledge-graph /path/to/wordpress/wp-content/plugins/
```

### 2. Activate

1. Log in to WordPress Admin
2. Navigate to **Plugins**
3. Find "Graphiti Knowledge Graph"
4. Click **Activate**

### 3. Configure

1. Go to **Knowledge Graph** → **Settings**
2. Enter your API Server URL (default: `http://localhost:8000`)
3. Click **Save Settings**
4. Test the connection

## Shortcodes

### Search Form

```
[graphiti_search placeholder="Search..." button_text="Search"]
```

### Submit Form

```
[graphiti_submit button_text="Share Knowledge"]
```

## Requirements

- WordPress 5.0+
- PHP 7.4+
- Running Graphiti API server (see main documentation)

## File Structure

```
graphiti-knowledge-graph/
├── graphiti-knowledge-graph.php  # Main plugin file
├── assets/
│   ├── css/
│   │   ├── admin.css             # Admin styles
│   │   └── frontend.css          # Frontend styles
│   └── js/
│       ├── admin.js              # Admin functionality
│       └── frontend.js           # Frontend functionality
├── templates/
│   ├── admin-page.php            # Main admin dashboard
│   ├── review-page.php           # Review queue interface
│   ├── settings-page.php         # Plugin settings
│   ├── search-form.php           # Search widget template
│   └── submit-form.php           # Submission form template
└── README.md                     # This file
```

## Support

See the main [WORDPRESS_PLUGIN_GUIDE.md](../../WORDPRESS_PLUGIN_GUIDE.md) for detailed installation instructions, troubleshooting, and API documentation.

## License

GPL v2 or later

## Version

1.0.0
