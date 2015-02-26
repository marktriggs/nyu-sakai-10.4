$(function() {

  var $toggle;

  var syncAlertBanner = function() {
    $.getJSON("/portal/system-alerts/banner", function(json, xhr) {
      renderBannerAlerts(json);
    });
  };

  var clearBannerAlerts = function() {
    $(".system-alert-banner").remove();
  };

  var setupAlertBannerToggle = function() {
    $toggle = $($("#systemAlertsBannerToggleTemplate").html());
    $toggle.hide();
    $("#siteNavWrapper").prepend($toggle);

    $toggle.on("click", function() {
      showAllAlerts();
      $toggle.slideUp();
    });
  };

  var showAllAlerts = function() {
    $.cookie("system-alert-banners-dismissed", null, { path: "/" });
    syncAlertBanner();
  };

  var hasAlertBeenDismissed = function(alertId) {
    var dismissedIds = [];
    if ($.cookie("system-alert-banners-dismissed") != null) {
      dismissedIds = $.cookie("system-alert-banners-dismissed").split(",");
    }
    return dismissedIds.indexOf(alertId) >= 0
  };

  var markAlertAsDismissed = function(alertId) {
    if ($.cookie("system-alert-banners-dismissed") != null) {
      var ids = $.cookie("system-alert-banners-dismissed");
      $.cookie("system-alert-banners-dismissed", ids + "," + alertId, { path: "/" });
    } else {
      $.cookie("system-alert-banners-dismissed", alertId, { path: "/" });
    };
  };

  var handleBannerAlertClose = function($alert) {
    markAlertAsDismissed($alert.attr("id"));
    $alert.slideUp(function() {
      $alert.remove();
      $toggle.slideDown();
    });
  };

  var renderBannerAlerts = function(alerts) {
    if (alerts.length == 0) {
      return clearBannerAlerts();
    }

    var dismissedAlertIds = [];
    var activeAlertIds = [];

    // ensure all active alerts are rendered
    $.each(alerts, function(i, alert) {
      var alertId = "bannerAlert"+alert.id;

      activeAlertIds.push(alertId);

      // if alert is not in the DOM.. add it.
      var $alert = $("#"+alertId);
      if ($alert.length == 0) {
          $alert = $($("#systemAlertsBannerTemplate").html()).attr("id", alertId);
          $alert.find(".system-alert-banner-message").html(alert.message);
          if (!alert.dismissible) {
            $alert.find(".system-alert-banner-close").remove();
          }
          $alert.hide();
          $(document.body).prepend($alert);
      }

      if (hasAlertBeenDismissed(alertId)) {
        $toggle.show().slideDown();
      } else {
        $alert.slideDown();
      }
    });

    // remove any alerts that are now inactive
    $(".system-banner-alert").each(function() {
      var $alert = $(this);
      if (activeAlertIds.indexOf($alert.attr("id")) < 0) {
        $alert.remove();
      }
    });
  };

  $(document).on("click", ".system-alert-banner-close", function() {
    handleBannerAlertClose($(this).closest(".system-alert-banner"));
  });

  setupAlertBannerToggle();

  // check now
  syncAlertBanner();
  // and again every 10min
  var interval = setInterval(syncAlertBanner, 1000 * 60 * 10);
});