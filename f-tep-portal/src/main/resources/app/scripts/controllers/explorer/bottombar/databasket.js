/**
 * @ngdoc function
 * @name ftepApp.controller:DatabasketCtrl
 * @description
 * # DatabasketCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('DatabasketCtrl', ['$scope', '$rootScope', '$mdDialog', 'CommonService', 'BasketService', 'TabService',
                                 function ($scope, $rootScope, $mdDialog, CommonService, BasketService, TabService) {

            $scope.dbPaging = BasketService.pagingData;
            $scope.dbParams = BasketService.params.explorer;
            $scope.dbOwnershipFilters = BasketService.dbOwnershipFilters;

            $scope.databaskets = BasketService.getBasketCache().data;

            var collectedFiles = {};

            $scope.fetchDbPage = function (page, noCache) {
                $scope.dbPaging.dbCurrentPage = page;
                BasketService.getDatabaskets(page, $scope.dbPaging.dbPageSize, noCache).then(function (result) {
                    $scope.databaskets = result.data;
                    $scope.dbPaging.dbTotal = result.meta.total;
                    collectFiles(result.included);
                });
            };

            function collectFiles(files) {
                collectedFiles = {};
                if(files){
                    for (var i = 0; i < files.length; i++) {
                        collectedFiles[files[i].id] = files[i];
                    }
                }
            }

            $scope.$on('add.basket', function (event, basket) {
                $scope.fetchDbPage($scope.dbPaging.dbCurrentPage, true);
            });

            $scope.$on('refresh.databaskets', function (event, result) {
                $scope.databaskets = result.data;
                $scope.dbPaging.dbTotal = result.meta.total;
                collectFiles(result.included);
            });

            $scope.removeDatabasket = function (event, basket) {
                CommonService.confirm(event, 'Are you sure you want to delete databasket ' + basket.attributes.name + "?").then(function (confirmed) {
                    if (confirmed === false) {
                        return;
                    }
                    BasketService.removeBasket(basket).then(function (data) {
                        if(angular.equals(basket, $scope.dbParams.selectedDatabasket)){
                            delete $scope.dbParams.selectedDatabasket;
                        }
                        if ($scope.databaskets.length === 0) {
                            $scope.dbPaging.dbCurrentPage = $scope.dbPaging.dbCurrentPage - 1;
                        }
                        $scope.fetchDbPage($scope.dbPaging.dbCurrentPage, true);
                    });
                });
            };

            $scope.toggleFilters = function () {
                $scope.dbParams.displayFilters = !$scope.dbParams.displayFilters;
                $scope.$broadcast('rebuild:scrollbar');
            };

            $scope.databasketSearch = function (item) {
                if (item.attributes.name.toLowerCase().indexOf(
                    $scope.dbParams.searchText.toLowerCase()) > -1) {
                    return true;
                }
                return false;
            };


            /* Show databasket items on map */
            $scope.dbLoaded = {
                id: undefined
            };

            $scope.loadBasket = function (basket) {
                var basketFiles = [];
                $scope.dbLoaded.id = basket.id;
                if (basket.relationships.files && basket.relationships.files.data.length > 0) {
                    for (var i = 0; i < basket.relationships.files.data.length; i++) {
                        var file = collectedFiles[basket.relationships.files.data[i].id];
                        basketFiles.push(file);
                    }
                }
                $rootScope.$broadcast('upload.basket', basketFiles);
            };

            /* Hide databasket items on map */
            $scope.unloadBasket = function (basket) {
                $rootScope.$broadcast('unload.basket');
                $scope.dbLoaded.id = undefined;
            };

            /* Edit existing databasket's name and description */
            $scope.editDatabasketDialog = function($event, selectedBasket) {
                var parentEl = angular.element(document.body);
                $mdDialog.show({
                  parent: parentEl,
                  targetEvent: $event,
                  template:
                    '<md-dialog id="databasket-dialog" aria-label="Edit databasket dialog">' +
                    '    <h4>Edit Databasket</h4>' +
                    '  <md-dialog-content>' +
                    '    <div class="dialog-content-area">' +
                    '        <md-input-container class="md-block" flex-gt-sm>' +
                    '           <label>Name</label>' +
                    '           <input ng-model="editableBasket.attributes.name" type="text"></input>' +
                    '       </md-input-container>' +
                    '       <md-input-container class="md-block" flex-gt-sm>' +
                    '           <label>Description</label>' +
                    '           <textarea ng-model="editableBasket.attributes.description"></textarea>' +
                    '       </md-input-container>' +
                    '    </div>' +
                    '  </md-dialog-content>' +
                    '  <md-dialog-actions>' +
                    '    <md-button ng-click="updateDatabasket(editableBasket)" ng-disabled="!editableBasket.attributes.name" class="md-primary">Save</md-button>' +
                    '    <md-button ng-click="closeDialog()" class="md-primary">Cancel</md-button>' +
                    '  </md-dialog-actions>' +
                    '</md-dialog>',
                  controller: EditController
               });
               function EditController($scope, $mdDialog, BasketService) {
                   $scope.editableBasket = angular.copy(selectedBasket);
                   $scope.closeDialog = function() {
                       $mdDialog.hide();
                   };
                   $scope.updateDatabasket = function (basket) {
                       BasketService.updateBasket(basket).then(function () {
                           $scope.fetchDbPage($scope.dbPaging.dbCurrentPage, true);
                       });
                       $mdDialog.hide();
                   };
               }
            };

            $scope.cloneDatabasket = function (event, basket) {
                BasketService.getItems(basket).then(function (result) {
                    $scope.createDatabasketDialog(event, result.files);
                });
            };

            $scope.getBasketDragItems = function (basket) {
                var files = [];
                if (basket.relationships.files && basket.relationships.files.data.length > 0) {
                    for (var i = 0; i < basket.relationships.files.data.length; i++) {
                        var file = collectedFiles[basket.relationships.files.data[i].id];
                        files.push(file);
                    }
                }
                return files;
            };

            /* Selected Databasket */

            $scope.selectDatabasket = function (basket) {
                BasketService.getItems(basket).then(function (result) {
                    $scope.dbParams.selectedDatabasket = basket;
                    $scope.dbParams.selectedDatabasket.items= result.files;
                });
            };

            $scope.clearDatabasket = function() {
                BasketService.clearBasket($scope.dbParams.selectedDatabasket);
                $scope.dbParams.selectedDatabasket.items = [];
            };

            $scope.removeItemFromBasket = function(item) {
                if(item.name){
                    BasketService.removeRelation($scope.dbParams.selectedDatabasket, item).then(function() {
                        removeFromBasket(item);
                    });
                }
                else{
                    removeFromBasket(item);
                }
            };

            function removeFromBasket(item){
                var i = $scope.dbParams.selectedDatabasket.items.indexOf(item);
                $scope.dbParams.selectedDatabasket.items.splice(i, 1);
            }

            $scope.getBasketItem = function(item){
                if(item.properties){
                    return item.properties.details.file.path;
                }
                return '';
            };
            /* End of Selected Databasket */
    }]);
});
