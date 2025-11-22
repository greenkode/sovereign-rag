# Graphiti Knowledge Graph Chat Widget

The chat widget provides a floating, interactive chat interface on your WordPress site for users to search and interact with your knowledge graph.

## Features

✅ **Floating Widget** - Appears on all pages (bottom-right corner)
✅ **Autocomplete** - Real-time suggestions as users type
✅ **Smart Search** - Full knowledge graph search with confidence scores
✅ **Satisfaction Prompts** - Automatic feedback requests for low-confidence results
✅ **Mobile Responsive** - Works great on all screen sizes
✅ **Beautiful UI** - Modern, gradient design with smooth animations

## Installation

The chat widget is automatically enabled when you activate the Graphiti Knowledge Graph plugin.

### Manual Enable/Disable

Add to your theme's `functions.php` or a custom plugin:

```php
// Disable chat widget
update_option('graphiti_enable_chat_widget', false);

// Enable chat widget
update_option('graphiti_enable_chat_widget', true);
```

## Configuration

The widget uses your existing plugin settings:

1. **API URL**: Settings → Graphiti Knowledge Graph → API URL
2. **Min Confidence**: Settings → Graphiti Knowledge Graph → Minimum Confidence

No additional configuration required!

## User Experience Flow

### 1. Widget Appears
- Floating purple gradient button in bottom-right corner
- Clicking opens the chat window

### 2. Autocomplete
- User types at least 2 characters
- Suggestions appear immediately (debounced 300ms)
- Shows top 5 matching entities/episodes
- Click suggestion to auto-fill and search

### 3. Search Results
- Press Enter or click Send button
- Results display with:
  - Fact/information
  - Source
  - Confidence score (color-coded)
- Empty results trigger feedback prompt

### 4. Satisfaction Prompts
Shown automatically when:
- No results found
- Low confidence results (< 50%)
- Similar query detected (within 30 mins)

### 5. Feedback Collection
- "Yes, helpful" or "No, not helpful" buttons
- Feedback sent to API for review
- Thank you message displayed

## Customization

### Colors & Styling

Edit `assets/css/chat-widget.css`:

```css
/* Change widget colors */
.graphiti-chat-toggle {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    /* Change to your brand colors */
}

/* Change confidence score colors */
.graphiti-confidence-high {
    background: #c6f6d5;  /* Green */
    color: #22543d;
}

.graphiti-confidence-medium {
    background: #fef3c7;  /* Yellow */
    color: #744210;
}

.graphiti-confidence-low {
    background: #fed7d7;  /* Red */
    color: #742a2a;
}
```

### Position

Change widget position in `assets/css/chat-widget.css`:

```css
/* Default: bottom-right */
.graphiti-chat-widget {
    bottom: 20px;
    right: 20px;
}

/* Bottom-left */
.graphiti-chat-widget {
    bottom: 20px;
    left: 20px;  /* Change right to left */
}

/* Top-right */
.graphiti-chat-widget {
    top: 20px;     /* Change bottom to top */
    right: 20px;
}
```

### Widget Size

Adjust dimensions in `assets/css/chat-widget.css`:

```css
.graphiti-chat-window {
    width: 380px;           /* Wider: 480px, Narrower: 320px */
    height: 600px;          /* Taller: 700px, Shorter: 500px */
}
```

### Messages & Text

Edit text in `assets/js/chat-widget.js`:

```javascript
// Welcome message
addMessage('Welcome! How can I help you today?', 'bot');

// No results message
addMessage('Sorry, I couldn\'t find anything. Try rephrasing?', 'bot');

// Error message
addMessage('Oops! Something went wrong. Please try again.', 'bot');
```

## API Endpoints Used

The widget communicates with these API endpoints:

### 1. Autocomplete
```http
GET /api/autocomplete?q=search&limit=5
```

Response:
```json
{
  "success": true,
  "query": "search",
  "suggestions": [
    {
      "text": "Entity Name",
      "summary": "Short description...",
      "type": "entity"
    }
  ],
  "count": 5
}
```

### 2. Search
```http
POST /api/search

{
  "query": "Who is Kamala Harris?",
  "num_results": 5,
  "min_confidence": 0
}
```

Response:
```json
{
  "success": true,
  "results": [...],
  "satisfaction_prompt": "Were these results helpful?",
  "auto_flagged": true,
  "similar_query_detected": false
}
```

### 3. Feedback
```http
POST /api/feedback

{
  "query": "Who is Kamala Harris?",
  "is_accurate": false,
  "feedback": "Marked as not helpful"
}
```

## Performance Optimization

### Debouncing
Autocomplete requests are debounced (300ms) to reduce API calls:

```javascript
// In assets/js/chat-widget.js
clearTimeout(autocompleteTimeout);
autocompleteTimeout = setTimeout(function() {
    fetchAutocomplete(query);
}, 300);  // Adjust delay: lower = faster, higher = fewer requests
```

### Result Limits
Default limits:
- Autocomplete: 5 suggestions
- Search results: 5 results

