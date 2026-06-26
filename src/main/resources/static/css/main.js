// src/main/resources/static/js/main.js
$(document).ready(function() {
    // Enable Bootstrap tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    });

    // Auto-hide alerts after 5 seconds
    setTimeout(function() {
        $('.alert').fadeOut('slow');
    }, 5000);

    // Confirm delete for books
    window.confirmDelete = function(bookId, bookTitle) {
        $('#deleteBookTitle').text(bookTitle);
        $('#deleteForm').attr('action', '/books/' + bookId);
        var deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
        deleteModal.show();
    };

    // Confirm delete for members
    window.confirmDeleteMember = function(memberId, memberName) {
        if (confirm('Are you sure you want to delete member: ' + memberName + '? This action cannot be undone.')) {
            $.ajax({
                url: '/api/members/' + memberId,
                type: 'DELETE',
                success: function(result) {
                    location.reload();
                },
                error: function(err) {
                    alert('Error deleting member: ' + err.responseJSON.message);
                }
            });
        }
    };

    // Search functionality with debounce
    let searchTimeout;
    $('.search-input').on('keyup', function() {
        clearTimeout(searchTimeout);
        const searchTerm = $(this).val();
        const table = $(this).closest('.card').find('table tbody');

        searchTimeout = setTimeout(function() {
            if (searchTerm.length >= 2) {
                table.find('tr').each(function() {
                    const rowText = $(this).text().toLowerCase();
                    $(this).toggle(rowText.indexOf(searchTerm.toLowerCase()) > -1);
                });
            } else {
                table.find('tr').show();
            }
        }, 300);
    });

    // Form validation enhancement
    $('form').on('submit', function(e) {
        const form = this;
        if (!form.checkValidity()) {
            e.preventDefault();
            e.stopPropagation();
        }
        $(form).addClass('was-validated');
    });

    // Dynamic available copies calculation
    $('#totalCopies').on('change', function() {
        const total = $(this).val();
        $('#availableCopies').attr('max', total);
        if ($('#availableCopies').val() > total) {
            $('#availableCopies').val(total);
        }
    });

    // Date formatting
    $('.date-format').each(function() {
        const date = new Date($(this).text());
        if (!isNaN(date.getTime())) {
            $(this).text(date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            }));
        }
    });

    // Table sorting
    $('.sortable').click(function() {
        const table = $(this).closest('table');
        const tbody = table.find('tbody');
        const rows = tbody.find('tr').toArray();
        const column = $(this).index();

        rows.sort(function(a, b) {
            const aVal = $(a).find('td').eq(column).text();
            const bVal = $(b).find('td').eq(column).text();
            return aVal.localeCompare(bVal);
        });

        $.each(rows, function(index, row) {
            tbody.append(row);
        });
    });

    // Print functionality
    window.printPage = function() {
        window.print();
    };

    // Refresh page
    window.refreshPage = function() {
        location.reload();
    };

    // Handle API errors globally
    $(document).ajaxError(function(event, jqxhr, settings, thrownError) {
        console.error('API Error:', thrownError);
        if (jqxhr.status === 403) {
            alert('You do not have permission to perform this action.');
        } else if (jqxhr.status === 404) {
            alert('Resource not found.');
        } else if (jqxhr.status === 500) {
            alert('Internal server error. Please try again later.');
        }
    });
});

// Prevent accidental double form submissions
$('form').on('submit', function() {
    $(this).find('button[type="submit"]').prop('disabled', true);
});