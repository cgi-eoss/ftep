/**
 * @ngdoc function
 * @name ftepApp.controller:ExplorerCtrl
 * @description
 * # ExplorerCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ExplorerCtrl', ['$scope', '$rootScope', '$mdDialog', 'TabService', 'MessageService', '$mdSidenav', '$timeout', 'ftepProperties', '$injector',
                                            function ($scope, $rootScope, $mdDialog, TabService, MessageService, $mdSidenav, $timeout, ftepProperties, $injector) {

        /* Set active page */
        $scope.navInfo = TabService.navInfo;
        $scope.navInfo.sideViewVisible = false;
        $scope.navInfo.activeTab = TabService.getTabs().EXPLORER;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event) {
            $scope.message.count = MessageService.countMessages();
        });

        /** BOTTOM BAR **/
        $scope.displayTab = function(tab, close) {
            if ($scope.navInfo.activeBottomNav === tab && close !== false) {
                $scope.toggleBottomView();
            } else {
                $scope.toggleBottomView(true);
                $scope.navInfo.activeBottomNav = tab;
            }
        };

        $scope.toggleBottomView = function (openBottombar) {
            if (openBottombar === true) {
                 $scope.navInfo.bottomViewVisible = true;
            } else {
                $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
            }
        };

        /** END OF BOTTOM BAR **/

        /* Show Result Metadata Modal */
        $scope.showMetadata = function($event, data) {
            function MetadataController($scope, $mdDialog, ftepProperties) {
                $scope.item = data;

                $scope.getQuicklookSrc = function(item){
                    return '' + ftepProperties.URLv2 + item.ql;
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

    }]);
});
