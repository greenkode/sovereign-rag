<?php
// Prevent direct access
if (!defined('ABSPATH')) {
    exit;
}

$api_url = get_option('sovereignrag_ai_api_url', 'http://localhost:8000');
?>

<div class="wrap sovereignrag-review-page">
    <h1><?php echo esc_html__('Review & Add Content', 'sovereign-rag'); ?></h1>

    <h2 class="nav-tab-wrapper">
        <a href="#unanswered-queries" class="nav-tab nav-tab-active"><?php echo esc_html__('Unanswered Queries', 'sovereign-rag'); ?></a>
        <a href="#ingest-content" class="nav-tab"><?php echo esc_html__('Add Content', 'sovereign-rag'); ?></a>
    </h2>

    <!-- Unanswered Queries Tab -->
    <div id="unanswered-queries-tab" class="tab-content" style="display:block;">
        <h2><?php echo esc_html__('Queries Needing Review', 'sovereign-rag'); ?></h2>
        <p class="description"><?php echo esc_html__('These are queries that couldn\'t be answered with high confidence from the knowledge base.', 'sovereign-rag'); ?></p>

        <div id="queries-loading" style="margin: 20px 0;">
            <p><?php echo esc_html__('Loading unanswered queries...', 'sovereign-rag'); ?></p>
        </div>

        <div id="queries-list" style="display:none;">
            <div class="tablenav top" style="margin-bottom: 10px;">
                <div class="alignleft actions bulkactions">
                    <button type="button" id="bulk-accept" class="button action" disabled>
                        <?php echo esc_html__('Bulk Accept', 'sovereign-rag'); ?>
                    </button>
                    <button type="button" id="bulk-ignore" class="button action" disabled>
                        <?php echo esc_html__('Bulk Ignore', 'sovereign-rag'); ?>
                    </button>
                </div>
            </div>
            <table class="wp-list-table widefat fixed striped">
                <thead>
                    <tr>
                        <td class="manage-column column-cb check-column" style="width: 3%;">
                            <input id="select-all-queries" type="checkbox">
                        </td>
                        <th style="width: 22%;"><?php echo esc_html__('Query', 'sovereign-rag'); ?></th>
                        <th style="width: 28%;"><?php echo esc_html__('AI Response', 'sovereign-rag'); ?></th>
                        <th style="width: 10%;"><?php echo esc_html__('Timestamp', 'sovereign-rag'); ?></th>
                        <th style="width: 8%;"><?php echo esc_html__('Confidence', 'sovereign-rag'); ?></th>
                        <th style="width: 10%;"><?php echo esc_html__('Used General Knowledge', 'sovereign-rag'); ?></th>
                        <th style="width: 19%;"><?php echo esc_html__('Actions', 'sovereign-rag'); ?></th>
                    </tr>
                </thead>
                <tbody id="queries-tbody">
                    <!-- Populated by JavaScript -->
                </tbody>
            </table>

            <!-- Pagination Controls -->
            <div class="tablenav bottom" id="queries-pagination" style="display:none; margin-top: 10px;">
                <div class="tablenav-pages">
                    <span class="displaying-num" id="pagination-info"></span>
                    <span class="pagination-links">
                        <button class="button first-page" id="first-page" disabled>&laquo;</button>
                        <button class="button prev-page" id="prev-page" disabled>&lsaquo;</button>
                        <span class="paging-input">
                            <span class="tablenav-paging-text">
                                <span id="current-page-display">1</span> of <span id="total-pages-display">1</span>
                            </span>
                        </span>
                        <button class="button next-page" id="next-page">&rsaquo;</button>
                        <button class="button last-page" id="last-page">&raquo;</button>
                    </span>
                </div>
            </div>
        </div>

        <div id="queries-empty" style="display:none; margin: 20px 0;">
            <p><?php echo esc_html__('No unanswered queries found. Great job!', 'sovereign-rag'); ?></p>
        </div>
    </div>

    <!-- Add Content Tab -->
    <div id="ingest-content-tab" class="tab-content" style="display:none;">
        <h2><?php echo esc_html__('Add New Content', 'sovereign-rag'); ?></h2>
        <p class="description"><?php echo esc_html__('Add information to the knowledge base manually or by uploading files.', 'sovereign-rag'); ?></p>

        <div style="border: 1px solid #ddd; border-radius: 4px; padding: 20px; margin-bottom: 30px; background: #fff;">
            <h3 style="margin-top: 0;"><?php echo esc_html__('Manual Text Entry', 'sovereign-rag'); ?></h3>
            <form id="ingest-manual-form" method="post">
                <table class="form-table">
                    <tr>
                        <th scope="row">
                            <label for="manual-title"><?php echo esc_html__('Title', 'sovereign-rag'); ?></label>
                        </th>
                        <td>
                            <input type="text" id="manual-title" name="title" class="regular-text" required>
                            <p class="description"><?php echo esc_html__('A descriptive title for this content', 'sovereign-rag'); ?></p>
                        </td>
                    </tr>
                    <tr>
                        <th scope="row">
                            <label for="manual-content"><?php echo esc_html__('Content', 'sovereign-rag'); ?></label>
                        </th>
                        <td>
                            <textarea id="manual-content" name="content" rows="10" class="large-text" style="width: 100%;" required></textarea>
                            <p class="description"><?php echo esc_html__('Enter the content to add to the knowledge base', 'sovereign-rag'); ?></p>
                        </td>
                    </tr>
                    <tr>
                        <th scope="row">
                            <label for="manual-url"><?php echo esc_html__('Source URL (optional)', 'sovereign-rag'); ?></label>
                        </th>
                        <td>
                            <input type="url" id="manual-url" name="url" class="regular-text" placeholder="<?php echo esc_attr__('https://example.com/source', 'sovereign-rag'); ?>">
                            <p class="description"><?php echo esc_html__('URL where this information comes from', 'sovereign-rag'); ?></p>
                        </td>
                    </tr>
                </table>

                <p class="submit">
                    <button type="submit" class="button button-primary"><?php echo esc_html__('Add to Knowledge Base', 'sovereign-rag'); ?></button>
                </p>
            </form>
            <div id="manual-result" style="margin-top: 20px;"></div>
        </div>

        <div style="border: 1px solid #ddd; border-radius: 4px; padding: 20px; background: #fff;">
            <h3 style="margin-top: 0;"><?php echo esc_html__('File Upload', 'sovereign-rag'); ?></h3>
            <form id="ingest-file-form" method="post" enctype="multipart/form-data">
                <table class="form-table">
                    <tr>
                        <th scope="row">
                            <label for="file-title"><?php echo esc_html__('Title (optional)', 'sovereign-rag'); ?></label>
                        </th>
                        <td>
                            <input type="text" id="file-title" name="title" class="regular-text">
                            <p class="description"><?php echo esc_html__('If not provided, filename will be used as title', 'sovereign-rag'); ?></p>
                        </td>
                    </tr>
                    <tr>
                        <th scope="row">
                            <label for="ingest-file"><?php echo esc_html__('Select File', 'sovereign-rag'); ?></label>
                        </th>
                        <td>
                            <input type="file" id="ingest-file" name="file" accept=".txt,.pdf,.doc,.docx" required>
                            <p class="description"><?php echo esc_html__('Supported formats: TXT, PDF, DOC, DOCX', 'sovereign-rag'); ?></p>
                        </td>
                    </tr>
                    <tr>
                        <th scope="row">
                            <label for="file-url"><?php echo esc_html__('Source URL (optional)', 'sovereign-rag'); ?></label>
                        </th>
                        <td>
                            <input type="url" id="file-url" name="url" class="regular-text" placeholder="<?php echo esc_attr__('https://example.com/source', 'sovereign-rag'); ?>">
                            <p class="description"><?php echo esc_html__('URL where this file/information comes from', 'sovereign-rag'); ?></p>
                        </td>
                    </tr>
                </table>

                <p class="submit">
                    <button type="submit" class="button button-primary"><?php echo esc_html__('Upload & Process', 'sovereign-rag'); ?></button>
                </p>
            </form>
            <div id="file-result" style="margin-top: 20px;"></div>
        </div>
    </div>
