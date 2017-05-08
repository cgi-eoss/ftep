/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityCtrl
 * @description
 * # CommunityCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityCtrl', ['$scope', 'GroupService', 'UserService', 'MessageService', 'TabService', function ($scope, GroupService, UserService, MessageService, TabService) {

        $scope.navInfo = TabService.navInfo.community;
        $scope.bottombarNavInfo = TabService.navInfo.bottombar;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        /* Sidebar navigation */
        $scope.communityTabs = TabService.getCommunityNavTabs();
        $scope.togglePage = function (tab) {
            $scope.navInfo.activeSideNav = tab;
        };

        /** Bottom bar **/
        $scope.displayTab = function(tab){
            $scope.bottombarNavInfo.bottomViewVisible = true;
            $scope.bottombarNavInfo.activeBottomNav = tab;
        };

        $scope.toggleBottomView = function(){
            $scope.bottombarNavInfo.bottomViewVisible = !$scope.bottombarNavInfo.bottomViewVisible;
        };

        $scope.hideContent = true;
        var navbar, sidenav, manage;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'manage':
                    manage = true;
                    break;
            }

            if (navbar && sidenav && manage) {
                $scope.hideContent = false;
            }
        };

    }]);
});
