/**
 * @ngdoc function
 * @name ftepApp.controller:ExplorerCtrl
 * @description
 * # ExplorerCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ExplorerCtrl', ['$scope', '$rootScope', '$mdDialog', 'TabService', 'MessageService', 'ftepProperties', 'CommonService', 'CommunityService', '$timeout', function ($scope, $rootScope, $mdDialog, TabService, MessageService, ftepProperties, CommonService, CommunityService, $timeout) {

        /* Set active page */
        $scope.navInfo = TabService.navInfo.explorer;
        $scope.bottomNavTabs = TabService.getBottomNavTabs();

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event) {
            $scope.message.count = MessageService.countMessages();
        });

        /* SIDE BAR */
        $scope.sideNavTabs = TabService.getExplorerSideNavs();

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

        $scope.$on('update.selectedService', function(event) {
            $scope.navInfo.activeSideNav = $scope.sideNavTabs.WORKSPACE;
            showSidebarArea();
        });
        /* END OF SIDE BAR */

        /** BOTTOM BAR **/
        $scope.displayTab = function(tab, allowedToClose) {
            if ($scope.navInfo.activeBottomNav === tab && allowedToClose !== false) {
                $scope.toggleBottomView();
            } else {
                $scope.navInfo.bottomViewVisible = true;
                $timeout(function () {
                    $scope.navInfo.activeBottomNav = tab;
                }, 600);
            }
        };

        $scope.toggleBottomView = function () {
            $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
        };

        $scope.getOpenedBottombar = function(){
            if($scope.navInfo.bottomViewVisible){
                return $scope.navInfo.activeBottomNav;
            }
            else {
                return undefined;
            }
        };
        /** END OF BOTTOM BAR **/

        /** WMS layer show/hide option for Product Search result items **/
        $scope.visibleWmsList = [];

        /* Toggles display of a wms item */
        $scope.toggleSearchResWMS = function ($event, item, show) {
            if (show) {
                $scope.visibleWmsList.push(item.properties);
            } else {
                var index = $scope.visibleWmsList.indexOf(item.properties);
                $scope.visibleWmsList.splice(index, 1);
            }
            $rootScope.$broadcast('update.wmslayer', $scope.visibleWmsList);
        };

        /* Clear visible WMS-s when map is reset */
        $scope.$on('map.cleared', function () {
            $scope.visibleWmsList = [];
        });

        $scope.hasWmsLink = function(item) {
            return item.properties._links.wms;
        };

        $scope.isSearchResWmsVisible = function(item) {
            return $scope.visibleWmsList.indexOf(item.properties) > -1;
        };
        /** END OF WMS layer show/hide option for Product Search result items **/

        /* Show Result Metadata Modal */
        $scope.showMetadata = function($event, data) {
            function MetadataController($scope, $mdDialog, ftepProperties) {
                $scope.item = data;

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            MetadataController.$inject = ['$scope', '$mdDialog', 'ftepProperties'];

            $mdDialog.show({
                controller: MetadataController,
                templateUrl: 'views/explorer/templates/metadata.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {
                    items: $scope.items
                }
            });
        };

        /* Share Object Modal */
        $scope.sharedObject = {};
        $scope.shareObjectDialog = function($event, item, type, serviceName, serviceMethod) {
            CommunityService.getObjectGroups(item, type).then(function(groups){
                CommonService.shareObjectDialog($event, item, type, groups, serviceName, serviceMethod, 'explorer');
            });
        };

        $scope.hideContent = true;
        var map, sidenav, navbar;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'map':
                    map = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'navbar':
                    navbar = true;
                    break;
            }

            if (map && sidenav && navbar) {
                $scope.hideContent = false;
            }
        };

    }]);
});
