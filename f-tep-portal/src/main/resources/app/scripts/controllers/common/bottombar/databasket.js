/**
 * @ngdoc function
 * @name ftepApp.controller:DatabasketCtrl
 * @description
 * # DatabasketCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('DatabasketCtrl', ['$scope', '$rootScope', 'CommonService', 'BasketService', 'TabService', function ($scope, $rootScope, CommonService, BasketService, TabService) {

        $scope.dbPaging = BasketService.pagingData;
        $scope.dbParams = BasketService.params.explorer;
        $scope.dbOwnershipFilters = BasketService.dbOwnershipFilters;

        $scope.toggleFilters = function () {
            $scope.dbParams.displayFilters = !$scope.dbParams.displayFilters;
        };

        $scope.databasketSearch = function (basket) {
            if (basket.name.toLowerCase().indexOf(
                $scope.dbParams.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Get Databaskets */
        BasketService.refreshDatabaskets("explorer");

        /* Update Databaskets when polling */
        $scope.$on('poll.baskets', function (event, data) {
            $scope.dbParams.databaskets = data;
        });

        /* Edit existing databasket's name and description */
        $scope.editDatabasket = function($event, basket) {
            CommonService.editItemDialog($event, basket, 'BasketService', 'updateDatabasket').then(function (updatedBasket) {
                BasketService.refreshDatabaskets("explorer");
            });
        };

        $scope.removeDatabasket = function (event, basket) {
            CommonService.confirm(event, 'Are you sure you want to delete databasket ' + basket.name + "?").then(function (confirmed) {
                if (confirmed === false) {
                    return;
                }
                BasketService.removeDatabasket(basket).then(function (data) {
                    BasketService.refreshDatabaskets("explorer", "Remove", basket);
                });
            });
        };

        $scope.loadBasket = function (basket) {
            BasketService.getDatabasketContents(basket).then(function(files){
                $rootScope.$broadcast('upload.basket', files);
            });
        };

        /* Hide databasket items on map */
        $scope.unloadBasket = function (basket) {
            $rootScope.$broadcast('unload.basket');
            $scope.dbParams.databasketOnMap.id = undefined;
        };

        $scope.cloneDatabasket = function (event, basket) {
            CommonService.createItemDialog(event, 'BasketService', 'createDatabasket').then(function (newBasket) {
                BasketService.getDatabasketContents(basket).then(function(files){
                    BasketService.addItems(newBasket, files).then(function(data){
                        BasketService.refreshDatabaskets("explorer");
                    });
                });
            });
        };

        /* Selected Databasket */

        $scope.selectDatabasket = function (basket) {
            $scope.dbParams.selectedDatabasket = basket;
            BasketService.refreshSelectedBasket("explorer");
        };

        $scope.deselectDatabasket  = function () {
            $scope.dbParams.selectedDatabasket = undefined;
        };

        $scope.clearDatabasket = function() {
            BasketService.clearDatabasket($scope.basketParams.selectedDatabasket).then(function (data) {
                BasketService.refreshDatabaskets("explorer");
            });
        };

        $scope.removeItemFromBasket = function(item) {
            BasketService.removeItem($scope.basketParams.selectedDatabasket, $scope.basketParams.items, item).then(function (data) {
                BasketService.refreshDatabaskets("explorer");
            });
        };
        /* End of Selected Databasket */

        /* GET ITEM FOR DARGGING */
        $scope.getBasketItem = function(item){
            var dragObject = {
                    type: 'basketItem',
                    item: item
            };
            return dragObject;
        };

        /* GET ALL BASKET FILES FOR DRAGGING */
        $scope.getBasketDragItems = function (basket) {
            var dragObject = {
                    type: 'databasket',
                    basket: basket
            };
            return dragObject;
        };
    }]);
});
