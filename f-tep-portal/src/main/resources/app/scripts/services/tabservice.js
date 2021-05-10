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
        var tabs = { EXPLORER: 0, FILES: 1, DEVELOPER: 2, COMMUNITY: 3, ACCOUNT: 4 };
        var bottomNavTabs = { RESULTS: 0, DATABASKETS: 1, JOBS: 2, MESSAGES: 3 };
        var communityTabs = { GROUPS: 0, PROJECTS: 1, DATABASKETS: 2, JOBS: 3, SERVICES: 4, FILES: 5};
        var explorerSideNavs = { SEARCH: 0, SERVICES: 1, WORKSPACE: 2 };
        var developerSideNavs = { SERVICES: 0 };
        var filesSideNavs = { SEARCH: 0, UPLOAD: 1 };

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

        this.getFilesSideNavs = function(){
            return angular.copy(filesSideNavs);
        };

        /** PRESERVE USER SELECTIONS **/
        this.navInfo = {
            explorer: {
                activeTab: tabs.EXPLORER,
                sideViewVisible: false,
                activeSideNav: undefined,
                activeBottomNav: bottomNavTabs.RESULTS,
                resultTabNameExtention: '',
                bottomViewVisible: false
            },
            files: {
                activeTab: tabs.FILES,
                sideViewVisible: true,
                activeSideNav: filesSideNavs.SEARCH,
                activeBottomNav: undefined,
                bottomViewVisible: false
            },
            developer: {
                activeTab: tabs.DEVELOPER,
                sideViewVisible: true,
                activeSideNav: developerSideNavs.SERVICES,
                activeBottomNav: undefined,
                bottomViewVisible: false
            },
            community: {
                activeTab: tabs.COMMUNITY,
                sideViewVisible: true,
                activeSideNav: communityTabs.GROUPS,
                activeBottomNav: undefined,
                bottomViewVisible: false
            },
            admin: {
                sideViewVisible: false,
                bottomViewVisible: false
            }
        };
        /** END OF PRESERVE USER SELECTIONS **/
        return this;
    }]);
});
