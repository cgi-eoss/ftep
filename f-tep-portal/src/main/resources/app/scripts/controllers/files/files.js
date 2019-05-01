/**
 * @ngdoc function
 * @name ftepApp.controller:FilesCtrl
 * @description
 * # FilesCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('FilesCtrl', ['$scope', 'TabService', 'MessageService', function ($scope, TabService, MessageService) {

        $scope.filesSideNavs = TabService.getFilesSideNavs();
        $scope.navInfo = TabService.navInfo.files;

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
        var navbar, sidenav, search;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'search':
                    search = true;
                    break;
            }

            if (navbar && sidenav && search) {
                $scope.hideContent = false;
            }
        };

    }]);

    return this;

});
