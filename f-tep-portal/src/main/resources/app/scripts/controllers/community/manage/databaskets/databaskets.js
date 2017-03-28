/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityDatabasketsCtrl
 * @description
 * # CommunityDatabasketsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityDatabasketsCtrl', ['BasketService', 'MessageService', '$rootScope', '$scope', '$mdDialog', '$sce', function (BasketService, MessageService, $rootScope, $scope, $mdDialog, $sce) {

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
            function CreateDatabasketController($scope, $mdDialog, BasketService) {

                $scope.item = "Databasket";

                $scope.basketParams = BasketService.params.community;

                $scope.addItem = function () {
                    BasketService.createDatabasketV2($scope.newItem.name, $scope.newItem.description).then(function (createdItem) {
                        /* Update the databasket list */
                        BasketService.getDatabasketsV2().then(function (baskets) {
                            $scope.basketParams.databaskets = baskets;
                            /* Set created databasket to the active databasket */
                            $scope.basketParams.selectedDatabasket = baskets[baskets.length-1];
                        });
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };

            }
            CreateDatabasketController.$inject = ['$scope', '$mdDialog', 'BasketService'];
            $mdDialog.show({
                controller: CreateDatabasketController,
                templateUrl: 'views/community/manage/common/templates/createitem.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
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
                function EditDatabasketController($scope, $mdDialog, BasketService) {

                /* Get Databaskets and User details */
                $scope.basketParams = BasketService.params.community;

                /* Save temporary changes */
                $scope.tempItem = angular.copy(item);
                $scope.itemName = item.name;

                /* Patch databasket and update databasket list */
                $scope.updateItem = function () {
                    BasketService.updateDatabasketV2($scope.tempItem).then(function (data) {
                        BasketService.getDatabasketsV2().then(function (data) {
                            $scope.basketParams.databaskets = data;
                            /* If the modified item is currently selected then update it */
                            if ($scope.basketParams.selectedDatabasket.id === $scope.tempItem.id) {
                                $scope.basketParams.selectedDatabasket = $scope.tempItem;
                            }
                        });
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };

            }
            EditDatabasketController.$inject = ['$scope', '$mdDialog', 'BasketService'];
            $mdDialog.show({
                controller: EditDatabasketController,
                templateUrl: 'views/community/manage/common/templates/edititem.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

    }]);
});
