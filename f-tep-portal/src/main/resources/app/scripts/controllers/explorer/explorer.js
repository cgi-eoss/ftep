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
            var parentEl = angular.element(document.body);
            function DialogController($scope, $mdDialog, BasketService) {
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
            $mdDialog.show({
              parent: parentEl,
              targetEvent: $event,
              template:
                '<md-dialog id="databasket-dialog" aria-label="Create databasket dialog">' +
                '    <h4>Create New Databasket</h4>' +
                '  <md-dialog-content>' +
                '    <div class="dialog-content-area">' +
                '        <md-input-container class="md-block" flex-gt-sm>' +
                '           <label>Name</label>' +
                '           <input ng-model="newBasket.name" type="text"></input>' +
                '       </md-input-container>' +
                '       <md-input-container class="md-block" flex-gt-sm>' +
                '           <label>Description</label>' +
                '           <textarea ng-model="newBasket.description"></textarea>' +
                '       </md-input-container>' +
                '    </div>' +
                '    <ul class="dialog-list">' +
                '        <li ng-repeat="item in items" class="product-item">{{item.identifier}}{{item.title}}{{item.attributes.fname}}{{item.name}}</li>' +
                '    </ul>' +
                '  </md-dialog-content>' +
                '  <md-dialog-actions>' +
                '    <md-button ng-click="addBasket(items)" ng-disabled="!newBasket.name" class="md-primary">Create</md-button>' +
                '    <md-button ng-click="closeDialog()" class="md-primary">Cancel</md-button>' +
                '  </md-dialog-actions>' +
                '</md-dialog>',
              controller: DialogController,
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
            var parentEl = angular.element(document.body);
            function DialogController($scope, $mdDialog, ProjectService) {
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
            $mdDialog.show({
              parent: parentEl,
              targetEvent: $event,
              template:
                '<md-dialog id="project-dialog" aria-label="Create project dialog">' +
                '    <h4>Create New Project</h4>' +
                '  <md-dialog-content>' +
                '    <div class="dialog-content-area">' +
                '        <md-input-container class="md-block" flex-gt-sm>' +
                '           <label>Name</label>' +
                '           <input ng-model="newProject.name" type="text"></input>' +
                '       </md-input-container>' +
                '       <md-input-container class="md-block" flex-gt-sm>' +
                '           <label>Description</label>' +
                '           <textarea ng-model="newProject.description"></textarea>' +
                '       </md-input-container>' +
                '    </div>' +
                '  </md-dialog-content>' +
                '  <md-dialog-actions>' +
                '    <md-button ng-click="addProject()" ng-disabled="!newProject.name" class="md-primary">Create</md-button>' +
                '    <md-button ng-click="closeDialog()" class="md-primary">Cancel</md-button>' +
                '  </md-dialog-actions>' +
                '</md-dialog>',
              controller: DialogController,
              locals: {
                  items: $scope.items
              }
           });
        };
        /** END OF CREATE PROJECT MODAL **/

        /* Show Result Metadata Modal */
        $scope.showMetadata = function($event, data) {
            var parentEl = angular.element(document.body);
            function DialogController($scope, $mdDialog, CommonService) {
                $scope.item = data;
                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            if(data.type === 'file'){
                $mdDialog.show({
                    parent: parentEl,
                    targetEvent: $event,
                    clickOutsideToClose: true,
                    template:
                      '<md-dialog id="metadata-dialog" aria-label="Metadata dialog">' +
                      '  <h4 class="product-item">{{item.attributes.fname}}</h4>' +
                      '  <md-dialog-content>' +
                      '    <div class="dialog-content-area">' +
                      '    </div>' +
                      '  <md-button  aria-label="Download" tooltip-trigger="mouseenter" tooltip-append-to-body="true"' +
                      '              href="{{item.link}}" target="_self">' +
                      '    Download <i class="material-icons">file_download</i>' +
                      '  </md-button>' +
                      '  </md-dialog-content>' +
                      '</md-dialog>',
                    controller: DialogController,
                    locals: {
                        item: $scope.item
                    }
                 });
            }
            else {
                $mdDialog.show({
                  parent: parentEl,
                  targetEvent: $event,
                  clickOutsideToClose: true,
                  template:
                    '<md-dialog id="metadata-dialog" aria-label="Metadata dialog">' +
                    '  <h4>{{item.identifier}}</h4>' +
                    '  <md-dialog-content>' +
                    '    <div class="dialog-content-area">' +
                    '        <div class="row">' +
                    '            <div class="col-md-4">Name</div>' +
                    '            <div class="col-md-8">{{item.identifier}}</div>' +
                    '        </div>' +
                    '        <div class="row">' +
                    '            <div class="col-md-4">Size</div>' +
                    '            <div class="col-md-8">{{item.size | bytesToGB}}</div>' +
                    '        </div>' +
                    '        <div class="row">' +
                    '            <div class="col-md-4">Start</div>' +
                    '            <div class="col-md-8">{{item.start}}</div>' +
                    '        </div>' +
                    '        <div class="row">' +
                    '            <div class="col-md-4">Stop</div>' +
                    '            <div class="col-md-8">{{item.stop}}</div>' +
                    '        </div>' +
                    '    </div>' +
                    '    <md-button  class = "download-button" aria-label="Download" tooltip-trigger="mouseenter"' +
                    '                tooltip-append-to-body="true" href="{{item.link}}" target="_self">' +
                    '      Download <i class="material-icons">file_download</i>' +
                    '    </md-button>' +
                    '  </md-dialog-content>' +
                    '</md-dialog>',
                  controller: DialogController,
                  locals: {
                      item: $scope.item
                  }
               });
            }
        };

        /* Share Object Modal */
        $scope.sharedObject = {};
        $scope.shareObjectDialog = function($event, item) {
            var parentEl = angular.element(document.body);
            function DialogController($scope, $mdDialog, GroupService) {
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
            $mdDialog.show({
                parent: parentEl,
                targetEvent: $event,
                template:
                    '<md-dialog id="share-dialog" aria-label="Share an object dialog">' +
                    '    <h4>Share a {{sharedObject.type | asSingular}}</h4>' +
                    '  <md-dialog-content>' +
                    '    <div class="dialog-content-area">' +
                    '        <div class="row" ng-show="sharedObject.attributes.name != undefined">' +
                    '            <div class="col-md-3">Name:</div>' +
                    '            <div class="col-md-9">{{sharedObject.attributes.name}}</div>' +
                    '        </div>' +
                    '        <div class="row" ng-show="sharedObject.attributes.name == undefined">' +
                    '            <div class="col-md-3">Id:</div>' +
                    '            <div class="col-md-9">{{sharedObject.id}}</div>' +
                    '        </div>' +
                    '        <br/><br/>' +
                    '        <md-input-container class="md-block" flex-gt-sm>' +
                    '            <label>Group</label>' +
                    '            <md-select ng-model="sharedObject.group">' +
                    '                <md-option ng-value="undefined"><em>None</em></md-option>' +
                    '                <md-option ng-repeat="group in groups" ng-value="group">' +
                    '                    {{group.attributes.name}}' +
                    '                </md-option>' +
                    '            </md-select>' +
                    '        </md-input-container>' +
                    '    </div>' +
                    '  </md-dialog-content>' +
                    '  <md-dialog-actions>' +
                    '    <md-button ng-click="shareObject(sharedObject)" ng-disabled="sharedObject.group == undefined" class="md-primary">Share</md-button>' +
                    '    <md-button ng-click="closeDialog()" class="md-primary">Cancel</md-button>' +
                    '  </md-dialog-actions>' +
                    '</md-dialog>',
                controller: DialogController,
                locals: {}
            });
        };

    }]);
});
