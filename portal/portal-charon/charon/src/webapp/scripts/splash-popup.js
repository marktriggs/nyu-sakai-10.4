$(function() {

    var permanentlyAcknowledged = false;

    var popupContent = $('#popup-container-content');
    var campaign = popupContent.data('popup-campaign');
    var csrf_token = popupContent.data('csrf-token');

    var acknowledge = function (campaign, acknowledgement) {
        $.ajax({
            method: 'POST',
            url: '/portal/popupAcknowledge',
            data: {
                campaign: campaign,
                acknowledgement: acknowledgement,
                sakai_csrf_token: csrf_token
            },
        });
    };


    if (campaign) {
        $.featherlight(popupContent.html() + $('#popup-container-footer').html(),
                       {
                           afterClose : function (event) {
                               var acknowledgement = permanentlyAcknowledged ? 'permanent' : 'temporary';
                               acknowledge(campaign, acknowledgement);
                           },
                           afterContent : function (event) {
                               $('#popup-acknowledged-button').on('click', function () {
                                   permanentlyAcknowledged = true;
                                   $.featherlight.current().close();
                               });
                           }
                       });
    }
});
