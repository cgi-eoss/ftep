/**
 * @ngdoc function
 * @name ftepApp.controller:ExplorerCtrl
 * @description
 * # ExplorerCtrl
 * Controller of the ftepApp
 */
define(['../../ftepmodules'], function (ftepmodules) {
  'use strict';

  ftepmodules.controller('ExplorerCtrl', function ($scope, $rootScope, $mdDialog) {
    /** BOTTOM BAR **/

    $scope.resultsMenuVisible = false;

    $scope.openResultsMenu = function(){
        $scope.resultsMenuVisible = true;
        $scope.$broadcast('rebuild:scrollbar');
    };

    $scope.toggleResultsMenu = function(){
        $scope.resultsMenuVisible = !$scope.resultsMenuVisible;
        $scope.$broadcast('rebuild:scrollbar');
    };

    /** END OF BOTTOM BAR **/

    /* Warnings */
    $scope.warning = { isVisible: false, message: ''};

    $scope.$on('show.warning', function(event, message) {
        console.log(message);
        $scope.warning.isVisible= true;
        $scope.warning.message= message;

        setTimeout(function(){ clearMessages(); }, 1000);
    });

    function clearMessages(){
        console.log('clean');
        $scope.warning.isVisible= false;
        $scope.warning.message= message;
    }

    /* Create Databasket Modal */
    $scope.newBasket = {name: undefined, description: undefined};
    $scope.createDatabasketDialog = function($event, selectedItems) {
        console.log(selectedItems);
        var parentEl = angular.element(document.body);
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
    };

    /** CREATE PROJECT MODAL **/
    $scope.newProject = {name: undefined, description: undefined};
    $scope.createProjectDialog = function($event) {
        $event.stopPropagation();
        $event.preventDefault();
        var parentEl = angular.element(document.body);
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
    };
    /** END OF CREATE PROJECT MODAL **/

    /* Show Result Metadata Modal */
    $scope.showMetadata = function($event, data) {
        var parentEl = angular.element(document.body);

        if(data.type === 'file'){
            $mdDialog.show({
                parent: parentEl,
                targetEvent: $event,
                clickOutsideToClose: true,
                template:
                  '<md-dialog id="metadata-dialog" aria-label="Metadata dialog">' +
                  '  <h4 class="product-item">{{item.attributes.fname}}</h4>' +
                  '  <a class="item-download" uib-tooltip="Download" tooltip-trigger="mouseenter" tooltip-append-to-body="true" href="{{getLink(item)}}" target="_self">' +
                  '     <span class="glyphicon glyphicon-download-alt"></span>' +
                  '  </a>' +
                  '  <md-dialog-content>' +
                  '    <div class="dialog-content-area">' +
                  '    </div>' +
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
                '  <a class="item-download" uib-tooltip="Download" tooltip-trigger="mouseenter" tooltip-append-to-body="true" href="{{getLink(item)}}" target="_self">' +
                '     <span class="glyphicon glyphicon-download-alt"></span>' +
                '  </a>' +
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
                '  </md-dialog-content>' +
                '</md-dialog>',
              controller: DialogController,
              locals: {
                  item: $scope.item
              }
           });
        }
       function DialogController($scope, $mdDialog, CommonService) {
         $scope.item = data;
         $scope.closeDialog = function() {
             $mdDialog.hide();
         };

         $scope.getLink = function(item){
             return CommonService.getLink(item);
         };
       }
    };
  });
});