</div>

<!-- Confirmation Modal -->
<div id="confirm-modal" class="sovereignrag-modal" style="display:none;">
    <div class="sovereignrag-modal-overlay"></div>
    <div class="sovereignrag-modal-content">
        <div class="sovereignrag-modal-header">
            <h2 id="modal-title"><?php echo esc_html__('Confirm Action', 'sovereign-rag'); ?></h2>
            <button type="button" class="sovereignrag-modal-close">&times;</button>
        </div>
        <div class="sovereignrag-modal-body">
            <p id="modal-message"><?php echo esc_html__('Are you sure you want to perform this action?', 'sovereign-rag'); ?></p>
        </div>
        <div class="sovereignrag-modal-footer">
            <button type="button" class="button" id="modal-cancel"><?php echo esc_html__('Cancel', 'sovereign-rag'); ?></button>
            <button type="button" class="button button-primary" id="modal-confirm"><?php echo esc_html__('Confirm', 'sovereign-rag'); ?></button>
        </div>
    </div>
</div>

<style>
/* Modal Styles */
.sovereignrag-modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 100000;
}

.sovereignrag-modal-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
}

.sovereignrag-modal-content {
    position: relative;
    width: 500px;
    max-width: 90%;
    margin: 100px auto;
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
    z-index: 100001;
}

