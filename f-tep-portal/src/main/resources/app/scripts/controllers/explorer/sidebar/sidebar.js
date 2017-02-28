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

        $scope.sideViewVisible = TabService.sideViewVisible;
        $scope.sideNavTabs = TabService.getSideNavTabs();
        $scope.bottomNavTabs = TabService.getBottomNavTabs();
        $scope.navInfo = TabService.navInfo;

        var sidebarWidth = $('#sidebar-left').width();
        var sidenavWidth = $('#sidenav').width();

        function showSidebarArea() {
            $scope.navInfo.sideViewVisible = true;
            $mdSidenav('left').open();
            $("#bottombar").css("left", sidebarWidth + 44);
            $("#bottombar").css({ 'width': 'calc(100% - ' + (sidebarWidth + 44) + 'px)'});
            $scope.$broadcast('rebuild:scrollbar');
            $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
            }, 300);
        }

        $scope.hideSidebarArea = function () {
            $scope.navInfo.sideViewVisible = false;
            $scope.navInfo.activeSideNav = undefined;
            $mdSidenav('left').close();
            $("#bottombar").css("left", sidenavWidth + 44);
            $("#bottombar").css({ 'width': 'calc(100% - ' + 44 + 'px)' });
            $scope.$broadcast('rebuild:scrollbar');
        };

        $scope.$on('rerun.service', function(event) {
            $scope.navInfo.activeSideNav = $scope.sideNavTabs.WORKSPACE;
            showSidebarArea();
        });

        $scope.toggleSidebar = function (tab) {

            $scope.$broadcast('rebuild:scrollbar');
            if($scope.navInfo.activeSideNav === tab){
                $scope.hideSidebarArea();
            }
            else{
                $scope.navInfo.activeSideNav = tab;
                showSidebarArea();
            }
        };

        function setup(){
            if($scope.navInfo.sideViewVisible){
                $timeout(function () {
                    showSidebarArea();
                }, 300);
            }
        }
        setup();

    }]);

});
