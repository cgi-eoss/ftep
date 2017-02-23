/**
 * @ngdoc function
 * @name ftepApp.controller:SidebarCtrl
 * @description
 * # SidebarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('SidebarCtrl', function ($scope, $timeout, $mdSidenav, TabService) {

        $scope.sideViewVisible = TabService.sideViewVisible;
        $scope.sideNavTabs = TabService.getSideNavTabs();
        $scope.bottomNavTabs = TabService.getBottomNavTabs();
        $scope.navInfo = TabService.navInfo;

        function getSidebarWidth() {
             return $('#sidebar-left').width();
        }

        function getSidenavWidth() {
            return $('#sidenav').width() === 0 ? 44 : $('#sidenav').width();
        }

        function showSidebarArea() {
            var sidebarWidth = getSidebarWidth();
            var sidenavWidth = getSidenavWidth();
            $scope.navInfo.sideViewVisible = true;
            $mdSidenav('left').open();
            $("#bottombar").css("left", sidebarWidth + sidenavWidth);
            $("#bottombar").css({ 'width': 'calc(100% - ' + (sidebarWidth + sidenavWidth) + 'px)'});
            $scope.$broadcast('rebuild:scrollbar');
            $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
            }, 300);
        }

        $scope.hideSidebarArea = function () {
            var sidebarWidth = getSidebarWidth();
            var sidenavWidth = getSidenavWidth();
            if (sidenavWidth === 0) {
                sidenavWidth = 44;
            }
            $scope.navInfo.sideViewVisible = false;
            $scope.navInfo.activeSideNav = undefined;
            $mdSidenav('left').close();
            $("#bottombar").css("left", sidenavWidth);
            $("#bottombar").css({ 'width': 'calc(100% - ' + sidenavWidth + 'px)' });
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

    });

});
