/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityDatabasketsCtrl
 * @description
 * # CommunityDatabasketsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityDatabasketsCtrl', ['BasketService', 'CommonService', '$scope', '$sce', function (BasketService, CommonService, $scope, $sce) {

        /* Get stored Databasket details */
        $scope.basketParams = BasketService.params.community;
        $scope.item = "Databasket";

        /* Get Databaskets */
        BasketService.refreshDatabasketsV2("Community");

        /* Update Databaskets when polling */
        $scope.$on('poll.baskets', function (event, data) {
            $scope.basketParams.databaskets = data;
        });

        /* Select a Databasket */
        $scope.selectBasket = function (item) {
            $scope.basketParams.selectedDatabasket = item;
            BasketService.refreshSelectedBasketV2("Community");
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.basketParams.displayFilters = !$scope.basketParams.displayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.basketParams.searchText
        };

        $scope.quickSearch = function (item) {
            if (item.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Databasket Description Popup */
        var popover = {};
        $scope.getDescription = function (item) {
            if (!item.description) {
                item.description = "No description.";
            }
            var html =
                '<div class="metadata">' +
                    '<div class="row">' +
                        '<div class="col-sm-12">' + item.description + '</div>' +
                    '</div>' +
                '</div>';
            return popover[html] || (popover[html] = $sce.trustAsHtml(html));
        };

        /* Create Databasket */
        $scope.createItemDialog = function ($event) {
            CommonService.createItemDialog($event, 'BasketService', 'createDatabasketV2').then(function (newBasket) {
                BasketService.refreshDatabasketsV2("Community", "Create");
            });
        };

        /* Remove Databasket */
        $scope.removeItem = function (key, item) {
             BasketService.removeDatabasketV2(item).then(function (data) {
                 BasketService.refreshDatabasketsV2("Community", "Remove", item);
            });
        };

        /* Edit Databasket */
        $scope.editItemDialog = function ($event, item) {
            CommonService.editItemDialog($event, item, 'BasketService', 'updateDatabasketV2').then(function (updatedBasket) {
                BasketService.refreshDatabasketsV2("Community");
            });
        };

    }]);
});
