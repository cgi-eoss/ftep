/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityDatabasketsCtrl
 * @description
 * # CommunityDatabasketsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityDatabasketsCtrl', ['BasketService', 'CommonService', 'MessageService', '$rootScope', '$scope',
                                                        '$mdDialog', '$sce',
                                  function (BasketService, CommonService, MessageService, $rootScope, $scope, $mdDialog, $sce) {

        /* Get stored Databasket details */
        $scope.basketParams = BasketService.params.community;
        $scope.item = "Databasket";

        /* Get databaskets */
        BasketService.getDatabasketsV2().then(function (data) {
             $scope.basketParams.databaskets = data;
        });

        /* Update databaskets when polling */
        $scope.$on('refresh.databasketsV2', function (event, data) {
            $scope.basketParams.databaskets = data;
        });

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
                /* Update the databasket list */
                BasketService.getDatabasketsV2().then(function (baskets) {
                    $scope.basketParams.databaskets = baskets;
                    /* Set created databasket to the active databasket */
                    $scope.basketParams.selectedDatabasket = baskets[baskets.length-1];
                });
            });
        };

        /* Remove Databasket */
        $scope.removeItem = function (key, item) {
             BasketService.removeDatabasketV2(item).then(function (data) {
                 /* Update list of databaskets*/
                 BasketService.getDatabasketsV2().then(function (baskets) {
                    $scope.basketParams.databaskets = baskets;
                    /* Clear selected databasket and databasket items */
                     if (item.id === $scope.basketParams.selectedDatabasket.id) {
                        $scope.basketParams.selectedDatabasket = undefined;
                        $scope.basketParams.items = [];
                     }

                });
            });
        };

        /* Edit Databasket */
        $scope.editItemDialog = function ($event, item) {
            CommonService.editItemDialog($event, item, 'BasketService', 'updateDatabasketV2').then(function (updatedBasket) {
                /* If the modified item is currently selected then update it */
                if ($scope.basketParams.selectedDatabasket && $scope.basketParams.selectedDatabasket.id === updatedBasket.id) {
                    $scope.basketParams.selectedDatabasket = updatedBasket;
                }
                BasketService.getDatabasketsV2().then(function (data) {
                    $scope.basketParams.databaskets = data;
                });
            });
        };

    }]);
});