.sovereignrag-modal-header {
    padding: 15px 20px;
    border-bottom: 1px solid #ddd;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.sovereignrag-modal-header h2 {
    margin: 0;
    font-size: 20px;
}

.sovereignrag-modal-close {
    background: none;
    border: none;
    font-size: 28px;
    line-height: 1;
    color: #666;
    cursor: pointer;
    padding: 0;
    width: 30px;
    height: 30px;
}

.sovereignrag-modal-close:hover {
    color: #000;
}

.sovereignrag-modal-body {
    padding: 20px;
}

.sovereignrag-modal-footer {
    padding: 15px 20px;
    border-top: 1px solid #ddd;
    text-align: right;
    display: flex;
    gap: 10px;
    justify-content: flex-end;
}

<style>
.tab-content {
    margin-top: 20px;
}

.nav-tab-wrapper {
    margin-bottom: 0;
}

#queries-tbody tr.reviewed {
    opacity: 0.6;
}

.query-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.query-actions button {
    padding: 4px 8px;
    font-size: 12px;
}

.inline-editor {
    margin-top: 15px;
    padding: 15px;
    background: #f9f9f9;
    border: 1px solid #ddd;
    border-radius: 4px;
}

.inline-editor textarea {
    width: 100%;
    min-height: 150px;
    margin: 10px 0;
}

.inline-editor .button-group {
    display: flex;
    gap: 10px;
}

/* Align checkboxes properly */
.wp-list-table thead .check-column,
.wp-list-table tbody .check-column {
    padding-left: 11px;
    padding-right: 5px;
    text-align: left;
    vertical-align: middle;
}

.wp-list-table tbody .check-column {
    padding-top: 10px;
    padding-bottom: 10px;
}

.wp-list-table .check-column input[type="checkbox"] {
    margin: 0;
    vertical-align: middle;
}
</style>

