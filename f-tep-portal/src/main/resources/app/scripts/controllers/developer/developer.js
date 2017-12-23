/**
 * @ngdoc function
 * @name ftepApp.controller:DeveloperCtrl
 * @description
 * # DeveloperCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('DeveloperCtrl', ['$scope', 'TabService', 'MessageService', function ($scope, TabService, MessageService) {

        $scope.developerSideNavs = TabService.getDeveloperSideNavs();
        $scope.navInfo = TabService.navInfo.developer;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.toggleBottomView = function(){
            $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
        };

        function showSidebarArea() {
            $scope.navInfo.sideViewVisible = true;
        }

        $scope.hideSidebarArea = function () {
            $scope.navInfo.activeSideNav = undefined;
            $scope.navInfo.sideViewVisible = false;
        };

        $scope.toggleSidebar = function (tab) {
            if($scope.navInfo.activeSideNav === tab) {
                $scope.hideSidebarArea();
            } else {
                $scope.navInfo.activeSideNav = tab;
                showSidebarArea();
            }
        };

        $scope.hideContent = true;
        var navbar, sidenav, services;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'services':
                    services = true;
                    break;
            }

            if (navbar && sidenav && services) {
                $scope.hideContent = false;
            }
        };

    }]);

});
