/**
 * @ngdoc function
 * @name ftepApp.controller:ExplorerCtrl
 * @description
 * # ExplorerCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ExplorerCtrl', ['$scope', '$mdDialog', 'TabService', 'MessageService', '$mdSidenav', 'ftepProperties', '$injector', function ($scope, $mdDialog, TabService, MessageService, $mdSidenav, ftepProperties, $injector) {

        /* Set active page */
        $scope.navInfo = TabService.navInfo.explorer;
        $scope.bottombarNavInfo = TabService.navInfo.bottombar;
        $scope.bottomNavTabs = TabService.getBottomNavTabs();

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event) {
            $scope.message.count = MessageService.countMessages();
        });

        /** BOTTOM BAR **/
        $scope.displayTab = function(tab, allowedToClose) {
            if ($scope.navInfo.activeBottomNav === tab && allowedToClose !== false) {
                $scope.toggleBottomView();
            } else {
                $scope.bottombarNavInfo.bottomViewVisible = true;
                $scope.navInfo.activeBottomNav = tab;
            }
        };

        $scope.toggleBottomView = function () {
            $scope.bottombarNavInfo.bottomViewVisible = !$scope.bottombarNavInfo.bottomViewVisible;
        };

        $scope.getOpenedBottombar = function(){
            if($scope.bottombarNavInfo.bottomViewVisible){
                return $scope.navInfo.activeBottomNav;
            }
            else {
                return undefined;
            }
        };

        /** END OF BOTTOM BAR **/

        /* Show Result Metadata Modal */
        $scope.showMetadata = function($event, data) {
            function MetadataController($scope, $mdDialog, ftepProperties) {
                $scope.item = data;

                $scope.getQuicklookSrc = function(item){
                    return item.ql;
                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            MetadataController.$inject = ['$scope', '$mdDialog', 'ftepProperties'];
            if(data.type === 'file') {
                $mdDialog.show({
                    controller: MetadataController,
                    templateUrl: 'views/explorer/templates/metadatafile.tmpl.html',
                    parent: angular.element(document.body),
                    targetEvent: $event,
                    clickOutsideToClose: true,
                    locals: {
                        items: $scope.items
                    }
                });
            }
            else {
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
            }
        };

        /* Share Object Modal */
        $scope.sharedObject = {};
        $scope.shareObjectDialog = function($event, item, type, serviceName, serviceMethod) {
            function ShareObjectController($scope, $mdDialog, GroupService, CommunityService) {

                var service = $injector.get(serviceName);
                $scope.permissions = CommunityService.permissionTypes;
                $scope.ace = item;
                $scope.ace.type = type;
                $scope.ace.permission = $scope.permissions.READ;
                $scope.groups = [];

                GroupService.getGroups().then(function(data){
                    $scope.groups = data;
                });

                $scope.shareObject = function (item) {
                    CommunityService.getObjectGroups(item, type).then(function(groups){

                        CommunityService.shareObject($scope.ace, groups).then(function (data) {
                            service[serviceMethod]('explorer');
                        });
                    });

                    $mdDialog.hide();
                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            ShareObjectController.$inject = ['$scope', '$mdDialog', 'GroupService', 'CommunityService'];
            $mdDialog.show({
                controller: ShareObjectController,
                templateUrl: 'views/common/templates/shareitem.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {}
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
