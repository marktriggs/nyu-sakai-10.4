// NYU Script for showing the Tutorial popup
$(function() {

	var PORTAL_BODY_SELECTOR = "body.portalBody";
	var POPUP_SELECTOR = "#tutorialPopup";
	var POPUP_CONTENT_TEMPLATE_SELECTOR = "#tutorialPopupContent";

	var displayWelcomePopup = function() {
		if ($(POPUP_SELECTOR).length > 0) {
			var $popup = $(POPUP_SELECTOR);

			var _onScroll = function() {
				$('#qtip-blanket').css("top", $(document).scrollTop());
			};

			var _onResize = function(event) {
				$('#qtip-blanket').css("height", $(document).height());
			};

			$popup.qtip({
			  id: "tutorialPopup",
				content: {
					text: $(POPUP_CONTENT_TEMPLATE_SELECTOR).html()
				},
				position: {
					target: $(document.body),
					my: 'top center',
					at: 'top center'
				},
				show: {
					ready: true, // Show it when ready
					solo: true // And hide all other tooltips
				},
				hide: false,
				events: {
					show: function() {
						$('<div id="qtip-blanket">')
								.css({
									 position: 'absolute',
									 top: $(document).scrollTop(),
									 left: 0,
									 height: $(document).height(),
									 width: '100%',
									 opacity: 0.7,
									 backgroundColor: 'black',
									 zIndex: 5000
								}).appendTo(document.body).fadeIn();
						$(window).bind("scroll", _onScroll);
						$(window).bind("resize", _onResize);

						$(document).on("click", ".tutorialPopupContinue", function() {
							$popup.qtip("hide");
						});
					},
					hide: function() {
						$('#qtip-blanket').fadeOut(function() {
							$('#qtip-blanket').remove();
						});
						$popup.qtip("destroy");
						$(window).unbind("scroll", _onScroll);
						$(window).unbind("resize", _onResize);
						$(document).off("click", ".tutorialPopupContinue");
					},
				}
			});
		}
	};

	// Things to run on document ready
	if (sakai.tutorial_popup.display_on_load == true) {
		displayWelcomePopup();
	}

  // Setup a callback so we can open the dialog from the menu
  sakai.tutorial_popup.open_it = displayWelcomePopup;
});