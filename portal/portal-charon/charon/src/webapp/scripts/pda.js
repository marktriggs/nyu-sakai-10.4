// NYU Script for PDA Specific Javascript
$(function() {

  var PORTAL_BODY_SELECTOR = "body.portalBodyPDA";

  var displaySelectInlineHelp = function() {
    // Only show help tip for 'multiple' selects
    // as single selects look like dropdown lists
    $("select[multiple]", PORTAL_BODY_SELECTOR).after("<small class='select-inline-help'>Tap to select option(s)</small>");
  };

	var displayWelcomePopup = function() {
		if ($("#pdaWelcomeDialog").length > 0) {
			var _onScroll = function() {
				$('#qtip-blanket').css("top", $(document).scrollTop());
			};

			var _onResize = function(event) {
				$("#pdaWelcomeDialog").qtip('api').updateWidth($(window).width() - 20);
				$("#pdaWelcomeDialog").qtip('api').updatePosition(event, false);
				$('#qtip-blanket').css("height", $(document).height());
			};

			$("#pdaWelcomeDialog").qtip({
				content: {
					title: "Welcome to NYU Classes Mobile",
					button: '<a href="#"><img src="/library/image/silk/cancel.png"/></a>',
					text: $("#pdaWelcomeDialogContent").html()
				},
				position: {
					target: $(document.body),
					my: 'center',
					at: 'center'
				},
				show: {
					ready: true, // Show it when ready
					solo: true // And hide all other tooltips
				},
				hide: false,
				style: {
					width: {
						max: $(document).width() - 20,
					},
					border: {
						width: 9,
						radius: 9,
						color: '#666666'
					},
					padding: '14px',
					name: 'light'
				},
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

						$("#pdaPopupContinue").click(function() {
							$("#pdaWelcomeDialog").qtip("hide");
						});
					},
					hide: function() {
						$('#qtip-blanket').fadeOut(function() {
							$('#qtip-blanket').remove();
						});
						$("#pdaWelcomeDialog").qtip("destroy");
						$(window).unbind("scroll", _onScroll);
						$(window).unbind("resize", _onResize);
					},
				}
			});
		}
	};

  // Things to run on document ready
  displaySelectInlineHelp();
  displayWelcomePopup();
});