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

        var tabs = { EXPLORER: 0, DEVELOPER: 1, COMMUNITY: 2, ACCOUNT: 3, HELPDESK: 4 };

        var bottomNavTabs = { RESULTS: 0, DATABASKETS: 1, JOBS: 2, MESSAGES: 3 };

        var communityTabs = { GROUPS: 0, PROJECTS: 1, DATABASKETS: 2, JOBS: 3, SERVICES: 4, FILES: 5};

        var explorerSideNavs = { SEARCH: 0, SERVICES: 1, WORKSPACE: 2 };

        var developerSideNavs = { SERVICES: 0 };

        this.getTabs = function(){
            return angular.copy(tabs);
        };

        this.getExplorerSideNavs = function(){
            return angular.copy(explorerSideNavs);
        };

        this.getBottomNavTabs = function(){
            return angular.copy(bottomNavTabs);
        };

        this.getCommunityNavTabs = function(){
            return angular.copy(communityTabs);
        };

        this.getDeveloperSideNavs = function(){
            return angular.copy(developerSideNavs);
        };

        /** PRESERVE USER SELECTIONS **/
        this.navInfo = {
                explorer: {
                    activeTab: tabs.EXPLORER,
                    sideViewVisible: false,
                    activeSideNav: undefined,
                    activeBottomNav: bottomNavTabs.RESULTS,
                    resultTabNameExtention: ''
                },
                developer: {
                    activeTab: tabs.DEVELOPER,
                    sideViewVisible: true,
                    activeSideNav: developerSideNavs.SERVICES,
                    activeBottomNav: undefined
                },
                community: {
                    activeTab: tabs.COMMUNITY,
                    sideViewVisible: true,
                    activeSideNav: communityTabs.GROUPS,
                    activeBottomNav: undefined
                },
                admin: {
                    sideViewVisible: false
                },
                bottombar: {
                    bottomViewVisible: false
                }
        };

        /** END OF PRESERVE USER SELECTIONS **/

        return this;
    }]);
});