Adjust in `assets/js/chat-widget.js`:

```javascript
// Autocomplete limit
const apiUrl = graphitiChat.apiUrl + '/api/autocomplete?q=' +
    encodeURIComponent(query) + '&limit=10';  // Change 5 to 10

// Search results limit
data: JSON.stringify({
    query: query,
    num_results: 10,  // Change 5 to 10
    min_confidence: 0
}),
```

### Caching
To reduce server load, consider adding client-side caching:

```javascript
// Add to chat-widget.js
const searchCache = {};

function performSearch(query) {
    // Check cache first
    if (searchCache[query]) {
        displayResults(searchCache[query]);
        return;
    }

    // ... existing AJAX call ...
    success: function(response) {
        searchCache[query] = response;  // Cache response
        displayResults(response);
    }
}
```

## Accessibility

The widget includes ARIA labels for screen readers:

```html
<button aria-label="Open Knowledge Graph Chat">
<button aria-label="Close chat">
<button aria-label="Send message">
```

### Keyboard Navigation
- **Enter**: Send message
- **Esc**: Close widget (add custom handler)
- **Tab**: Navigate buttons/inputs

Add ESC key handler:

```javascript
// In assets/js/chat-widget.js initChatWidget()
$(document).on('keydown', function(e) {
    if (e.key === 'Escape' && $('#graphiti-chat-window').is(':visible')) {
        $('#graphiti-chat-window').hide();
    }
});
```

## Troubleshooting

### Widget Not Appearing

1. **Check if enabled**:
   ```php
   echo get_option('graphiti_enable_chat_widget'); // Should be true
   ```

2. **Check for JavaScript errors**:
   - Open browser console (F12)
   - Look for errors

3. **Verify API URL**:
   - Settings → Graphiti Knowledge Graph
   - Ensure API URL is correct

### Autocomplete Not Working

1. **Check API connection**:
   ```bash
   curl http://localhost:8000/api/autocomplete?q=test
   ```

2. **Check browser network tab**:
   - Open DevTools → Network
   - Type in widget input
   - Look for `/api/autocomplete` request

3. **Check CORS settings**:
   - API server must allow requests from WordPress domain

### Styles Not Loading

1. **Hard refresh**: Ctrl+F5 (Windows) or Cmd+Shift+R (Mac)

2. **Check file path**:
   ```php
   echo plugins_url('assets/css/chat-widget.css', __FILE__);
   ```

3. **Clear WordPress cache** (if using caching plugin)

## Advanced Features

### Add Typing Simulation
Make bot responses feel more natural:

```javascript
function addMessageWithDelay(content, type, delay) {
    $('#graphiti-typing').show();

    setTimeout(function() {
        $('#graphiti-typing').hide();
        addMessage(content, type);
    }, delay || 1000);
}
```

### Save Chat History
Store conversation in localStorage:

```javascript
// Save message
function addMessage(content, type, isHtml) {
    // ... existing code ...

    // Save to localStorage
    const history = JSON.parse(localStorage.getItem('graphiti_chat_history') || '[]');
    history.push({ content, type, timestamp: new Date().toISOString() });
    localStorage.setItem('graphiti_chat_history', JSON.stringify(history.slice(-50)));
}

// Load on init
function loadChatHistory() {
    const history = JSON.parse(localStorage.getItem('graphiti_chat_history') || '[]');
    history.forEach(msg => addMessage(msg.content, msg.type));
}
```

### Add Voice Input
Use Web Speech API:

```javascript
// Add microphone button
<button id="graphiti-voice-input">🎤</button>

// Add handler
$('#graphiti-voice-input').on('click', function() {
    const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();

    recognition.onresult = function(event) {
        const transcript = event.results[0][0].transcript;
        $('#graphiti-chat-input').val(transcript);
        sendMessage();
    };

    recognition.start();
});
```

## Best Practices

1. **Test on Mobile**: Widget is responsive but test on actual devices
2. **Monitor Performance**: Watch API response times
3. **Review Feedback**: Check admin dashboard for flagged queries
4. **Update Content**: Add more episodes to improve results
5. **Customize Branding**: Match widget colors to your site theme

## Browser Support

- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support
- IE11: Not supported (use polyfills)

## Security

The widget:
- Escapes all user input to prevent XSS
- Uses WordPress nonces for AJAX requests
- Validates all API responses
- Sanitizes displayed content

## Updates

When updating the plugin:
1. Clear browser cache
2. Test autocomplete and search
3. Verify styling on mobile
4. Check admin feedback dashboard

## Support

For issues or questions:
1. Check browser console for errors
2. Verify API server is running
3. Review this documentation
4. Check GitHub issues

## Roadmap

Planned features:
- [ ] Multiple language support
- [ ] Custom themes/skins
- [ ] Admin widget customization panel
- [ ] Quick replies/suggested questions
- [ ] File attachment support
- [ ] Export chat transcript
- [ ] Dark mode toggle
