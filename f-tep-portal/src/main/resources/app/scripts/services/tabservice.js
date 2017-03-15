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

        var sideNavTabs = { SEARCH: 0, SERVICES: 1, WORKSPACE: 2 };

        var bottomNavTabs = { RESULTS: 0, DATABASKETS: 1, JOBS: 2, MESSAGES: 3 };

        var communityTabs = { MANAGE: 0, SHARE: 1};

        var developerSideNavs = { REFERENCE_DATA: 0, SERVICES: 1 };

        this.getTabs = function(){
            return angular.copy(tabs);
        };

        this.getSideNavTabs = function(){
            return angular.copy(sideNavTabs);
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
                activeTab: tabs.EXPLORER,
                activeSideNav: undefined,
                activeBottomNav: bottomNavTabs.RESULTS,
                bottomViewVisible: false,
                sideViewVisible: false,
                activeCommunityPage: communityTabs.MANAGE,
                activeDeveloperPage: developerSideNavs.REFERENCE_DATA
        };

        /** END OF PRESERVE USER SELECTIONS **/

        this.resultTab = { nameExtention: '' };

        return this;
    }]);
});
