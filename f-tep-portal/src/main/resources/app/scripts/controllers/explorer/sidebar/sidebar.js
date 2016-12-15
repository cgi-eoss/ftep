/**
 * @ngdoc function
 * @name ftepApp.controller:SidebarCtrl
 * @description
 * # SidebarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('SidebarCtrl', function ($scope, $timeout, $mdSidenav) {

        $scope.searchMenuVisible = false;
        $scope.nav_active = "none";

        var sidebarWidth = $('#sidebar-left').width();
        var sidenavWidth = $('#sidenav').width();

        $scope.showSearchArea = function () {
            $scope.searchMenuVisible = true;
            $mdSidenav('left').open();
            $("#bottombar").css("left", sidebarWidth + 44);
            $("#bottombar").css({ 'width': 'calc(100% - ' + (sidebarWidth + 44) + 'px)'});
            $scope.$broadcast('rebuild:scrollbar');
        };

        $scope.hideSearchArea = function ($event) {
            $scope.searchMenuVisible = false;
            $mdSidenav('left').close();
            $("#bottombar").css("left", sidenavWidth + 44);
            $("#bottombar").css({ 'width': 'calc(100% - ' + 44 + 'px)' });
            $scope.$broadcast('rebuild:scrollbar');
        };

        $scope.toggleSidebar = function (section) {

            $scope.$broadcast('rebuild:scrollbar');

            if ($scope.nav_active === "none") {
                $scope.showSearchArea();
            } else if ($scope.nav_active === section) {

                if ($scope.searchMenuVisible) {
                    $scope.hideSearchArea();
                    $scope.searchMenuVisible = false;
                } else {
                    $scope.showSearchArea();
                    $scope.searchMenuVisible = true;
                }
            } else {
                $scope.showSearchArea();
            }

            $scope.nav_active = section;

            $scope.nav_search = false;
            $scope.nav_databaskets = false;
            $scope.nav_services = false;
            $scope.nav_workspace = false;
            $scope.nav_jobs = false;

            switch (section) {
            case "search":
                $scope.nav_search = true;
                break;
            case "databasket":
                $scope.nav_databaskets = true;
                break;
            case "services":
                $scope.nav_services = true;
                break;
            case "workspace":
                $scope.nav_workspace = true;
                break;
            case "jobs":
                $scope.nav_jobs = true;
                break;
            }
        };

    });

});
