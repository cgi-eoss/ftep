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

        var sideNavTabs = { SEARCH: 0, SERVICES: 1, WORKSPACE: 2 };

        var bottomNavTabs = { RESULTS: 0, DATABASKETS: 1, JOBS: 2, MESSAGES: 3 };

        this.getSideNavTabs = function(){
            return angular.copy(sideNavTabs);
        }

        this.getBottomNavTabs = function(){
            return angular.copy(bottomNavTabs);
        }

        /** PRESERVE USER SELECTIONS **/
        this.navInfo = {
                activeSideNav: undefined,
                activeBottomNav: bottomNavTabs.RESULTS,
                bottomViewVisible: false,
                sideViewVisible: false
        };

        /** END OF PRESERVE USER SELECTIONS **/

        this.resultTab = { nameExtention: '' };

        return this;
    }]);
});
