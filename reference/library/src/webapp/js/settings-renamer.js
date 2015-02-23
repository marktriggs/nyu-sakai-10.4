var OLD_TOOL_LABEL = 'Settings';
var SETTINGS_TOOL_LABEL = 'Site Groups';

function renameSettingsToJoinable(title) {
    if (showSiteInfoAsSettings || title != OLD_TOOL_LABEL) {
        return title;
    } else {
        return SETTINGS_TOOL_LABEL;
    }
}


var findSettingsMenuLink = function () {
    var result = undefined;

    $('.toolMenuLink').each (function (idx, link) {
        var title = $(link).find('.menuTitle');
        if (title.text() === OLD_TOOL_LABEL) {
            result = link;
        }
    });

    return result;
};

var switchToJoinableGroups = function (link) {
    var title = $(link).find('.menuTitle');
    title.text(SETTINGS_TOOL_LABEL)

    var iconSpan = $(link).find('span.toolMenuIcon');
    iconSpan.removeClass('icon-sakai-siteinfo').addClass('icon-sakai-joinable-groups');
};


var markAsHidden = function (elt) {
    $(elt).addClass("hidden");
};


$(document).ready(function() {
    var settingsTool = findSettingsMenuLink();

    if (showSiteInfoAsSettings) {
        markAsHidden(settingsTool);

        return;
    }

    if (!settingsTool) {
        return;
    }

    if (showJoinableGroups) {
        switchToJoinableGroups(settingsTool);

        var tool = $('.tool-sakai-siteinfo');
        if (tool.length > 0) {
            tool.find('.portletTitle .title h2').text(SETTINGS_TOOL_LABEL);
        }
    } else {
        $(settingsTool).closest('li').hide();
    }
});
