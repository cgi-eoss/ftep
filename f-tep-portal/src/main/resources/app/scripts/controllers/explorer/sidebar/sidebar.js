/**
 * @ngdoc function
 * @name ftepApp.controller:SidebarCtrl
 * @description
 * # SidebarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('SidebarCtrl', function ($scope) {

        $scope.searchMenuVisible = false;
        $scope.nav_active = "none";

        $scope.showSearchArea = function () {
            $scope.searchMenuVisible = true;
        };

        $scope.hideSearchArea = function ($event) {
            $event.stopPropagation();
            $event.preventDefault();
            $scope.searchMenuVisible = false;
        };

        $scope.toggleSearchArea = function (section) {

            $scope.$broadcast('rebuild:scrollbar');

            if ($scope.nav_active === "none") {
                $scope.searchMenuVisible = true;

            } else if ($scope.nav_active === section) {
                $scope.searchMenuVisible = !$scope.searchMenuVisible;
            } else {
                $scope.searchMenuVisible = true;
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
