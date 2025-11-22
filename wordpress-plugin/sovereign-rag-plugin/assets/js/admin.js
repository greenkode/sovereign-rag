/**
 * Admin JavaScript for Sovereign RAG Assistant Plugin
 */

(function($) {
    'use strict';

    console.log('Sovereign RAG Admin script loaded');
    console.log('sovereignRAG object:', typeof sovereignRAG !== 'undefined' ? sovereignRAG : 'NOT DEFINED');

    // JWT Token Manager
    var jwtToken = {
        token: null,
        expiresAt: null,

        /**
         * Get JWT token (fetches new one if expired or missing)
         */
        get: function() {
            return new Promise(function(resolve, reject) {
                // Check if we have a valid token
                if (jwtToken.token && jwtToken.expiresAt && new Date().getTime() < jwtToken.expiresAt) {
                    resolve(jwtToken.token);
                    return;
                }

                // Fetch new token
                jwtToken.fetch().then(resolve).catch(reject);
            });
        },

        /**
         * Fetch new JWT token from server
         */
        fetch: function() {
            return new Promise(function(resolve, reject) {
                $.ajax({
                    url: sovereignRAG.ajax_url,
                    method: 'POST',
                    data: {
                        action: 'sovereignrag_get_jwt_token',
                        nonce: sovereignRAG.nonce
                    },
                    success: function(response) {
                        if (response.success && response.data.token) {
                            jwtToken.token = response.data.token;
                            // Set expiry to 90% of token lifetime to refresh before expiration
                            var expiresIn = response.data.expiresIn || 3600;
                            jwtToken.expiresAt = new Date().getTime() + (expiresIn * 900); // 90% of lifetime in ms
                            console.log('JWT token fetched successfully, expires at:', new Date(jwtToken.expiresAt));
                            resolve(jwtToken.token);
                        } else {
                            var error = 'Failed to get JWT token: ' + (response.data?.message || 'Unknown error');
                            console.error(error);
                            reject(new Error(error));
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('JWT token fetch error:', error);
                        reject(new Error('Failed to fetch JWT token: ' + error));
                    }
                });
            });
        },

        /**
         * Helper: Make authenticated API request with JWT token
         */
        apiRequest: function(url, options) {
            return jwtToken.get().then(function(token) {
                var headers = options.headers || {};
                headers['Authorization'] = 'Bearer ' + token;
                headers['Content-Type'] = 'application/json';

                return $.ajax(Object.assign({}, options, {
                    url: url,
                    headers: headers
                }));
            });
        }
    };

    // Admin Search Form
    $('#admin-search-form').on('submit', function(e) {
        e.preventDefault();

        var $form = $(this);
        var $button = $form.find('button[type="submit"]');
        var $spinner = $form.find('.spinner');
        var $results = $('#search-results');

        $button.prop('disabled', true);
        $spinner.addClass('is-active');
        $results.html('<p>Searching...</p>');

        $.ajax({
            url: sovereignRAG.ajax_url,
            method: 'POST',
            data: {
                action: 'sovereignrag_search',
                nonce: sovereignRAG.nonce,
                query: $('#search-query').val(),
                num_results: $('#num-results').val(),
                min_confidence: $('#min-confidence').val()
            },
            success: function(response) {
                if (response.success && response.data) {
                    displaySearchResults(response.data, $results);
                } else {
                    $results.html('<p class="error">Search failed: ' + (response.data.message || 'Unknown error') + '</p>');
                }
            },
            error: function() {
                $results.html('<p class="error">Network error. Please try again.</p>');
            },
            complete: function() {
                $button.prop('disabled', false);
                $spinner.removeClass('is-active');
            }
        });
    });

    // Display AI response
    function displaySearchResults(data, $container) {
        if (!data.response) {
            $container.html('<p>No response received.</p>');
            return;
        }

        // Configure marked.js for safe rendering
        if (typeof marked !== 'undefined') {
            marked.setOptions({
                breaks: true,
                gfm: true,
                sanitize: false,
                headerIds: false,
                mangle: false
            });
        }

        var html = '<div class="ai-response">';
        html += '<h3>AI Answer</h3>';

        // Render markdown to HTML
        var renderedResponse = typeof marked !== 'undefined'
            ? marked.parse(data.response)
            : data.response.replace(/\n/g, '<br>');

        html += '<div class="response-text">' + renderedResponse + '</div>';

        // Note: Sources are already included inline in the AI response
        // No need to display them separately

        html += '</div>';

        $container.html(html);
    }

    // Test API Connection
    $('#test-connection').on('click', function() {
        var $button = $(this);
        var $result = $('#connection-test-result');

        $button.prop('disabled', true);
        $result.html('<p>Testing connection...</p>');

        $.ajax({
            url: sovereignRAG.ajax_url,
            method: 'POST',
            data: {
                action: 'sovereignrag_test_connection',
                nonce: sovereignRAG.nonce
            },
            success: function(response) {
                if (response.success) {
                    var html = '<div class="notice notice-success"><p>';
                    html += '<strong>✓ Connection Successful</strong><br>';
                    html += '<strong>Status:</strong> ' + response.data.status + '<br>';
                    html += '<strong>Message:</strong> ' + response.data.message;
                    html += '</p></div>';
                    $result.html(html);
                } else {
                    $result.html('<div class="notice notice-error"><p><strong>✗ Connection Failed</strong><br>' + (response.data.message || 'Unknown error') + '</p></div>');
                }
            },
            error: function(xhr, status, error) {
                $result.html('<div class="notice notice-error"><p><strong>✗ Connection Failed</strong><br>Error: ' + error + '</p></div>');
            },
            complete: function() {
                $button.prop('disabled', false);
            }
        });
    });

    // Regenerate API Key - Two-Step Secure Flow
    $('#regenerate-api-key').on('click', function() {
        var $button = $(this);
        var $result = $('#api-key-result');
        var tenantId = sovereignRAG.tenantId || $('#tenant_id').val();

        if (!tenantId) {
            $result.html('<div class="notice notice-error"><p>Please enter a Tenant ID first.</p></div>');
            return;
        }

        // Show confirmation modal instead of native confirm dialog
        showConfirmModal(
            'Regenerate API Key',
            'A 6-digit verification code will be sent to your admin email. Do you want to proceed?',
            function() {
                // User confirmed - proceed with reset request
                proceedWithApiKeyReset($button, $result, tenantId);
            }
        );
    });

    // Helper function to show confirmation modal
    function showConfirmModal(title, message, onConfirm) {
        // Create modal HTML
        var modalHtml = '<div id="sovereignrag-confirm-modal" style="display: none; position: fixed; z-index: 100000; left: 0; top: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5);">' +
            '<div style="background-color: #fff; margin: 10% auto; padding: 0; border: 1px solid #ccc; border-radius: 4px; width: 90%; max-width: 500px; box-shadow: 0 5px 15px rgba(0,0,0,0.3);">' +
            '<div style="padding: 15px 20px; border-bottom: 1px solid #ddd; background-color: #f7f7f7;">' +
            '<h2 style="margin: 0; font-size: 18px; font-weight: 600;">' + title + '</h2>' +
            '</div>' +
            '<div style="padding: 20px;">' +
            '<p style="margin: 0 0 20px 0; font-size: 14px; line-height: 1.6;">' + message + '</p>' +
            '</div>' +
            '<div style="padding: 15px 20px; border-top: 1px solid #ddd; text-align: right; background-color: #f7f7f7;">' +
            '<button type="button" id="sovereignrag-modal-cancel" class="button" style="margin-right: 10px;">Cancel</button>' +
            '<button type="button" id="sovereignrag-modal-confirm" class="button button-primary">Proceed</button>' +
            '</div>' +
            '</div>' +
            '</div>';

        // Remove existing modal if any
        $('#sovereignrag-confirm-modal').remove();

        // Add modal to body
        $('body').append(modalHtml);

        // Show modal
        $('#sovereignrag-confirm-modal').fadeIn(200);

        // Handle confirm button
        $('#sovereignrag-modal-confirm').on('click', function() {
            $('#sovereignrag-confirm-modal').fadeOut(200, function() {
                $(this).remove();
            });
            if (onConfirm) onConfirm();
        });

        // Handle cancel button
        $('#sovereignrag-modal-cancel').on('click', function() {
            $('#sovereignrag-confirm-modal').fadeOut(200, function() {
                $(this).remove();
            });
        });

        // Handle click outside modal
        $('#sovereignrag-confirm-modal').on('click', function(e) {
            if (e.target.id === 'sovereignrag-confirm-modal') {
                $(this).fadeOut(200, function() {
                    $(this).remove();
                });
            }
        });

        // Handle ESC key
        $(document).on('keydown.sovereignragModal', function(e) {
            if (e.keyCode === 27) { // ESC key
                $('#sovereignrag-confirm-modal').fadeOut(200, function() {
                    $(this).remove();
                });
                $(document).off('keydown.sovereignragModal');
            }
        });
    }

    // Extracted function to proceed with API key reset
    function proceedWithApiKeyReset($button, $result, tenantId) {
        $button.prop('disabled', true).text('Sending code...');
        $result.html('<p>Requesting verification code...</p>');

        // Step 1: Request reset - No authentication required (public endpoint secured by email)
        $.ajax({
            url: sovereignRAG.api_url + '/api/admin/tenants/' + encodeURIComponent(tenantId) + '/request-reset',
            method: 'POST',
            contentType: 'application/json',
            dataType: 'json'
        }).done(function(response) {
            if (response.success) {
                // Show success message and input for verification code
                var html = '<div class="notice notice-success"><p>';
                html += '<strong>Verification code sent!</strong><br>';
                html += response.message + '<br><br>';
                html += '<label for="reset_token" style="font-weight: bold;">Enter 6-digit verification code:</label><br>';
                html += '<input type="text" id="reset_token" maxlength="6" pattern="[0-9]{6}" placeholder="000000" style="width: 150px; font-size: 18px; letter-spacing: 5px; text-align: center; margin: 10px 0;" /><br>';
                html += '<button type="button" id="confirm_reset" class="button button-primary" style="margin-top: 10px;">Confirm Reset</button> ';
                html += '<button type="button" id="cancel_reset" class="button">Cancel</button>';
                html += '</p></div>';
                $result.html(html);

                // Focus on input field
                setTimeout(function() {
                    $('#reset_token').focus();
                }, 100);

                // Handle confirm button
                $('#confirm_reset').on('click', function() {
                    var token = $('#reset_token').val().trim();

                    if (!token || token.length !== 6) {
                        alert('Please enter a valid 6-digit verification code.');
                        return;
                    }

                    $('#confirm_reset').prop('disabled', true).text('Verifying...');

                    // Step 2: Confirm reset with token - No authentication required
                    $.ajax({
                        url: sovereignRAG.api_url + '/api/admin/tenants/' + encodeURIComponent(tenantId) + '/confirm-reset',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({ token: token }),
                        dataType: 'json'
                    }).done(function(confirmResponse) {
                        if (confirmResponse.success && confirmResponse.newApiKey) {
                            var successHtml = '<div class="notice notice-success"><p>';
                            successHtml += '<strong>Success!</strong> Your new API key has been generated and saved.';
                            successHtml += '</p></div>';
                            $result.html(successHtml);

                            // Update the API key field with the new key
                            $('#api_key').val(confirmResponse.newApiKey);

                            // Update the WordPress option via AJAX
                            $.post(ajaxurl, {
                                action: 'update_api_key',
                                api_key: confirmResponse.newApiKey,
                                _wpnonce: sovereignRAG.nonce
                            });
                        } else {
                            $result.html('<div class="notice notice-error"><p>Failed to confirm reset: ' + (confirmResponse.message || 'Unknown error') + '</p></div>');
                        }
                        $button.prop('disabled', false).text('Regenerate API Key');
                    }).fail(function(xhr) {
                        var message = 'Connection failed';
                        if (xhr.responseJSON && xhr.responseJSON.message) {
                            message = xhr.responseJSON.message;
                        } else if (xhr.statusText) {
                            message = xhr.statusText;
                        }
                        $result.html('<div class="notice notice-error"><p>Failed to confirm reset: ' + message + '</p></div>');
                        $button.prop('disabled', false).text('Regenerate API Key');
                    });
                });

                // Handle cancel button
                $('#cancel_reset').on('click', function() {
                    $result.html('<div class="notice notice-info"><p>Reset cancelled.</p></div>');
                    $button.prop('disabled', false).text('Regenerate API Key');
                });

                // Allow Enter key to submit
                $('#reset_token').on('keypress', function(e) {
                    if (e.which === 13) {
                        $('#confirm_reset').click();
                    }
                });

            } else {
                $result.html('<div class="notice notice-error"><p>Failed to request reset: ' + (response.message || 'Unknown error') + '</p></div>');
                $button.prop('disabled', false).text('Regenerate API Key');
            }
        }).fail(function(xhr) {
            var message = 'Connection failed';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                message = xhr.responseJSON.message;
            } else if (xhr.statusText) {
                message = xhr.statusText;
            }
            $result.html('<div class="notice notice-error"><p>Failed to request reset: ' + message + '</p></div>');
            $button.prop('disabled', false).text('Regenerate API Key');
        });
    }

    // Helper functions
    function escapeHtml(text) {
        if (!text) return '';
        var map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, function(m) { return map[m]; });
    }

    // Content Synchronization: Queue-based Sync
    var syncJobId = null;

    // Process next post in queue (recursive)
    function processNextPost() {
        var $progress = $('#sync-progress');
        var $progressBar = $('#sync-progress-bar');
        var $status = $('#sync-status');
        var $button = $('#sovereignrag-sync-all');
        var $result = $('#sync-result');

        console.log('Processing next post for job:', syncJobId);

        $.ajax({
            url: sovereignRAG.ajax_url,
            method: 'POST',
            data: {
                action: 'sovereignrag_process_next',
                job_id: syncJobId,
                nonce: sovereignRAG.nonce
            },
            success: function(response) {
                console.log('Process next response:', response);

                if (!response.success) {
                    $result.html(
                        '<div class="notice notice-error"><p>Error: ' +
                        escapeHtml(response.data.message || 'Unknown error') +
                        '</p></div>'
                    );
                    $button.prop('disabled', false);
                    $progress.hide();
                    return;
                }

                var data = response.data;

                if (data.status === 'completed') {
                    // All done!
                    $progressBar.val(100);
                    $status.text('Sync complete!');

                    // Build result message
                    var resultClass = data.failed > 0 ? 'notice-warning' : 'notice-success';
                    var resultHtml = '<div class="notice ' + resultClass + '"><p>' +
                        escapeHtml(data.message) +
                        '</p>';

                    // Add failed posts list if any
                    if (data.failed_posts && data.failed_posts.length > 0) {
                        resultHtml += '<p><strong>Failed posts:</strong></p><ul>';
                        data.failed_posts.forEach(function(post) {
                            resultHtml += '<li>' + escapeHtml(post.title) + ' (ID: ' + post.id + ')</li>';
                        });
                        resultHtml += '</ul>';
                    }

                    resultHtml += '</div>';
                    $result.html(resultHtml);
                    $button.prop('disabled', false);

                    console.log('Sync completed:', data.message);

                    // Hide progress after 5 seconds (or 10 if there were failures)
                    setTimeout(function() {
                        $progress.fadeOut();
                    }, data.failed > 0 ? 10000 : 5000);

                } else if (data.status === 'next') {
                    // Update progress and continue
                    var progress = data.total > 0 ? (data.processed / data.total * 100) : 0;
                    $progressBar.val(progress);

                    var statusText = 'Syncing: ' + data.processed + ' of ' + data.total;
                    if (data.current_post) {
                        statusText += ' (Current: ' + data.current_post + ')';
                    }
                    $status.text(statusText);

                    console.log('Progress:', data.processed + '/' + data.total);

                    // Process next post immediately (no delay)
                    processNextPost();
                }
            },
            error: function(xhr, status, error) {
                console.error('AJAX error processing next post:', status, error, xhr);
                $result.html(
                    '<div class="notice notice-error"><p>Network error: ' +
                    escapeHtml(error) +
                    '</p></div>'
                );
                $button.prop('disabled', false);
                $progress.hide();
            }
        });
    }

    // Content Synchronization: Sync All Button
    var $syncButton = $('#sovereignrag-sync-all');
    console.log('Sync All button element found:', $syncButton.length > 0 ? 'YES' : 'NO');

    $syncButton.on('click', function() {
        console.log('Sync All button clicked');
        var $button = $(this);
        var $progress = $('#sync-progress');
        var $progressBar = $('#sync-progress-bar');
        var $status = $('#sync-status');
        var $result = $('#sync-result');

        // Clear previous results
        $result.html('');
        $status.text('Creating sync queue...');
        $progressBar.val(0);
        $progress.show();
        $button.prop('disabled', true);

        console.log('Sending AJAX request to:', sovereignRAG.ajax_url);

        // Create sync queue
        $.ajax({
            url: sovereignRAG.ajax_url,
            method: 'POST',
            data: {
                action: 'sovereignrag_bulk_sync',
                nonce: sovereignRAG.nonce
            },
            success: function(response) {
                console.log('AJAX response received:', response);
                if (response.success) {
                    syncJobId = response.data.job_id;
                    $status.text('Starting sync of ' + response.data.total + ' posts...');
                    console.log('Sync queue created with job:', syncJobId, 'Total:', response.data.total);

                    // Start processing posts one by one
                    processNextPost();
                } else {
                    console.error('Sync failed to start:', response);
                    $result.html(
                        '<div class="notice notice-error"><p>Failed to start sync: ' +
                        escapeHtml(response.data.message || 'Unknown error') +
                        '</p></div>'
                    );
                    $button.prop('disabled', false);
                    $progress.hide();
                }
            },
            error: function(xhr, status, error) {
                console.error('AJAX error:', status, error, xhr);
                $result.html(
                    '<div class="notice notice-error"><p>Network error: ' +
                    escapeHtml(error) +
                    '</p></div>'
                );
                $button.prop('disabled', false);
                $progress.hide();
            }
        });
    });

    // Clear Sync Status Button
    $('#sovereignrag-clear-sync').on('click', function() {
        var $button = $(this);
        var $result = $('#sync-result');

        if (!confirm('Are you sure you want to clear the sync status? This will reset any ongoing sync.')) {
            return;
        }

        $button.prop('disabled', true);

        // Hide progress
        $('#sync-progress').hide();

        // Reset job ID
        syncJobId = null;

        // Clear sync status on server
        $.ajax({
            url: sovereignRAG.ajax_url,
            method: 'POST',
            data: {
                action: 'sovereignrag_clear_sync',
                nonce: sovereignRAG.nonce
            },
            success: function(response) {
                if (response.success) {
                    $result.html('<div class="notice notice-success"><p>Sync status cleared. You can now start a new sync.</p></div>');
                    $('#sovereignrag-sync-all').prop('disabled', false);
                } else {
                    $result.html('<div class="notice notice-error"><p>Failed to clear sync status: ' + escapeHtml(response.data.message || 'Unknown error') + '</p></div>');
                }
            },
            error: function() {
                $result.html('<div class="notice notice-error"><p>Network error while clearing sync status</p></div>');
            },
            complete: function() {
                $button.prop('disabled', false);
            }
        });
    });

})(jQuery);
