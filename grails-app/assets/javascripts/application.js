// This is a manifest file that'll be compiled into application.js.
//
// Any JavaScript file within this directory can be referenced here using a relative path.
//
// You're free to add application-wide JavaScript to this file, but it's generally better 
// to create separate JavaScript files as needed.
//
//= require jquery
//= require js/jquery-ui-1.10.4.custom
//= require jquery.cookie
//= require bootstrap
//= require apniFormat
//= require_self

if (typeof jQuery !== 'undefined') {
    window.console && console.log("Yes we have JQuery");
    (function ($) {
        $('#spinner').ajaxStart(function () {
            $(this).fadeIn();
        }).ajaxStop(function () {
            $(this).fadeOut();
        });

    })(jQuery);
}

$(function () {

    function worker() {
        var logs = $('body').find('#logs').length;
        if (logs == 1) {
            $.ajax({
                url: 'logs',
                success: function (data, statusText, jqXHR) {
                    //note redirects will be followed
                    if (jqXHR.status == 200) {
                        $('#logs').html(data);
                        setTimeout(worker, 5000);
                    } else {
                        location.reload(true);
                    }
                }
            });
        }
    }

    worker();


    $('#productDescription').on('closed.bs.alert', function () {
        var cookieName = 'close' + $(this).data('product') + 'Description';
        $.cookie(cookieName, 'true', {expires: 30});
    }).each(function () {
        var cookieName = 'close' + $(this).data('product') + 'Description';
        var closeProductDescription = $.cookie(cookieName);
        if (closeProductDescription) {
            $('#productDescription').alert('close');
        }
    });

    $('help').click(function (event) {
        $(this).children('div').toggle();
        event.preventDefault();
    });

    $('input.fromDate').datepicker({dateFormat: 'd/m/yy', defaultDate: '-1w', maxDate: 0});
    $('input.toDate').datepicker({dateFormat: 'd/m/yy', maxDate: 0});

});
