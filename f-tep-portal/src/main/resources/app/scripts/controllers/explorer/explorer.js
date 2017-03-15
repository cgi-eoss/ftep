/**
 * @ngdoc function
 * @name ftepApp.controller:ExplorerCtrl
 * @description
 * # ExplorerCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ExplorerCtrl', ['$scope', '$rootScope', '$mdDialog', 'TabService', 'MessageService', '$mdSidenav', '$timeout', function ($scope, $rootScope, $mdDialog, TabService, MessageService, $mdSidenav, $timeout) {

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

        /* Create Databasket Modal */
        $scope.newBasket = {name: undefined, description: undefined};
        $scope.createDatabasketDialog = function($event, selectedItems) {
            function CreateDatabasketController($scope, $mdDialog, BasketService) {
                $scope.items = selectedItems;
                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
                $scope.addBasket = function(items) {
                    BasketService.createDatabasket($scope.newBasket.name, $scope.newBasket.description, items).then(function(data){
                        $rootScope.$broadcast('add.basket', data);
                    });
                    $mdDialog.hide();
                };
            }
            CreateDatabasketController.$inject = ['$scope', '$mdDialog', 'BasketService'];
            $mdDialog.show({
              controller: CreateDatabasketController,
              templateUrl: 'views/explorer/templates/createdatabasket.tmpl.html',
              parent: angular.element(document.body),
              targetEvent: $event,
              clickOutsideToClose: true,
              locals: {
                  items: $scope.items
              }
           });

        };

        /** CREATE PROJECT MODAL **/
        $scope.newProject = {name: undefined, description: undefined};
        $scope.createProjectDialog = function($event) {
            $event.stopPropagation();
            $event.preventDefault();
            function CreateProjectController($scope, $mdDialog, ProjectService) {
                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
                $scope.addProject = function() {
                    ProjectService.createProject($scope.newProject.name, $scope.newProject.description).then(function(data){
                        $rootScope.$broadcast('add.project', data);
                    });
                    $mdDialog.hide();
                };
            }
            CreateProjectController.$inject = ['$scope', '$mdDialog', 'ProjectService'];
            $mdDialog.show({
              controller: CreateProjectController,
              templateUrl: 'views/explorer/templates/createproject.tmpl.html',
              parent: angular.element(document.body),
              targetEvent: $event,
              clickOutsideToClose: true,
              locals: {
                  items: $scope.items
              }
           });
        };
        /** END OF CREATE PROJECT MODAL **/

        /* Show Result Metadata Modal */
        $scope.showMetadata = function($event, data) {
            function MetadataController($scope, $mdDialog, CommonService) {
                $scope.item = data;
                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            MetadataController.$inject = ['$scope', '$mdDialog', 'CommonService'];
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
        $scope.shareObjectDialog = function($event, item) {
            function ShareObjectController($scope, $mdDialog, GroupService) {
                $scope.sharedObject = item;
                $scope.groups = [];
                GroupService.getGroups().then(function(data){
                    $scope.groups = data;
                });
                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
                $scope.shareObject = function(item) {
                    //TODO
                    console.log('sharing: ', item);
                    $mdDialog.hide();
                };
            }
            ShareObjectController.$inject = ['$scope', '$mdDialog', 'GroupService'];
            $mdDialog.show({
                controller: ShareObjectController,
                templateUrl: 'views/explorer/templates/shareobject.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {}
            });
        };

    }]);
});
