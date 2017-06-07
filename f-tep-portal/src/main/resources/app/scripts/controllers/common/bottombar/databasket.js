/**
 * @ngdoc function
 * @name ftepApp.controller:DatabasketCtrl
 * @description
 * # DatabasketCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('DatabasketCtrl', ['$scope', '$rootScope', '$mdDialog', 'CommonService', 'BasketService', 'TabService', function ($scope, $rootScope, $mdDialog, CommonService, BasketService, TabService) {

        $scope.dbPaging = BasketService.pagingData;
        $scope.dbParams = BasketService.params.explorer;
        $scope.dbOwnershipFilters = BasketService.dbOwnershipFilters;

        /* Get Databaskets */
        BasketService.refreshDatabaskets("explorer");

        /* Update Databaskets when polling */
        $scope.$on('poll.baskets', function (event, data) {
            $scope.dbParams.databaskets = data;
        });

        /* Stop Polling */
        $scope.$on("$destroy", function() {
            BasketService.stopPolling();
        });

        /* Paging */
        $scope.getPage = function(url){
            BasketService.getDatabasketsPage('explorer', url);
        };

        /* Filtering */
        $scope.filter = function(){
            BasketService.getDatabasketsByFilter('explorer');
        };

        $scope.toggleFilters = function () {
            $scope.dbParams.displayFilters = !$scope.dbParams.displayFilters;
        };

        /* Selecting a Databasket */
        $scope.selectDatabasket = function (basket) {
            $scope.dbParams.selectedDatabasket = basket;
            BasketService.refreshSelectedBasket("explorer");
        };

        $scope.deselectDatabasket  = function () {
            $scope.dbParams.selectedDatabasket = undefined;
        };

        /* Select databasket from bottombar menu */
        $scope.basketSelection = { searchText: ''};
        $scope.showList = true;
        $scope.searchBaskets = function() {
            $scope.showList = false;
            BasketService.searchBaskets($scope.basketSelection.searchText).then(function(baskets){
                $scope.dbParams.selectBasketList = baskets;
                $scope.showList = true;
            });
        };
        $scope.searchBaskets();

        $scope.isItemDisabled = function(basketId){
            return document.getElementById('select_' + basketId).disabled;
        };

        /* Get item for dragging */
        $scope.getBasketItem = function(item){
            if($scope.dbParams.selectedItems.indexOf(item) < 0) {
                 $scope.dbParams.selectedItems.push(item);
            }
            var dragObject = {
                type: 'basketItems',
                selectedItems: $scope.dbParams.selectedItems
            };

            return dragObject;
        };

        /* Get all basket files for dragging */
        $scope.getBasketDragItems = function (basket) {
            var dragObject = {
                type: 'databasket',
                basket: basket
            };
            return dragObject;
        };

        /* Edit existing databasket's name and description */
        $scope.editDatabasket = function($event, basket) {
            CommonService.editItemDialog($event, basket, 'BasketService', 'updateDatabasket').then(function (updatedBasket) {
                BasketService.refreshDatabaskets("explorer");
            });
        };

        /* Remove items from a databasket */
        $scope.clearDatabasket = function (event) {
            CommonService.confirm(event, 'Are you sure you want to clear the databasket?').then(function (confirmed) {
                if (confirmed !== false) {
                    BasketService.clearDatabasket($scope.dbParams.selectedDatabasket).then(function (data) {
                        BasketService.refreshDatabaskets("explorer");
                    });
                }
            });
        };

        $scope.removeDatabasketItem = function(item) {
            BasketService.removeDatabasketItem($scope.dbParams.selectedDatabasket, $scope.dbParams.items, item).then(function (data) {
                BasketService.refreshDatabaskets("explorer");
            });
        };

        /* Clone a databasket */
        $scope.cloneDatabasket = function($event, basket){
            function CloneController($scope, $mdDialog) {

                BasketService.getDatabasketContents(basket).then(function(files){
                    $scope.files = files;
                });

                $scope.cloneBasket= function() {
                    BasketService.createDatabasket($scope.newBasket.name, $scope.newBasket.description).then(function (newBasket) {
                        var fileLinks = [];
                        for (var file in $scope.files) {
                            fileLinks.push($scope.files[file]._links.self.href);
                        }
                        BasketService.addItems(newBasket, fileLinks).then(function(data){
                            BasketService.refreshDatabaskets("explorer");
                        });
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }

            CloneController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: CloneController,
                templateUrl: 'views/explorer/templates/createdatabasket.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        /* Displaying databasket items on map */
        $scope.loadBasket = function (basket) {
            BasketService.getDatabasketContents(basket).then(function(files){
                $rootScope.$broadcast('upload.basket', files);
                $scope.dbParams.databasketOnMap.id = basket.id;
                $scope.dbParams.selectedItems = [];
            });
        };

        $scope.unloadBasket = function (basket) {
            $rootScope.$broadcast('unload.basket');
            $scope.dbParams.databasketOnMap.id = undefined;
            $scope.dbParams.selectedItems = [];
        };

        $scope.getIndexById = function (item) {
            return $scope.dbParams.selectedItems.map(function(el) {
                return el.id;
            }).indexOf(item.id);
        };

        /* Toggle selection of items displayed on the map */
        $scope.toggleSelection = function(item) {
            var index = $scope.getIndexById(item);
            if (index < 0) {
                $scope.dbParams.selectedItems.push(item);
            } else {
                $scope.dbParams.selectedItems.splice(index, 1);
            }
            $rootScope.$broadcast('databasket.item.selected', item, index < 0);
        };

        $scope.$on('map.item.toggled', function(event, items) {
            for(var i = 0;  i < items.length; i++) {
                $scope.toggleSelection(items[i]);
            }
        });

        /* Clear loaded databasket when map is reset */
        $scope.$on('map.cleared', function () {
            $scope.unloadBasket();
            $scope.dbParams.selectedItems = [];
        });

    }]);
});
