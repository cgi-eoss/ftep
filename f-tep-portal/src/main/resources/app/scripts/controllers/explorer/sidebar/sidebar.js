/**
 * @ngdoc function
 * @name ftepApp.controller:SidebarCtrl
 * @description
 * # SidebarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('SidebarCtrl', ['$scope', '$timeout', '$mdSidenav', 'TabService', function ($scope, $timeout, $mdSidenav, TabService) {

        $scope.sideNavTabs = TabService.getExplorerSideNavs();
        $scope.bottomNavTabs = TabService.getBottomNavTabs();
        $scope.navInfo = TabService.navInfo.explorer;

        function showSidebarArea() {
            $scope.navInfo.sideViewVisible = true;
            $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
            }, 50);
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

        $scope.$on('update.selectedService', function(event) {
            $scope.navInfo.activeSideNav = $scope.sideNavTabs.WORKSPACE;
            showSidebarArea();
        });

    }]);

});