<script>
jQuery(document).ready(function($) {
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

    // Tab switching
    $('.nav-tab').on('click', function(e) {
        e.preventDefault();
        $('.nav-tab').removeClass('nav-tab-active');
        $(this).addClass('nav-tab-active');

        $('.tab-content').hide();
        var target = $(this).attr('href') + '-tab';
        $(target).show();
    });

    // Modal functionality
    var modalCallback = null;

    function showModal(title, message, onConfirm) {
        $('#modal-title').text(title);
        $('#modal-message').text(message);
        modalCallback = onConfirm;
        $('#confirm-modal').fadeIn(200);
    }

    function hideModal() {
        $('#confirm-modal').fadeOut(200);
        modalCallback = null;
    }

    $('#modal-confirm').on('click', function() {
        if (modalCallback) {
            modalCallback();
        }
        hideModal();
    });

    $('#modal-cancel, .sovereignrag-modal-close, .sovereignrag-modal-overlay').on('click', function() {
        hideModal();
    });

    // Select all checkbox functionality
    $('#select-all-queries').on('change', function() {
        $('.query-checkbox').prop('checked', $(this).prop('checked'));
        updateBulkButtons();
    });

    // Individual checkbox change
    $(document).on('change', '.query-checkbox', function() {
        updateBulkButtons();
        // Update select-all checkbox state
        var totalCheckboxes = $('.query-checkbox').length;
        var checkedCheckboxes = $('.query-checkbox:checked').length;
        $('#select-all-queries').prop('checked', totalCheckboxes === checkedCheckboxes);
    });

    // Update bulk button states based on checkbox selection
    function updateBulkButtons() {
        var checkedCount = $('.query-checkbox:checked').length;
        $('#bulk-accept, #bulk-ignore').prop('disabled', checkedCount === 0);
    }

    // Bulk ignore handler
    $('#bulk-ignore').on('click', function() {
        var selectedIds = $('.query-checkbox:checked').map(function() {
            return $(this).val();
        }).get();

        if (selectedIds.length === 0) {
            return;
        }

        showModal(
            'Confirm Bulk Ignore',
            'Are you sure you want to ignore ' + selectedIds.length + ' selected ' + (selectedIds.length === 1 ? 'query' : 'queries') + '? They will be deleted permanently.',
            function() {
                var deletePromises = selectedIds.map(function(id) {
                    return jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/admin/unanswered-queries/' + id, {
                        method: 'DELETE',
                        xhrFields: {
                            withCredentials: false
                        },
                        crossDomain: true
                    });
                });

                Promise.all(deletePromises).then(function() {
                    selectedIds.forEach(function(id) {
                        $('input.query-checkbox[value="' + id + '"]').closest('tr').fadeOut(300, function() {
                            $(this).remove();
                            // Reload if table is now empty
                            if ($('#queries-tbody tr').length === 0) {
                                loadUnansweredQueries();
                            }
                        });
                    });
                    $('#select-all-queries').prop('checked', false);
                }).catch(function(error) {
                    showModal('Error', 'Failed to ignore some queries. Please try again.', function() {});
                });
            }
        );
    });

    // Bulk accept handler
    $('#bulk-accept').on('click', function() {
        var selectedIds = $('.query-checkbox:checked').map(function() {
            return $(this).val();
        }).get();

        if (selectedIds.length === 0) {
            return;
        }

        showModal(
            'Confirm Bulk Accept',
            'Are you sure you want to accept ' + selectedIds.length + ' selected ' + (selectedIds.length === 1 ? 'query' : 'queries') + '? The AI answers will be added to the knowledge base.',
            function() {
                var acceptPromises = selectedIds.map(function(id) {
                    return jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/admin/unanswered-queries/' + id + '/mark-resolved', {
                        method: 'POST',
                        xhrFields: {
                            withCredentials: false
                        },
                        crossDomain: true
                    });
                });

                Promise.all(acceptPromises).then(function() {
                    loadUnansweredQueries();
                    $('#select-all-queries').prop('checked', false);
                }).catch(function(error) {
                    showModal('Error', 'Failed to accept some queries. Please try again.', function() {});
                });
            }
        );
    });

    // Pagination state
    var currentPage = 0;
    var pageSize = 20;
    var totalPages = 0;

    // Load unanswered queries
    loadUnansweredQueries();

    function loadUnansweredQueries(page) {
        if (page !== undefined) {
            currentPage = page;
        }

        $('#queries-loading').show();
        $('#queries-list').hide();
        $('#queries-empty').hide();

        jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/admin/unanswered-queries?page=' + currentPage + '&size=' + pageSize + '&status=open', {
            method: 'GET',
            xhrFields: {
                withCredentials: false  // Don't send cookies for cross-origin
            },
            crossDomain: true
        }).then(function(response) {
            $('#queries-loading').hide();

            // Spring Page response has: content, totalElements, totalPages, number, size
            var queries = response.content || [];
            totalPages = response.totalPages || 0;
            currentPage = response.number || 0;

            if (queries.length === 0) {
                $('#queries-empty').show();
            } else {
                displayQueries(queries);
                displayPagination(response);
                $('#queries-list').show();
            }
        }).catch(function(xhr) {
            var error = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : xhr.statusText || 'Unknown error';
            $('#queries-loading').html('<p class="error">Failed to load queries: ' + error + '</p>');
        });
    }

    function displayQueries(queries) {
        var html = '';
        queries.forEach(function(query) {
            var timestamp = new Date(query.lastOccurredAt).toLocaleString();
            var confidence = query.confidenceScore ? (query.confidenceScore * 100).toFixed(0) + '%' : 'N/A';
            var usedGK = query.usedGeneralKnowledge ? 'Yes' : 'No';
            var reviewedClass = (query.status === 'resolved') ? 'reviewed' : '';

            html += '<tr class="' + reviewedClass + '" data-query-id="' + query.id + '">';
            // Checkbox column
            html += '<td class="check-column">';
            if (query.status !== 'resolved') {
                html += '<input type="checkbox" class="query-checkbox" value="' + query.id + '">';
            }
            html += '</td>';
            html += '<td><strong>' + escapeHtml(query.query) + '</strong>';
            if (query.resolutionNotes) {
                html += '<br><small style="color:#666;">' + escapeHtml(query.resolutionNotes) + '</small>';
            }
            html += '</td>';
            html += '<td>' + (query.response ? escapeHtml(query.response) : '<em>No response</em>') + '</td>';
            html += '<td>' + timestamp + '</td>';
            html += '<td>' + confidence + '</td>';
            html += '<td>' + usedGK + '</td>';
            html += '<td class="query-actions">';
            if (query.status !== 'resolved') {
                html += '<button class="button button-small button-primary mark-reviewed" data-id="' + query.id + '">Accept Answer</button>';
                html += '<button class="button button-small add-info" data-id="' + query.id + '" data-query="' + escapeHtml(query.query) + '" data-response="' + escapeHtml(query.response || '') + '">Add Info</button>';
                html += '<button class="button button-small button-link-delete ignore-query" data-id="' + query.id + '" style="color:#b32d2e;">Ignore</button>';
            } else {
                html += '<span style="color:#46b450;">✓ Reviewed</span>';
            }
            html += '</td>';
            html += '</tr>';

            // Add inline editor row (hidden by default)
            if (query.status !== 'resolved') {
                html += '<tr class="inline-editor-row" id="editor-' + query.id + '" style="display:none;">';
                html += '<td colspan="7">';
                html += '<div class="inline-editor">';
                html += '<h3>Add Information to Knowledge Base</h3>';
                html += '<p><strong>Original Query:</strong> ' + escapeHtml(query.query) + '</p>';
                html += '<label for="editor-title-' + query.id + '"><strong>Title:</strong></label>';
                html += '<input type="text" id="editor-title-' + query.id + '" class="regular-text" value="' + escapeHtml(query.query) + '" style="width:100%; margin-bottom:10px;">';
                html += '<label for="editor-content-' + query.id + '"><strong>Content:</strong></label>';
                html += '<textarea id="editor-content-' + query.id + '" class="large-text">' + escapeHtml(query.response || '') + '</textarea>';
                html += '<label for="editor-url-' + query.id + '"><strong>Source URL (optional):</strong></label>';
                html += '<input type="url" id="editor-url-' + query.id + '" class="regular-text" placeholder="https://example.com/source" style="width:100%; margin-bottom:10px;">';
                html += '<div class="button-group">';
                html += '<button class="button button-primary save-info" data-id="' + query.id + '">Save to Knowledge Base</button>';
                html += '<button class="button cancel-editor" data-id="' + query.id + '">Cancel</button>';
                html += '</div>';
                html += '<div id="editor-result-' + query.id + '" style="margin-top:10px;"></div>';
                html += '</div>';
                html += '</td>';
                html += '</tr>';
            }
        });

        $('#queries-tbody').html(html);
    }

    // Mark as reviewed (accept AI answer and add to knowledge base)
    $(document).on('click', '.mark-reviewed', function() {
        var button = $(this);
        var id = button.data('id');
        var $row = button.closest('tr');

        // Get query and response from the row
        var query = $row.find('td:first strong').text();
        var response = $row.find('td:nth-child(2)').text();

        button.prop('disabled', true);
        button.text('Saving...');

        // First, add to knowledge base
        jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/ingest', {
            method: 'POST',
            data: JSON.stringify({
                title: query,
                content: response,
                url: 'auto-accepted-' + id,
                postType: 'manual'
            }),
            xhrFields: {
                withCredentials: false
            },
            crossDomain: true
        }).then(function(ingestResponse) {
            // Then mark as reviewed
            return jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/admin/unanswered-queries/' + id + '/mark-reviewed', {
                method: 'POST',
                data: JSON.stringify({ notes: 'AI answer accepted and added to knowledge base' }),
                xhrFields: {
                    withCredentials: false
                },
                crossDomain: true
            });
        }).then(function() {
            var $editorRow = $('#editor-' + id);
            $row.addClass('reviewed');
            $row.find('.query-actions').html('<span style="color:#46b450;">✓ Accepted & Added</span>');
            $editorRow.remove();
        }).catch(function(xhr) {
            var error = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : xhr.statusText || 'Unknown error';
            alert('Failed to process: ' + error);
            button.prop('disabled', false);
            button.text('Accept Answer');
        });
    });

    // Show inline editor
    $(document).on('click', '.add-info', function() {
        var id = $(this).data('id');
        var $editorRow = $('#editor-' + id);

        // Hide all other editors
        $('.inline-editor-row').hide();

        // Toggle this editor
        $editorRow.toggle();
    });

    // Cancel inline editor
    $(document).on('click', '.cancel-editor', function() {
        var id = $(this).data('id');
        $('#editor-' + id).hide();
    });

    // Ignore (delete) query
    $(document).on('click', '.ignore-query', function() {
        var button = $(this);
        var id = button.data('id');
        var $row = button.closest('tr');

        showModal(
            'Confirm Ignore',
            'Are you sure you want to ignore this query? It will be deleted permanently.',
            function() {
                button.prop('disabled', true).text('Ignoring...');

                jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/admin/unanswered-queries/' + id, {
                    method: 'DELETE',
                    xhrFields: {
                        withCredentials: false
                    },
                    crossDomain: true
                }).then(function(response) {
                    $row.fadeOut(300, function() {
                        $(this).remove();
                        // Reload if table is now empty
                        if ($('#queries-tbody tr').length === 0) {
                            loadUnansweredQueries();
                        }
                    });
                }).catch(function(xhr) {
                    var error = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : xhr.statusText || 'Unknown error';
                    showModal('Error', 'Failed to ignore query: ' + error, function() {});
                    button.prop('disabled', false).text('Ignore');
                });
            }
        );
    });

    // Save info to knowledge base
    $(document).on('click', '.save-info', function() {
        var button = $(this);
        var id = button.data('id');
        var title = $('#editor-title-' + id).val();
        var content = $('#editor-content-' + id).val();
        var url = $('#editor-url-' + id).val() || 'manual-entry-' + Date.now();
        var $result = $('#editor-result-' + id);

        if (!title || !content) {
            $result.html('<div class="notice notice-error"><p>Title and content are required.</p></div>');
            return;
        }

        button.prop('disabled', true);
        $result.html('<p>Saving to knowledge base...</p>');

        jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/ingest', {
            method: 'POST',
            data: JSON.stringify({
                title: title,
                content: content,
                url: url,
                postType: 'manual'
            }),
            xhrFields: {
                withCredentials: false
            },
            crossDomain: true
        }).then(function(response) {
            $result.html('<div class="notice notice-success"><p>Saved to knowledge base! Now marking query as reviewed...</p></div>');

            // Mark the query as reviewed
            return jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/admin/unanswered-queries/' + id + '/mark-reviewed', {
                method: 'POST',
                data: JSON.stringify({ notes: 'Information added to knowledge base' }),
                xhrFields: {
                    withCredentials: false
                },
                crossDomain: true
            });
        }).then(function() {
            var $row = $('[data-query-id="' + id + '"]');
            $row.addClass('reviewed');
            $row.find('.query-actions').html('<span style="color:#46b450;">✓ Reviewed & Added</span>');
            $('#editor-' + id).fadeOut();
        }).catch(function(xhr) {
            var error = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : xhr.statusText || 'Unknown error';
            $result.html('<div class="notice notice-error"><p>Failed to save: ' + error + '</p></div>');
        }).finally(function() {
            button.prop('disabled', false);
        });
    });

    // Handle manual text entry form
    $('#ingest-manual-form').on('submit', function(e) {
        e.preventDefault();

        var $form = $(this);
        var $result = $('#manual-result');
        var $submit = $form.find('button[type="submit"]');

        $submit.prop('disabled', true);
        $result.html('<p>Adding to knowledge base...</p>');

        jwtToken.apiRequest('<?php echo esc_js($api_url); ?>/api/ingest', {
            method: 'POST',
            data: JSON.stringify({
                title: $('#manual-title').val(),
                content: $('#manual-content').val(),
                url: $('#manual-url').val() || 'manual-entry-' + Date.now(),
                postType: 'manual'
            }),
            xhrFields: {
                withCredentials: false
            },
            crossDomain: true
        }).then(function(response) {
            $result.html('<div class="notice notice-success"><p>Content added to knowledge base successfully!</p></div>');
            $form[0].reset();
        }).catch(function(xhr) {
            var error = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : xhr.statusText || 'Unknown error';
            $result.html('<div class="notice notice-error"><p>Failed to add: ' + error + '</p></div>');
        }).finally(function() {
            $submit.prop('disabled', false);
        });
    });

    // Handle file upload form
    $('#ingest-file-form').on('submit', function(e) {
        e.preventDefault();

        var $form = $(this);
        var $result = $('#file-result');
        var $submit = $form.find('button[type="submit"]');

        var file = $('#ingest-file')[0].files[0];
        if (!file) {
            $result.html('<div class="notice notice-error"><p>Please select a file to upload.</p></div>');
            return;
        }

        $submit.prop('disabled', true);
        $result.html('<p>Uploading and processing file...</p>');

        var formData = new FormData();
        formData.append('file', file);
        formData.append('title', $('#file-title').val() || file.name);
        formData.append('url', $('#file-url').val() || 'file-upload-' + Date.now());

        // For file upload, we need to get JWT token and manually build the request
        // because we can't set Content-Type (browser sets it with boundary for FormData)
        jwtToken.get().then(function(token) {
            return $.ajax({
                url: '<?php echo esc_js($api_url); ?>/api/ingest-file',
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                data: formData,
                processData: false,
                contentType: false,
                xhrFields: {
                    withCredentials: false
                },
                crossDomain: true
            });
        }).then(function(response) {
            $result.html('<div class="notice notice-success"><p>File processed and added to knowledge base successfully!</p></div>');
            $form[0].reset();
        }).catch(function(xhr) {
            var errorMsg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : xhr.statusText || 'Unknown error';
            $result.html('<div class="notice notice-error"><p>Failed to process file: ' + errorMsg + '</p></div>');
        }).finally(function() {
            $submit.prop('disabled', false);
        });
    });

    function displayPagination(pageData) {
        var totalElements = pageData.totalElements || 0;
        var totalPages = pageData.totalPages || 0;
        var currentPageNum = (pageData.number || 0) + 1; // Spring pages are 0-indexed
        var size = pageData.size || pageSize;

        // Calculate item range
        var startItem = (currentPage * size) + 1;
        var endItem = Math.min(startItem + size - 1, totalElements);

        // Update display
        $('#pagination-info').text(startItem + ' - ' + endItem + ' of ' + totalElements + ' items');
        $('#current-page-display').text(currentPageNum);
        $('#total-pages-display').text(totalPages);

        // Enable/disable buttons
        $('#first-page, #prev-page').prop('disabled', currentPage === 0);
        $('#next-page, #last-page').prop('disabled', currentPage >= totalPages - 1);

        // Always show pagination to display item count
        $('#queries-pagination').show();
    }

    // Pagination button handlers
    $('#first-page').on('click', function() {
        if (currentPage > 0) {
            loadUnansweredQueries(0);
        }
    });

    $('#prev-page').on('click', function() {
        if (currentPage > 0) {
            loadUnansweredQueries(currentPage - 1);
        }
    });

    $('#next-page').on('click', function() {
        if (currentPage < totalPages - 1) {
            loadUnansweredQueries(currentPage + 1);
        }
    });

    $('#last-page').on('click', function() {
        if (currentPage < totalPages - 1) {
            loadUnansweredQueries(totalPages - 1);
        }
    });

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
});
</script>
