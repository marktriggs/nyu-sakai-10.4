var OLD_TOOL_LABEL = 'Settings';
var SETTINGS_TOOL_LABEL = 'Site Groups';

function PortalRenamer() {
    "use strict";

    var $toolMenuItems = $();

    $('.toolMenuLink').each(function (idx, link) {
        var title = $(link).find('.menuTitle');
        if (title.text() === OLD_TOOL_LABEL) {
            $toolMenuItems = $toolMenuItems.add($(link));
        }
    });

    return {
        displayAsHiddenFromStudents: function () {
            $toolMenuItems.addClass("hidden");
        },

        rename: function () {
            // Change the title in the tool menu
            var title = $toolMenuItems.find('.menuTitle');
            title.text(SETTINGS_TOOL_LABEL);

            var iconSpan = $toolMenuItems.find('span.toolMenuIcon');
            iconSpan.removeClass('icon-sakai-siteinfo').addClass('icon-sakai-joinable-groups');


            // If siteinfo is selected as the current tool, change the Tool header too.
            var tool = $('.tool-sakai-siteinfo');
            if (tool.length > 0) {
                tool.find('.portletTitle .title h2').text(SETTINGS_TOOL_LABEL);
            }
        },

        hideTool: function () {
            $toolMenuItems.closest('li').hide();
        }
    };
}


function PDARenamer() {
    "use strict";

    var $toolMenuItems = $();

    $('#pda-portlet-page-menu li a').each(function (idx, link) {
        if ($(link).text() === OLD_TOOL_LABEL) {
            $toolMenuItems = $toolMenuItems.add($(link));
        }
    });

    return {
        displayAsHiddenFromStudents: function () {
            $toolMenuItems.addClass("hidden");
        },

        rename: function () {
            // Change the title in the tool menu
            $toolMenuItems.text(SETTINGS_TOOL_LABEL);

            $toolMenuItems.removeClass('icon-sakai-siteinfo').addClass('icon-sakai-joinable-groups');

            // If siteinfo is selected as the current tool, change the Tool header too.
            var toolTitle = $('.currentToolTitle span');
            if (toolTitle.text() === OLD_TOOL_LABEL) {
                toolTitle.text(SETTINGS_TOOL_LABEL);
            }
        },

        hideTool: function () {
            $toolMenuItems.closest('li').hide();
        }
    };
}


function renameSettingsToJoinable(title) {
    "use strict";

    if (showSiteInfoAsSettings || title !== OLD_TOOL_LABEL) {
        return title;
    }

    return SETTINGS_TOOL_LABEL;
}


$(document).ready(function () {
    var settingsRenamer = ($('body.portalBodyPDA').length > 0) ? PDARenamer() : PortalRenamer();

    if (showSiteInfoAsSettings) {
        // Instructor view
        settingsRenamer.displayAsHiddenFromStudents();
        return;
    }

    if (showJoinableGroups) {
        // We have something to show.  Display the tool with the correct name.
        settingsRenamer.rename();
    } else {
        // Nothing to show, so just hide the whole thing.
        settingsRenamer.hideTool();
    }
});
