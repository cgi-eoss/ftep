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
        $scope.section = { SEARCH: 'search', SERVICES: 'services', WORKSPACE: 'workspace'};
        $scope.nav_active = undefined;

        var sidebarWidth = $('#sidebar-left').width();
        var sidenavWidth = $('#sidenav').width();

        function showSidebarArea() {
            $scope.searchMenuVisible = true;
            $mdSidenav('left').open();
            $("#bottombar").css("left", sidebarWidth + 44);
            $("#bottombar").css({ 'width': 'calc(100% - ' + (sidebarWidth + 44) + 'px)'});
            $scope.$broadcast('rebuild:scrollbar');
            $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
            }, 300);
        };

        $scope.hideSidebarArea = function () {
            $scope.searchMenuVisible = false;
            $scope.nav_active = undefined;
            $mdSidenav('left').close();
            $("#bottombar").css("left", sidenavWidth + 44);
            $("#bottombar").css({ 'width': 'calc(100% - ' + 44 + 'px)' });
            $scope.$broadcast('rebuild:scrollbar');
        };

        $scope.$on('rerun.service', function(event) {
            $scope.nav_active = $scope.section.WORKSPACE;
            showSidebarArea();
        });

        $scope.toggleSidebar = function (section) {

            $scope.$broadcast('rebuild:scrollbar');
            if($scope.nav_active === section){
                $scope.hideSidebarArea();
            }
            else{
                $scope.nav_active = section;
                showSidebarArea();
            }
        };

    });

});
