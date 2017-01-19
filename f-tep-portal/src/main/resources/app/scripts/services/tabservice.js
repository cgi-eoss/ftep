/**
 * @ngdoc service
 * @name ftepApp.TabService
 * @description
 * # TabService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('TabService', [ '$q', function ($q) {

        var tabs = {};
        var activeTab = "";

        tabs.side = [
            {
                name: "SEARCH",
                active: true
            },
            {
                name: "SERVICES",
                active: false
            },
            {
                name: "WORKSPACE",
                active: false
            }
        ];

        tabs.bottom = [
            {
                name: "RESULTS",
                active: true
            },
            {
                name: "DATABASKETS",
                active: false
            },
            {
                name: "JOBS",
                active: false
            },
            {
                name: "MESSAGES",
                active: false
            },
        ];

        this.setActiveTab = function (location, tabName) {
            for (var item in tabs[location]) {
                if (tabs[location][item].name === tabName) {
                     tabs[location][item].active = true;
                     activeTab = item;
                } else {
                     tabs[location][item].active = false;
                }
            }
            return activeTab;
        };

        return this;
    }]);
});
