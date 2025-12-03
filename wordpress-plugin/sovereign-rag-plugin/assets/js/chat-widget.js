/**
 * Sovereign RAG Assistant Chat Widget with AG2 Conversational Agent
 */

(function($) {
    'use strict';

    let currentQuery = '';
    let autocompleteTimeout = null;
    let currentSatisfactionPrompt = null;

    // AG2 Conversational Agent state
    let chatSessionId = null;
    let isAgentMode = true; // Use AG2 agent by default
    let agentInitialized = false;
    let lastEscalationPrompt = null;

    // Inactivity timeout
    let inactivityTimeout = null;
    let inactivityWarningShown = false;
    // Convert minutes from settings to milliseconds
    const INACTIVITY_DELAY = (typeof sovereignragChat !== 'undefined' ? sovereignragChat.sessionTimeoutMinutes : 5) * 60 * 1000; // Default: 5 minutes

    // Close prompt timeout (60 seconds)
    let closePromptTimeout = null;
    const CLOSE_PROMPT_DELAY = 60 * 1000; // 60 seconds

    // Inactivity prompt timeout (60 seconds)
    let inactivityPromptTimeout = null;
    const INACTIVITY_PROMPT_DELAY = 60 * 1000; // 60 seconds

    // Session persistence keys
    const SESSION_STORAGE_KEY = 'sovereignrag_chat_session';
    const MESSAGES_STORAGE_KEY = 'sovereignrag_chat_messages';
    const SESSION_TIMESTAMP_KEY = 'sovereignrag_chat_timestamp';
    const LANGUAGE_STORAGE_KEY = 'sovereignrag_language_preference';

    // Language preference state (loaded from localStorage)
    let selectedLanguage = null;

    /**
     * Build headers with JWT Bearer token authentication for API requests
     *
     * SECURITY: Uses JWT token instead of exposing API key to browser
     */
    function getAuthHeaders() {
        const headers = {};

        if (typeof sovereignragChat !== 'undefined' && sovereignragChat.jwtToken) {
            // Use Bearer token authentication (JWT)
            headers['Authorization'] = 'Bearer ' + sovereignragChat.jwtToken;
        } else {
            console.error('Sovereign RAG: JWT token not available - authentication will fail');
            console.error('Sovereign RAG: sovereignragChat object:', typeof sovereignragChat !== 'undefined' ? sovereignragChat : 'undefined');
        }

        return headers;
    }

    /**
     * Refresh JWT token via WordPress AJAX
     * Returns a Promise that resolves with the new token or rejects on error
     */
    function refreshJwtToken() {
        console.log('Sovereign RAG: Refreshing expired JWT token...');

        return $.ajax({
            url: sovereignragChat.ajaxUrl,
            method: 'POST',
            data: {
                action: 'sovereignrag_refresh_token',
                nonce: sovereignragChat.nonce
            }
        }).then(function(response) {
            if (response.success && response.data.token) {
                // Update the global token
                sovereignragChat.jwtToken = response.data.token;
                console.log('Sovereign RAG: JWT token successfully refreshed');
                return response.data.token;
            } else {
                console.error('Sovereign RAG: Token refresh failed', response);
                throw new Error('Token refresh failed');
            }
        }).catch(function(error) {
            console.error('Sovereign RAG: Error refreshing token:', error);
            throw error;
        });
    }

    /**
     * Make an API request with automatic token refresh on 403 errors
     * Wraps $.ajax() to handle expired tokens transparently
     *
     * @param {Object} ajaxOptions - jQuery ajax options
     * @param {number} retryCount - Internal retry counter (don't pass this)
     * @returns {Promise} - jQuery Deferred/Promise
     */
    function apiRequest(ajaxOptions, retryCount) {
        retryCount = retryCount || 0;

        return $.ajax(ajaxOptions).catch(function(jqXHR, textStatus, errorThrown) {
            // If we get a 403 (Forbidden) and haven't retried yet, refresh token and retry
            if (jqXHR.status === 403 && retryCount === 0) {
                console.log('Sovereign RAG: Got 403 error, refreshing token and retrying...');

                return refreshJwtToken().then(function() {
                    // Update headers with new token
                    ajaxOptions.headers = getAuthHeaders();
                    // Retry the request once
                    return apiRequest(ajaxOptions, 1);
                });
            }

            // For all other errors or if retry already failed, propagate the error
            return $.Deferred().reject(jqXHR, textStatus, errorThrown);
        });
    }

    $(document).ready(function() {
        console.log('Session timeout set to: ' + (INACTIVITY_DELAY / 60000) + ' minutes');

        // Load language preference from WordPress setting (defaults to 'nl' or whatever admin configured)
        console.log('WordPress defaultLanguage setting:', sovereignragChat.defaultLanguage);
        selectedLanguage = sovereignragChat.defaultLanguage || 'auto';
        updateLanguageDisplay(selectedLanguage);
        console.log('Language initialized to:', selectedLanguage);

        // Try to restore previous session before initializing
        const sessionRestored = restoreSession();

        initChatWidget();

        // Automatically open chat window if session was restored
        if (sessionRestored) {
            console.log('Session restored - automatically opening chat window');
            $('#sovereignrag-chat-window').show();
            // Start inactivity timer for restored session
            resetInactivityTimer();
        }
    });

    /**
     * Restore previous session from localStorage if valid
     */
    function restoreSession() {
        try {
            const savedSessionId = localStorage.getItem(SESSION_STORAGE_KEY);
            const savedTimestamp = localStorage.getItem(SESSION_TIMESTAMP_KEY);
            const savedMessages = localStorage.getItem(MESSAGES_STORAGE_KEY);

            if (!savedSessionId || !savedTimestamp) {
                return false;
            }

            // Check if session has expired (use same timeout as inactivity)
            const now = new Date().getTime();
            const sessionAge = now - parseInt(savedTimestamp);

            if (sessionAge > INACTIVITY_DELAY) {
                console.log('Previous session expired, clearing...');
                clearSessionStorage();
                return false;
            }

            // Restore session
            chatSessionId = savedSessionId;
            agentInitialized = true;
            console.log('Restored chat session:', chatSessionId);

            // Restore messages
            if (savedMessages) {
                const messages = JSON.parse(savedMessages);
                const $messages = $('#sovereignrag-chat-messages');

                // Remove welcome message if present
                $('.graphiti-chat-welcome').remove();

                // Restore each message
                messages.forEach(function(msg) {
                    let $message = $('<div class="graphiti-message graphiti-message-' + msg.type + '"></div>');
                    let $bubble = $('<div class="graphiti-message-bubble"></div>');

                    if (msg.isHtml) {
                        $bubble.html(msg.content);
                    } else {
                        $bubble.text(msg.content);
                    }

                    $message.append($bubble);
                    $message.append('<div class="graphiti-message-time">' + msg.time + '</div>');
                    $messages.append($message);
                });

                // Scroll to bottom to show latest messages
                $messages.scrollTop($messages[0].scrollHeight);

                console.log('Restored ' + messages.length + ' messages');
            }

            // Update timestamp to prevent immediate expiration
            saveSessionTimestamp();

            // Return true to indicate session was restored
            return true;

        } catch (e) {
            console.error('Error restoring session:', e);
            clearSessionStorage();
            return false;
        }
    }

    /**
     * Save session ID to localStorage
     */
    function saveSessionId(sessionId) {
        try {
            localStorage.setItem(SESSION_STORAGE_KEY, sessionId);
            saveSessionTimestamp();
        } catch (e) {
            console.error('Error saving session:', e);
        }
    }

    /**
     * Update session timestamp (last activity)
     */
    function saveSessionTimestamp() {
        try {
            localStorage.setItem(SESSION_TIMESTAMP_KEY, new Date().getTime().toString());
        } catch (e) {
            console.error('Error saving timestamp:', e);
        }
    }

    /**
     * Save chat messages to localStorage
     */
    function saveMessages() {
        try {
            const messages = [];
            $('#sovereignrag-chat-messages .graphiti-message').each(function() {
                const $msg = $(this);
                const type = $msg.hasClass('graphiti-message-user') ? 'user' : 'bot';
                const $bubble = $msg.find('.graphiti-message-bubble');
                const time = $msg.find('.graphiti-message-time').text();

                // Check if content is HTML (has child elements)
                const isHtml = $bubble.children().length > 0;
                const content = isHtml ? $bubble.html() : $bubble.text();

                messages.push({
                    type: type,
                    content: content,
                    time: time,
                    isHtml: isHtml
                });
            });

            localStorage.setItem(MESSAGES_STORAGE_KEY, JSON.stringify(messages));
        } catch (e) {
            console.error('Error saving messages:', e);
        }
    }

    /**
     * Clear session storage
     */
    function clearSessionStorage() {
        try {
            localStorage.removeItem(SESSION_STORAGE_KEY);
            localStorage.removeItem(MESSAGES_STORAGE_KEY);
            localStorage.removeItem(SESSION_TIMESTAMP_KEY);
            console.log('Session storage cleared');
        } catch (e) {
            console.error('Error clearing session storage:', e);
        }
    }

    function initChatWidget() {
        // Toggle chat window
        $('#sovereignrag-chat-toggle').on('click', function() {
            const wasHidden = !$('#sovereignrag-chat-window').is(':visible');
            $('#sovereignrag-chat-window').toggle();

            if (wasHidden && $('#sovereignrag-chat-window').is(':visible')) {
                $('#sovereignrag-chat-input').focus();

                // Initialize agent session when chat opens for the first time
                if (isAgentMode && !agentInitialized && !chatSessionId) {
                    initializeAgentSession();
                }

                // If we have a restored session, restart inactivity timer
                if (chatSessionId && agentInitialized) {
                    resetInactivityTimer();
                }
            }
        });

        // Close chat window - show confirmation prompt if session is active
        $('#sovereignrag-chat-close').on('click', function() {
            if (chatSessionId && agentInitialized) {
                showClosePrompt();
            } else {
                // No active session - just clear history and hide
                clearSessionStorage();
                resetChatWidget();
                $('#sovereignrag-chat-window').hide();
            }
        });

        // Handle language toggle button click
        $('#sovereignrag-language-toggle').on('click', function(e) {
            e.stopPropagation();
            const dropdown = $('#sovereignrag-language-dropdown');
            dropdown.toggle();
        });

        // Handle language option selection
        $('.graphiti-language-option').on('click', function() {
            const lang = $(this).data('lang');
            selectedLanguage = lang;

            // Save to localStorage
            try {
                localStorage.setItem(LANGUAGE_STORAGE_KEY, lang);
                console.log('Language preference changed to:', lang);
            } catch (e) {
                console.error('Error saving language preference:', e);
            }

            // Update display
            updateLanguageDisplay(lang);

            // Close dropdown
            $('#sovereignrag-language-dropdown').hide();
        });

        // Close dropdown when clicking outside
        $(document).on('click', function(e) {
            if (!$(e.target).closest('.graphiti-language-selector').length) {
                $('#sovereignrag-language-dropdown').hide();
            }
        });

        // Handle input changes for autocomplete and auto-resize
        $('#sovereignrag-chat-input').on('input', function() {
            // Auto-resize textarea
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 120) + 'px';

            // Autocomplete
            const query = $(this).val().trim();
            if (query.length >= 2) {
                // Debounce autocomplete requests
                clearTimeout(autocompleteTimeout);
                autocompleteTimeout = setTimeout(function() {
                    fetchAutocomplete(query);
                }, 300);
            } else {
                hideAutocomplete();
            }
        });

        // Handle autocomplete selection
        $(document).on('click', '.graphiti-autocomplete-item', function() {
            const text = $(this).find('.graphiti-autocomplete-text').text();
            $('#sovereignrag-chat-input').val(text);
            hideAutocomplete();
            sendMessage();
        });

        // Handle send button click
        $('#sovereignrag-chat-send').on('click', function() {
            sendMessage();
        });

        // Handle Enter key for textarea (Shift+Enter = new line, Enter = send)
        $('#sovereignrag-chat-input').on('keydown', function(e) {
            if (e.which === 13 && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        // Handle satisfaction prompt buttons
        $(document).on('click', '.graphiti-satisfaction-buttons button', function() {
            const feedback = $(this).data('feedback');
            submitSatisfactionFeedback(feedback);
        });

        // Handle inactivity prompt buttons
        $(document).on('click', '.graphiti-btn-still-here', function() {
            // Clear the auto-close timeout
            if (inactivityPromptTimeout) {
                clearTimeout(inactivityPromptTimeout);
                inactivityPromptTimeout = null;
            }

            // Remove the entire message bubble containing the inactivity prompt
            $(this).closest('.graphiti-message').remove();
            resetInactivityTimer();
        });

        $(document).on('click', '.graphiti-btn-end-inactive', function() {
            // Clear the auto-close timeout
            if (inactivityPromptTimeout) {
                clearTimeout(inactivityPromptTimeout);
                inactivityPromptTimeout = null;
            }

            // Remove the entire message bubble containing the inactivity prompt
            $(this).closest('.graphiti-message').remove();
            closeSession();
        });

        // Close autocomplete when clicking outside
        $(document).on('click', function(e) {
            if (!$(e.target).closest('.graphiti-chat-input-wrapper, .graphiti-autocomplete').length) {
                hideAutocomplete();
            }
        });
    }

    function fetchAutocomplete(query) {
        const apiUrl = sovereignragChat.apiUrl + '/api/autocomplete?q=' + encodeURIComponent(query) + '&limit=5';

        apiRequest({
            url: apiUrl,
            method: 'GET',
            headers: getAuthHeaders()
        }).done(function(response) {
            if (response.success && response.suggestions.length > 0) {
                showAutocomplete(response.suggestions);
            } else {
                hideAutocomplete();
            }
        }).fail(function() {
            hideAutocomplete();
        });
    }

    function showAutocomplete(suggestions) {
        const $autocomplete = $('#sovereignrag-autocomplete');
        $autocomplete.empty();

        suggestions.forEach(function(suggestion) {
            const $item = $('<div class="graphiti-autocomplete-item"></div>');

            $item.append('<div class="graphiti-autocomplete-type">' + suggestion.type + '</div>');
            $item.append('<div class="graphiti-autocomplete-text">' + escapeHtml(suggestion.text) + '</div>');

            if (suggestion.summary) {
                $item.append('<div class="graphiti-autocomplete-summary">' + escapeHtml(suggestion.summary) + '</div>');
            }

            $autocomplete.append($item);
        });

        $autocomplete.show();
    }

    function hideAutocomplete() {
        $('#sovereignrag-autocomplete').hide().empty();
    }

    function sendMessage() {
        const query = $('#sovereignrag-chat-input').val().trim();

        if (!query) {
            return;
        }

        currentQuery = query;

        // Add user message to chat
        addMessage(query, 'user');

        // Clear input and reset height
        const $input = $('#sovereignrag-chat-input');
        $input.val('');
        $input.css('height', 'auto');

        // Hide autocomplete
        hideAutocomplete();

        // Hide any existing satisfaction prompt
        $('#sovereignrag-satisfaction-prompt').hide();

        // Show typing indicator
        $('#sovereignrag-typing').show();

        // Reset inactivity timer
        resetInactivityTimer();

        // Route to agent or traditional search based on mode
        if (isAgentMode && chatSessionId) {
            sendAgentMessage(query);
        } else if (isAgentMode && !chatSessionId) {
            // Initialize session with language detection from first message
            initializeAgentSession(query);
        } else {
            performSearch(query);
        }
    }

    /**
     * Reset inactivity timer
     */
    function resetInactivityTimer() {
        // Clear existing timer
        if (inactivityTimeout) {
            clearTimeout(inactivityTimeout);
        }

        // Reset warning flag
        inactivityWarningShown = false;

        // Update activity timestamp
        if (chatSessionId && agentInitialized) {
            saveSessionTimestamp();
        }

        // Only start timer if session is active
        if (!chatSessionId || !agentInitialized) {
            return;
        }

        // Start new timer
        inactivityTimeout = setTimeout(function() {
            if (chatSessionId && !inactivityWarningShown) {
                showInactivityPrompt();
            }
        }, INACTIVITY_DELAY);
    }

    /**
     * Show inactivity prompt
     * Auto-closes after 60 seconds if no response
     */
    function showInactivityPrompt() {
        inactivityWarningShown = true;

        const promptHtml = `
            <div class="graphiti-inactivity-prompt">
                <p>Still there? Would you like to continue or end this chat?</p>
                <div class="graphiti-inactivity-buttons">
                    <button class="graphiti-btn graphiti-btn-still-here">
                        👋 I'm still here
                    </button>
                    <button class="graphiti-btn graphiti-btn-end-inactive">
                        ✓ End chat
                    </button>
                </div>
            </div>
        `;

        addMessage(promptHtml, 'bot', true);

        // Clear any existing timeout
        if (inactivityPromptTimeout) {
            clearTimeout(inactivityPromptTimeout);
            inactivityPromptTimeout = null;
        }

        // Start 60-second timeout to auto-close
        inactivityPromptTimeout = setTimeout(function() {
            console.log('Inactivity prompt timeout - auto-closing session after 60 seconds');

            // Hide the prompt
            $('.graphiti-inactivity-prompt').fadeOut(function() {
                $(this).parent().remove(); // Remove the entire message
            });

            // Show session ended message
            addMessage('This chat session has ended due to inactivity. Feel free to start a new conversation anytime!', 'bot');

            // Close the session
            closeSession();
        }, INACTIVITY_PROMPT_DELAY);
    }

    /**
     * Update language display (flag, badge, tooltip, active state)
     * @param {string} lang - Language code (e.g., 'auto', 'en', 'nl', 'de', etc.)
     */
    function updateLanguageDisplay(lang) {
        // Language metadata mapping
        const languages = {
            'auto': { flag: '🌐', code: 'AUTO', name: 'Auto-detect' },
            'en': { flag: '🇬🇧', code: 'EN', name: 'English' },
            'nl': { flag: '🇳🇱', code: 'NL', name: 'Nederlands' },
            'de': { flag: '🇩🇪', code: 'DE', name: 'Deutsch' },
            'fr': { flag: '🇫🇷', code: 'FR', name: 'Français' },
            'es': { flag: '🇪🇸', code: 'ES', name: 'Español' },
            'it': { flag: '🇮🇹', code: 'IT', name: 'Italiano' },
            'pt': { flag: '🇵🇹', code: 'PT', name: 'Português' }
        };

        const langData = languages[lang] || languages['auto'];

        // Update toggle button display
        $('#sovereignrag-language-current-flag').text(langData.flag);
        $('#sovereignrag-language-current-badge').text(langData.code);
        $('.graphiti-language-tooltip').text(langData.name);

        // Update active state in dropdown
        $('.graphiti-language-option').removeClass('active');
        $(`.graphiti-language-option[data-lang="${lang}"]`).addClass('active');

        console.log('Language display updated:', langData.name);
    }

    /**
     * Get language to use for AI responses
     * Priority:
     * 1. User's manual selection from dropdown (overrides everything)
     * 2. If manual selection is "auto", return null and let AI detect from message
     * 3. Fall back to configured defaultLanguage
     * 4. Fall back to WordPress language or browser language
     */
    function getResponseLanguage() {
        // PRIORITY 1: Check user's manual language selection from dropdown
        if (selectedLanguage) {
            // If user selected auto-detect, return null to let AI detect language
            if (selectedLanguage === 'auto') {
                console.log('User selected auto-detect - AI will detect from user message');
                return null;
            }
            // User selected a specific language - use it (this overrides everything)
            console.log('Using user-selected language:', selectedLanguage);
            return selectedLanguage;
        }

        // PRIORITY 2: Fall back to configured default language
        const defaultLang = sovereignragChat.defaultLanguage || 'auto';

        // If auto-detect is enabled, return null to let AI detect language
        if (defaultLang === 'auto') {
            console.log('Language set to auto-detect - AI will detect from user message');
            return null;
        }

        // If a specific default language is set, use it
        if (defaultLang !== 'auto') {
            console.log('Using configured default language:', defaultLang);
            return defaultLang;
        }

        // PRIORITY 3: Fall back to WordPress or browser language
        const fallbackLang = getBrowserLanguage();
        console.log('Using fallback language:', fallbackLang);
        return fallbackLang;
    }

    /**
     * Get browser or WordPress language (first 2 characters of locale)
     */
    function getBrowserLanguage() {
        // Use WordPress language if set, otherwise detect from browser
        if (sovereignragChat.language) {
            // Extract first 2 characters (e.g., "en_US" -> "en")
            return sovereignragChat.language.substring(0, 2).toLowerCase();
        }

        // Get browser language (e.g., "en-US" -> "en", "nl-NL" -> "nl")
        const browserLang = navigator.language || navigator.userLanguage;
        return browserLang ? browserLang.substring(0, 2).toLowerCase() : 'en';
    }

    /**
     * Initialize AG2 chat agent session
     * @param {string} firstMessage - Optional first user message for language detection
     */
    function initializeAgentSession(firstMessage) {
        const apiUrl = sovereignragChat.apiUrl + '/api/agent/chat/start';
        const responseLanguage = getResponseLanguage();

        console.log('Initializing session with language:', responseLanguage);

        apiRequest({
            url: apiUrl,
            method: 'POST',
            contentType: 'application/json',
            headers: getAuthHeaders(),
            data: JSON.stringify({
                persona: sovereignragChat.ragPersona || 'customer_service',
                language: responseLanguage
            })
        }).done(function(response) {
            if (response.session_id) {
                chatSessionId = response.session_id;
                agentInitialized = true;

                // Save session to localStorage for persistence across pages
                saveSessionId(chatSessionId);

                // Keep the initial greeting visible (already shown in template)
                // Backend doesn't send greeting - frontend handles it

                // Start inactivity timer
                resetInactivityTimer();

                // If we have a first message, send it now
                if (firstMessage) {
                    sendAgentMessage(firstMessage);
                }
            } else {
                console.error('Failed to initialize agent session');
                // Fall back to traditional search mode
                isAgentMode = false;
                // Hide typing indicator
                $('#sovereignrag-typing').hide();
            }
        }).fail(function(xhr, status, error) {
            console.error('Error initializing agent:', error);
            // Fall back to traditional search mode
            isAgentMode = false;
        });
    }

    /**
     * Send message to AG2 conversational agent
     */
    function sendAgentMessage(message) {
        const apiUrl = sovereignragChat.apiUrl + '/api/agent/chat/' + chatSessionId + '/message';

        // Properly handle falsy values: only true if explicitly true or 1
        const showSourcesValue = sovereignragChat.showSources === true || sovereignragChat.showSources === 1 || sovereignragChat.showSources === '1';

        apiRequest({
            url: apiUrl,
            method: 'POST',
            contentType: 'application/json',
            headers: getAuthHeaders(),
            data: JSON.stringify({
                message: message,
                use_general_knowledge: sovereignragChat.enableGeneralKnowledge === true || sovereignragChat.enableGeneralKnowledge === 1 || sovereignragChat.enableGeneralKnowledge === '1',
                show_gk_disclaimer: sovereignragChat.showGeneralKnowledgeDisclaimer === true || sovereignragChat.showGeneralKnowledgeDisclaimer === 1 || sovereignragChat.showGeneralKnowledgeDisclaimer === '1',
                gk_disclaimer_text: sovereignragChat.generalKnowledgeDisclaimerText || null,
                show_sources: showSourcesValue
            })
        }).done(function(response) {
            $('#sovereignrag-typing').hide();

            if (response.response) {
                // Add agent response (with Markdown rendering and confidence score)
                addMessage(response.response, 'bot', false, true, response.confidence_score, response.show_confidence);

                // Check if AI detected user satisfaction
                if (response.suggests_close) {
                    showClosePrompt();
                }

                // Check if agent suggests escalation
                if (response.suggests_escalation) {
                    showEscalationPrompt();
                }

                // Check if user explicitly requested escalation
                if (response.escalation_requested) {
                    showEscalationConfirmation();
                }
            } else {
                addMessage('I apologize, but I encountered an error. Please try again.', 'bot');
            }
        }).fail(function(xhr, status, error) {
            $('#sovereignrag-typing').hide();

            // Check if session was not found (404 or error message contains "Session not found")
            const isSessionNotFound = xhr.status === 404 ||
                (xhr.responseText && xhr.responseText.includes('Session not found'));

            if (isSessionNotFound) {
                console.log('Session expired or not found, creating new session...');
                handleExpiredSession(message);
            } else {
                addMessage('Sorry, I couldn\'t process your message. Please try again later.', 'bot');
            }
        });
    }

    /**
     * Handle expired or invalid session by resetting to initial state
     */
    function handleExpiredSession(pendingMessage) {
        console.log('Session expired, resetting chat widget...');

        // Clear old session data
        clearSessionStorage();
        chatSessionId = null;
        agentInitialized = false;

        // Reset widget to initial greeting state
        resetChatWidget();

        // If there's a pending message, we could optionally auto-reinitialize
        // For now, just reset and let user start fresh
        if (pendingMessage) {
            console.log('Message pending but session expired - user needs to resend');
        }
    }

    /**
     * Show escalation prompt when agent suggests it
     */
    function showEscalationPrompt() {
        if (lastEscalationPrompt) {
            return; // Don't show multiple times
        }

        lastEscalationPrompt = true;

        const promptHtml = `
            <div class="graphiti-escalation-prompt">
                <p>Would you like to connect with a human team member for more personalized help?</p>
                <div class="graphiti-escalation-buttons">
                    <button class="graphiti-btn graphiti-btn-escalate-yes">
                        👤 Yes, connect me with a person
                    </button>
                    <button class="graphiti-btn graphiti-btn-escalate-no">
                        ✖ No, continue chatting
                    </button>
                </div>
            </div>
        `;

        addMessage(promptHtml, 'bot', true);

        // Bind escalation button handlers
        $('.graphiti-btn-escalate-yes').off('click').on('click', function() {
            $(this).closest('.graphiti-escalation-prompt').fadeOut();
            requestEscalation();
        });

        $('.graphiti-btn-escalate-no').off('click').on('click', function() {
            $(this).closest('.graphiti-escalation-prompt').fadeOut();
            addMessage('No problem! I\'m here to help. What else would you like to know?', 'bot');
            lastEscalationPrompt = null; // Allow showing again if needed
        });
    }

    /**
     * Show confirmation that escalation was requested
     */
    function showEscalationConfirmation() {
        const confirmHtml = `
            <div class="graphiti-escalation-confirmation">
                <p><strong>✓ Your conversation has been escalated</strong></p>
                <p>A team member will review your conversation and reach out soon.</p>
                <p>Would you like to provide your email address for follow-up?</p>
                <div class="graphiti-email-input-wrapper">
                    <input type="email" class="graphiti-email-input" placeholder="your@email.com" />
                    <button class="graphiti-btn graphiti-btn-submit-email">Submit</button>
                    <button class="graphiti-btn graphiti-btn-skip-email">Skip</button>
                </div>
            </div>
        `;

        addMessage(confirmHtml, 'bot', true);

        // Bind email submission handlers
        $('.graphiti-btn-submit-email').off('click').on('click', function() {
            const email = $('.graphiti-email-input').val().trim();
            if (email && validateEmail(email)) {
                submitEscalationWithEmail(email);
                $(this).closest('.graphiti-escalation-confirmation').fadeOut();
            } else {
                alert('Please enter a valid email address');
            }
        });

        $('.graphiti-btn-skip-email').off('click').on('click', function() {
            $(this).closest('.graphiti-escalation-confirmation').fadeOut();
            addMessage('Thank you. You can continue chatting, and your conversation is saved for our team.', 'bot');
        });
    }

    /**
     * Request escalation to human support
     */
    function requestEscalation(userEmail) {
        const apiUrl = sovereignragChat.apiUrl + '/api/agent/chat/' + chatSessionId + '/escalate';

        apiRequest({
            url: apiUrl,
            method: 'POST',
            contentType: 'application/json',
            headers: getAuthHeaders(),
            data: JSON.stringify({
                reason: 'User requested human support',
                user_email: userEmail || null
            })
        }).done(function(response) {
            if (response.success) {
                showEscalationConfirmation();
            }
        }).fail(function() {
            addMessage('Sorry, there was an error processing your escalation request. Please try again.', 'bot');
        });
    }

    /**
     * Submit escalation with email address
     */
    function submitEscalationWithEmail(email) {
        const apiUrl = sovereignragChat.apiUrl + '/api/agent/chat/' + chatSessionId + '/escalate';

        apiRequest({
            url: apiUrl,
            method: 'POST',
            contentType: 'application/json',
            headers: getAuthHeaders(),
            data: JSON.stringify({
                reason: 'User requested human support',
                user_email: email
            })
        }).done(function() {
            addMessage('Thank you! We\'ll reach out to ' + email + ' soon.', 'bot');
        }).fail(function() {
            console.error('Failed to submit email for escalation');
        });
    }

    /**
     * Validate email format
     */
    function validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    /**
     * Show close session prompt when AI detects user satisfaction
     * Auto-closes after 60 seconds if no response
     */
    function showClosePrompt() {
        const promptHtml = `
            <div class="graphiti-close-prompt">
                <p>Is there anything else I can help you with?</p>
                <div class="graphiti-close-buttons">
                    <button class="graphiti-btn graphiti-btn-continue">
                        💬 Continue chatting
                    </button>
                    <button class="graphiti-btn graphiti-btn-end-chat">
                        ✓ End chat
                    </button>
                </div>
            </div>
        `;

        addMessage(promptHtml, 'bot', true);

        // Clear any existing timeout
        if (closePromptTimeout) {
            clearTimeout(closePromptTimeout);
            closePromptTimeout = null;
        }

        // Start 60-second timeout to auto-close
        closePromptTimeout = setTimeout(function() {
            console.log('Close prompt timeout - auto-closing session after 60 seconds');

            // Hide the prompt
            $('.graphiti-close-prompt').fadeOut(function() {
                $(this).remove();
            });

            // Show session ended message
            addMessage('This chat session has ended due to inactivity. Feel free to start a new conversation anytime!', 'bot');

            // Close the session
            closeSession();
        }, CLOSE_PROMPT_DELAY);

        // Bind close button handlers
        $('.graphiti-btn-continue').off('click').on('click', function() {
            // Clear the timeout
            if (closePromptTimeout) {
                clearTimeout(closePromptTimeout);
                closePromptTimeout = null;
            }

            $(this).closest('.graphiti-close-prompt').fadeOut();
            addMessage('Sure! What else would you like to know?', 'bot');
        });

        $('.graphiti-btn-end-chat').off('click').on('click', function() {
            // Clear the timeout
            if (closePromptTimeout) {
                clearTimeout(closePromptTimeout);
                closePromptTimeout = null;
            }

            $(this).closest('.graphiti-close-prompt').fadeOut();
            closeSession();
        });
    }

    /**
     * Close the current chat session
     */
    function closeSession() {
        if (!chatSessionId) {
            return;
        }

        const apiUrl = sovereignragChat.apiUrl + '/api/agent/chat/' + chatSessionId + '/close';

        apiRequest({
            url: apiUrl,
            method: 'POST',
            headers: getAuthHeaders()
        }).done(function() {
            // Clear session
            chatSessionId = null;
            agentInitialized = false;

            // Clear localStorage
            clearSessionStorage();

            // Reset chat widget to initial state
            resetChatWidget();

            // Close the chat window
            $('#sovereignrag-chat-window').hide();
        }).fail(function() {
            console.error('Failed to close session');
            // Reset anyway
            chatSessionId = null;
            agentInitialized = false;
            clearSessionStorage();
            resetChatWidget();

            // Close the chat window
            $('#sovereignrag-chat-window').hide();
        });
    }

    /**
     * Reset chat widget to initial greeting state
     */
    function resetChatWidget() {
        // Clear all messages
        $('#sovereignrag-chat-messages').empty();

        // Add initial greeting
        const greetingHtml = `
            <div class="graphiti-chat-welcome">
                <p>${sovereignragChat.greetingMessage}</p>
                <p class="graphiti-chat-hint">${sovereignragChat.greetingHint}</p>
            </div>
        `;
        $('#sovereignrag-chat-messages').html(greetingHtml);

        // Re-enable and clear input
        $('#sovereignrag-chat-input').prop('disabled', false).val('');
        $('#sovereignrag-chat-send').prop('disabled', false);

        // Hide any prompts
        $('#sovereignrag-satisfaction-prompt').hide();
        $('#sovereignrag-autocomplete').hide();
        $('#sovereignrag-typing').hide();

        console.log('Chat widget reset to initial state');
    }

    function performSearch(query) {
        // Choose endpoint based on RAG settings
        const useRag = sovereignragChat.enableRag || false;
        const endpoint = useRag ? '/api/ask' : '/api/search';
        const apiUrl = sovereignragChat.apiUrl + endpoint;

        // Properly handle falsy values: only true if explicitly true or 1
        const showSourcesValue = sovereignragChat.showSources === true || sovereignragChat.showSources === 1 || sovereignragChat.showSources === '1';

        // Prepare request data based on endpoint
        const requestData = useRag ? {
            query: query,
            num_results: 5,
            use_general_knowledge: sovereignragChat.enableGeneralKnowledge,
            persona: sovereignragChat.ragPersona,  // AI personality setting
            language: sovereignragChat.language || null,  // Site language
            return_mode: sovereignragChat.returnMode,  // Single vs multiple results
            min_confidence: sovereignragChat.minConfidence || 0.5,  // Use configured minimum confidence
            show_sources: showSourcesValue
        } : {
            query: query,
            num_results: 5,
            min_confidence: sovereignragChat.minConfidence || 0.5,  // Use configured minimum confidence
            low_confidence_threshold: sovereignragChat.lowConfidenceThreshold,  // For auto-flagging
            return_mode: sovereignragChat.returnMode  // Single vs multiple results
        };

        apiRequest({
            url: apiUrl,
            method: 'POST',
            contentType: 'application/json',
            headers: getAuthHeaders(),
            data: JSON.stringify(requestData)
        }).done(function(response) {
            $('#sovereignrag-typing').hide();

            // Backend returns data directly (no 'success' wrapper)
            if (response.response || response.results) {
                // Display results (handles both /api/search and /api/ask responses)
                displayResults(response, useRag);

                // Show satisfaction prompt if present (only for /api/search)
                if (!useRag && response.satisfaction_prompt) {
                    showSatisfactionPrompt(response.satisfaction_prompt, response);
                }
            } else {
                addMessage('Sorry, I encountered an error processing your request.', 'bot');
            }
        }).fail(function() {
            $('#sovereignrag-typing').hide();
            addMessage('Sorry, I couldn\'t connect to the knowledge graph. Please try again later.', 'bot');
        });
    }

    function displayResults(response, useRag) {
        if (useRag) {
            // RAG response: display AI-generated answer with Markdown
            if (response.response || response.answer) {
                // Use 'response' field (from /api/ask) or 'answer' field (fallback)
                const aiResponse = response.response || response.answer;

                // Display the AI answer with Markdown rendering
                addMessage(aiResponse, 'bot', false, true);
            } else {
                addMessage('I couldn\'t find any information about that. Could you try rephrasing your question?', 'bot');
            }
        } else {
            // Search response: display facts list (original behavior)
            if (!response.results || response.results.length === 0) {
                addMessage('I couldn\'t find any information about that. Could you try rephrasing your question?', 'bot');
                return;
            }

            let resultsHtml = '<div class="graphiti-search-results">';

            response.results.forEach(function(result) {
                const confidenceClass = getConfidenceClass(result.confidence);
                const confidenceLabel = getConfidenceLabel(result.confidence);

                resultsHtml += '<div class="graphiti-result-item">';
                resultsHtml += '<div class="graphiti-result-fact">' + escapeHtml(result.fact) + '</div>';
                resultsHtml += '<div class="graphiti-result-meta">';

                if (result.source) {
                    resultsHtml += '<span>' + escapeHtml(result.source) + '</span>';
                }

                if (result.confidence !== null && result.confidence !== undefined) {
                    resultsHtml += '<span class="graphiti-confidence ' + confidenceClass + '">' + confidenceLabel + '</span>';
                }

                resultsHtml += '</div>';
                resultsHtml += '</div>';
            });

            resultsHtml += '</div>';

            addMessage(resultsHtml, 'bot', true);
        }
    }

    function showSatisfactionPrompt(message, response) {
        currentSatisfactionPrompt = {
            query: response.query,
            auto_flagged: response.auto_flagged,
            flag_reason: response.flag_reason
        };

        $('#sovereignrag-satisfaction-message').text(message);
        $('#sovereignrag-satisfaction-prompt').slideDown();
    }

    function submitSatisfactionFeedback(isHelpful) {
        if (!currentSatisfactionPrompt) {
            return;
        }

        const apiUrl = sovereignragChat.apiUrl + '/api/feedback';

        apiRequest({
            url: apiUrl,
            method: 'POST',
            contentType: 'application/json',
            headers: getAuthHeaders(),
            data: JSON.stringify({
                query: currentSatisfactionPrompt.query,
                is_accurate: isHelpful === 'yes',
                feedback: isHelpful === 'yes' ? 'Marked as helpful' : 'Marked as not helpful',
                user_email: null
            })
        }).done(function() {
            // Show thank you message
            $('#sovereignrag-satisfaction-prompt').slideUp(function() {
                addMessage('Thank you for your feedback! It helps us improve.', 'bot');
            });
            currentSatisfactionPrompt = null;
        }).fail(function() {
            console.error('Failed to submit feedback');
        });
    }

    function addMessage(content, type, isHtml, isMarkdown, confidenceScore, showConfidence) {
        const $messages = $('#sovereignrag-chat-messages');
        const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        // Remove welcome message if present
        $('.graphiti-chat-welcome').remove();

        let $message = $('<div class="graphiti-message graphiti-message-' + type + '"></div>');
        let $bubble = $('<div class="graphiti-message-bubble"></div>');

        // Configure marked.js for safe rendering
        if (typeof marked !== 'undefined' && !marked.configured) {
            marked.setOptions({
                breaks: true,
                gfm: true,
                sanitize: false,
                headerIds: false,
                mangle: false
            });
            marked.configured = true;
        }

        if (isMarkdown && typeof marked !== 'undefined') {
            // Render Markdown to HTML
            $bubble.html(marked.parse(content));
        } else if (isHtml) {
            $bubble.html(content);
        } else {
            $bubble.text(content);
        }

        // Add confidence score badge for bot messages (only if showConfidence is true)
        if (type === 'bot' && confidenceScore !== null && confidenceScore !== undefined && showConfidence !== false) {
            let confidenceClass = '';
            if (confidenceScore >= 70) {
                confidenceClass = 'graphiti-confidence-high';
            } else if (confidenceScore >= 40) {
                confidenceClass = 'graphiti-confidence-medium';
            } else {
                confidenceClass = 'graphiti-confidence-low';
            }

            const $badge = $(
                '<div class="graphiti-confidence-badge-wrapper">' +
                    '<span class="graphiti-confidence-badge ' + confidenceClass + '">' +
                        '<svg width="10" height="10" viewBox="0 0 10 10" fill="none" xmlns="http://www.w3.org/2000/svg">' +
                            '<circle cx="5" cy="5" r="4" fill="currentColor" opacity="0.3"/>' +
                            '<circle cx="5" cy="5" r="2.5" fill="currentColor"/>' +
                        '</svg>' +
                        '<span class="graphiti-confidence-text">' + confidenceScore + '% confident</span>' +
                    '</span>' +
                '</div>'
            );
            $bubble.append($badge);
        }

        $message.append($bubble);
        $message.append('<div class="graphiti-message-time">' + time + '</div>');

        $messages.append($message);

        // Scroll to bottom
        $messages.scrollTop($messages[0].scrollHeight);

        // Save messages to localStorage for persistence
        saveMessages();

        // Update activity timestamp
        saveSessionTimestamp();
    }

    function getConfidenceClass(confidence) {
        if (confidence === null || confidence === undefined) {
            return '';
        }

        if (confidence >= 0.7) {
            return 'graphiti-confidence-high';
        } else if (confidence >= 0.4) {
            return 'graphiti-confidence-medium';
        } else {
            return 'graphiti-confidence-low';
        }
    }

    function getConfidenceLabel(confidence) {
        if (confidence === null || confidence === undefined) {
            return '';
        }

        return Math.round(confidence * 100) + '%';
    }

    function escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, function(m) { return map[m]; });
    }

})(jQuery);
