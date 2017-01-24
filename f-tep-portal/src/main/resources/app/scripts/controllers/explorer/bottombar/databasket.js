/**
 * @ngdoc function
 * @name ftepApp.controller:DatabasketCtrl
 * @description
 * # DatabasketCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('DatabasketCtrl', ['$scope', '$rootScope', '$mdDialog', 'CommonService', 'BasketService',
                                 function ($scope, $rootScope, $mdDialog, CommonService, BasketService) {

            $scope.dbPaging = {
                dbCurrentPage: 1,
                dbPageSize: 10,
                dbTotal: 0
            };

            $scope.databaskets = [];

            var collectedFiles = {};

            $scope.fetchDbPage = function (page, noCache) {
                $scope.dbPaging.dbCurrentPage = page;
                BasketService.getDatabaskets(page, $scope.dbPaging.dbPageSize, noCache).then(function (result) {
                    $scope.databaskets = result.data;
                    $scope.dbPaging.dbTotal = result.meta.total;
                    collectFiles(result.included);
                });
            };
            $scope.fetchDbPage($scope.dbPaging.dbCurrentPage);

            function collectFiles(files) {
                collectedFiles = {};
                for (var i = 0; i < files.length; i++) {
                    collectedFiles[files[i].id] = files[i];
                }
            }

            $scope.$on('add.basket', function (event, basket) {
                $scope.fetchDbPage($scope.dbPaging.dbCurrentPage, true);
            });

            $scope.$on('refresh.databaskets', function (event, result) {
                $scope.databaskets = result.data;
                $scope.dbPaging.dbTotal = result.meta.total;
            });

            $scope.removeDatabasket = function (event, basket) {
                CommonService.confirm(event, 'Are you sure you want to delete databasket ' + basket.attributes.name + "?").then(function (confirmed) {
                    if (confirmed === false) {
                        return;
                    }
                    BasketService.removeBasket(basket).then(function (data) {
                        $rootScope.$broadcast('delete.databasket', basket);
                        if ($scope.databaskets.length === 0) {
                            $scope.dbPaging.dbCurrentPage = $scope.dbPaging.dbCurrentPage - 1;
                        }
                        $scope.fetchDbPage(pgNr, true);
                    });
                });
            };

            $scope.displayFilters = false;

            $scope.toggleFilters = function () {
                $scope.displayFilters = !$scope.displayFilters;
                $scope.$broadcast('rebuild:scrollbar');
            };

            $scope.databasketSearch = {
                searchText: ''
            };

            $scope.databasketSearch = function (item) {
                if (item.attributes.name.toLowerCase().indexOf(
                    $scope.databasketSearch.searchText.toLowerCase()) > -1) {
                    return true;
                }
                return false;
            };


            /* Show databasket details */
            $scope.showDatabasket = function (basket) {
                BasketService.getItems(basket).then(function (result) {
                    $rootScope.$broadcast('update.databasket', basket, result.files);
                    var container = document.getElementById('bottombar');
                    container.scrollTop = 0;
                });
            };

            $scope.selectDatabasket = function (basket) {
                BasketService.getItems(basket).then(function (result) {
                    $rootScope.$broadcast('select.databasket', basket, result.files);
                });
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
                var str = "";
                var firstIsDone = false;
                if (basket.relationships.files && basket.relationships.files.data.length > 0) {
                    for (var i = 0; i < basket.relationships.files.data.length; i++) {
                        var file = collectedFiles[basket.relationships.files.data[i].id];
                        if (file.attributes.properties && file.attributes.properties.details.file && str.indexOf(file.attributes.properties.details.file.path) < 0) {
                            str = str.concat(',', file.attributes.properties.details.file.path);
                        }
                    }
                }
                return str.substr(1);
            };
    }]);
});
