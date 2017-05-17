/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityDatabasketsCtrl
 * @description
 * # CommunityDatabasketsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityDatabasketsCtrl', ['BasketService', 'CommonService', '$scope', function (BasketService, CommonService, $scope) {

        /* Get stored Databasket details */
        $scope.basketParams = BasketService.params.community;
        $scope.dbOwnershipFilters = BasketService.dbOwnershipFilters;
        $scope.item = "Databasket";

        /* Get Databaskets */
        BasketService.refreshDatabaskets("community");

        /* Update Databaskets when polling */
        $scope.$on('poll.baskets', function (event, data) {
            $scope.basketParams.databaskets = data;
        });

        /* Stop polling */
        $scope.$on("$destroy", function() {
            BasketService.stopPolling();
        });

        $scope.getPage = function(url){
            BasketService.getDatabasketsPage('community', url);
        };

        $scope.filter = function(){
            BasketService.getDatabasketsByFilter('community');
        };

        /* Select a Databasket */
        $scope.selectBasket = function (item) {
            $scope.basketParams.selectedDatabasket = item;
            BasketService.refreshSelectedBasket("community");
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.basketParams.displayFilters = !$scope.basketParams.displayFilters;
        };

        /* Create Databasket */
        $scope.createItemDialog = function ($event) {
            CommonService.createItemDialog($event, 'BasketService', 'createDatabasket').then(function (newBasket) {
                BasketService.refreshDatabaskets("community", "Create");
            });
        };

        /* Edit Databasket */
        $scope.editItemDialog = function ($event, item) {
            CommonService.editItemDialog($event, item, 'BasketService', 'updateDatabasket').then(function (updatedBasket) {
                BasketService.refreshDatabaskets("community");
            });
        };

    }]);
});
