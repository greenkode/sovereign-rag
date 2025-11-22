/**
 * Frontend JavaScript for Compilot AI Assistant Plugin
 */

(function($) {
    'use strict';

    // Search Form
    $('.graphiti-search-form').on('submit', function(e) {
        e.preventDefault();

        var $form = $(this);
        var $button = $form.find('.graphiti-search-button');
        var $spinner = $form.find('.graphiti-search-spinner');
        var $results = $form.closest('.compilot-ai-search-widget').find('.graphiti-search-results');
        var query = $form.find('[name="query"]').val();

        $button.prop('disabled', true);
        $spinner.show();
        $results.hide();

        $.ajax({
            url: compilotAI.ajax_url,
            method: 'POST',
            data: {
                action: 'compilot_search',
                nonce: compilotAI.nonce,
                query: query,
                num_results: 10,
                min_confidence: 0.5
            },
            success: function(response) {
                if (response.success && response.data) {
                    displayFrontendSearchResults(response.data, query, $results);
                } else {
                    $results.find('.results-container').html(
                        '<p>No results found or an error occurred.</p>'
                    );
                    $results.show();
                }
            },
            error: function() {
                $results.find('.results-container').html(
                    '<p class="error">Network error. Please try again.</p>'
                );
                $results.show();
            },
            complete: function() {
                $button.prop('disabled', false);
                $spinner.hide();
            }
        });
    });

    // Display search results
    function displayFrontendSearchResults(data, query, $container) {
        var $resultsContainer = $container.find('.results-container');

        if (!data.results || data.results.length === 0) {
            $resultsContainer.html('<p>No results found for "' + escapeHtml(query) + '"</p>');
            $container.show();
            return;
        }

        var html = '';
        data.results.forEach(function(result) {
            html += renderFrontendResult(result, query);
        });

        $resultsContainer.html(html);
        $container.show();
    }

    // Render single result
    function renderFrontendResult(result, query) {
        var confidence = result.confidence || 0;
        var confidenceClass = confidence >= 0.7 ? 'confidence-high' :
                             confidence >= 0.4 ? 'confidence-medium' :
                             'confidence-low';

        var html = '<div class="result-item" data-uuid="' + result.uuid + '" data-query="' + escapeHtml(query) + '">';
        html += '<div class="result-fact">' + escapeHtml(result.fact) + '</div>';

        html += '<div class="result-meta">';
        if (result.confidence) {
            html += '<span class="result-confidence ' + confidenceClass + '">';
            html += 'Confidence: ' + (confidence * 100).toFixed(0) + '%';
            html += '</span>';
        }

        if (result.valid_at) {
            html += '<span class="result-date">';
            html += 'Valid from: ' + formatDate(result.valid_at);
            if (result.invalid_at) {
                html += ' to ' + formatDate(result.invalid_at);
            }
            html += '</span>';
        }
        html += '</div>';

        // Feedback section
        html += '<div class="result-actions">';
        html += '<div class="feedback-question">Was this result helpful?</div>';
        html += '<div class="feedback-btns">';
        html += '<button class="feedback-btn" data-accurate="true">Yes, accurate</button>';
        html += '<button class="feedback-btn" data-accurate="false">No, inaccurate</button>';
        html += '</div>';
        html += '<div class="feedback-form" style="display:none;"></div>';
        html += '</div>';

        html += '</div>';
        return html;
    }

    // Feedback button click
    $(document).on('click', '.feedback-btn', function() {
        var $btn = $(this);
        var $item = $btn.closest('.result-item');
        var isAccurate = $btn.data('accurate') === true;
        var uuid = $item.data('uuid');
        var query = $item.data('query');

        // Mark button as active
        $item.find('.feedback-btn').removeClass('accurate inaccurate');
        $btn.addClass(isAccurate ? 'accurate' : 'inaccurate');

        if (!isAccurate) {
            // Show feedback form for inaccurate results
            showFeedbackForm($item, uuid, query);
        } else {
            // Submit positive feedback
            submitFeedback(uuid, query, true, '', '');
            $item.find('.feedback-form').hide();
        }
    });

    // Show feedback form
    function showFeedbackForm($item, uuid, query) {
        var html = '<div class="form-group">';
        html += '<label>Please tell us what was wrong (optional):</label>';
        html += '<textarea class="feedback-textarea" rows="3" placeholder="What was inaccurate about this result?"></textarea>';
        html += '</div>';
        html += '<div class="form-group">';
        html += '<label>Your email (optional, for follow-up):</label>';
        html += '<input type="email" class="feedback-email" placeholder="your@email.com">';
        html += '</div>';
        html += '<button class="submit-feedback-btn" data-uuid="' + uuid + '" data-query="' + escapeHtml(query) + '">Submit Feedback</button>';

        $item.find('.feedback-form').html(html).slideDown();
    }

    // Submit feedback button
    $(document).on('click', '.submit-feedback-btn', function() {
        var $btn = $(this);
        var $item = $btn.closest('.result-item');
        var uuid = $btn.data('uuid');
        var query = $btn.data('query');
        var feedback = $item.find('.feedback-textarea').val();
        var email = $item.find('.feedback-email').val();

        submitFeedback(uuid, query, false, feedback, email);
        $item.find('.feedback-form').slideUp();
        $item.find('.feedback-btns').append('<p class="feedback-thanks">Thank you for your feedback!</p>');
    });

    // Submit feedback to API
    function submitFeedback(uuid, query, isAccurate, feedback, email) {
        $.ajax({
            url: compilotAI.ajax_url,
            method: 'POST',
            data: {
                action: 'compilot_submit_feedback',
                nonce: compilotAI.nonce,
                result_uuid: uuid,
                query: query,
                is_accurate: isAccurate,
                feedback: feedback,
                user_email: email
            },
            success: function(response) {
                if (response.success) {
                    console.log('Feedback submitted successfully');
                }
            },
            error: function() {
                console.error('Failed to submit feedback');
            }
        });
    }

    // Submit Form
    $('.graphiti-submit-form').on('submit', function(e) {
        e.preventDefault();

        var $form = $(this);
        var $button = $form.find('.graphiti-submit-button');
        var $spinner = $form.find('.graphiti-submit-spinner');
        var $message = $form.find('.graphiti-submit-message');

        $button.prop('disabled', true);
        $spinner.show();
        $message.hide();

        $.ajax({
            url: compilotAI.ajax_url,
            method: 'POST',
            data: {
                action: 'compilot_add_episode',
                nonce: compilotAI.nonce,
                content: $form.find('[name="content"]').val(),
                description: $form.find('[name="description"]').val(),
                episode_type: 'text'
            },
            success: function(response) {
                if (response.success) {
                    $message
                        .removeClass('error')
                        .addClass('success')
                        .html('Thank you! Your information has been added to the knowledge graph.')
                        .show();
                    $form[0].reset();
                } else {
                    $message
                        .removeClass('success')
                        .addClass('error')
                        .html('Failed to submit: ' + (response.data.message || 'Unknown error'))
                        .show();
                }
            },
            error: function() {
                $message
                    .removeClass('success')
                    .addClass('error')
                    .html('Network error. Please try again.')
                    .show();
            },
            complete: function() {
                $button.prop('disabled', false);
                $spinner.hide();
            }
        });
    });

    // Helper functions
    function escapeHtml(text) {
        var map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, function(m) { return map[m]; });
    }

    function formatDate(dateString) {
        var date = new Date(dateString);
        return date.toLocaleDateString();
    }

})(jQuery);
