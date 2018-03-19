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
        // When a new Job has been started flip a flag
        var newjobWithBottombar = false;
        $scope.$on('newjob.started.init', function() {
            newjobWithBottombar = true;
        });

        $scope.displayTab = function(tab, allowedToClose) {
            if (newjobWithBottombar || allowedToClose === undefined) {
                $scope.toggleBottomView(true);
                // Transition without animation
                $scope.navInfo.activeBottomNav = tab;
            } else {
                if ($scope.navInfo.activeBottomNav === tab && allowedToClose !== false) {
                    // No need to switch tab
                    $scope.toggleBottomView();
                } else {
                    $scope.toggleBottomView(true);
                    // Transition with animation
                    $timeout(function () {
                        $scope.navInfo.activeBottomNav = tab;
                    }, 100);
                }
            }
        };

        // Triggering scroll action on the Jobs List when the document element is visible
        $scope.$on('newjob.started.show', function() {
            if (newjobWithBottombar) {
                // Timered watcher
                var checkExist = setInterval(function() {
                    // Scroll to first job item
                    document.querySelector("#jobs .job-list-item:nth-child(1)").scrollIntoView(true);
                    if ($('#jobs-container').length) {
                        // Resetting flag
                        newjobWithBottombar = false;
                        // Remove timer
                        clearInterval(checkExist);
                    }
                }, 100);
            }
        });

        $scope.toggleBottomView = function (flag) {
            if (flag === undefined) {
                $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
            } else {
                $scope.navInfo.bottomViewVisible = flag;
            }
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

                $scope.estimateAndDownload = function() {
                    CommonService.estimateDownloadCost($event, $scope.item.properties._links.download.href);
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
