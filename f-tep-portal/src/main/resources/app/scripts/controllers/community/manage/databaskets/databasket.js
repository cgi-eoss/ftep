/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityDatabasketCtrl
 * @description
 * # CommunityDatabasketCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityDatabasketCtrl', ['BasketService', 'FileService', 'MessageService', '$rootScope', '$scope', '$mdDialog', function (BasketService, FileService, MessageService, $rootScope, $scope, $mdDialog) {

        /* Get stored Databaskets & Files details */
        $scope.basketParams = BasketService.params.community;
        $scope.item = "File";

        /* Get Databaskets */
        BasketService.getItemsV2().then(function (data) {
            $scope.basketParams.items = data;
        });

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.basketParams.itemDisplayFilters = !$scope.basketParams.itemDisplayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.basketParams.itemSearchText
        };

        $scope.quickSearch = function (item) {
            if (item.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Remove file from databasket */
        $scope.removeItem = function() {
            BasketService.removeItemV2($scope.basketParams.selectedDatabasket).then(function (data) {
                /* Update file list */
                BasketService.getItemsV2().then(function (baskets) {
                    $scope.basketParams.items = baskets;
                    /* Update file count in databaskets view */
                    $scope.basketParams.selectedDatabasket.size = $scope.basketParams.items.length;
                });
            });
        };

        /* Remove all files from databasket */
        $scope.clearDatabasket = function(file) {
            BasketService.clearDatabasketV2($scope.basketParams.selectedDatabasket, $scope.basketParams.items, file).then(function (data) {
                /* Update file list */
                BasketService.getItemsV2().then(function (baskets) {
                    $scope.basketParams.items = baskets;
                    /* Update file count in databaskets view */
                    $scope.basketParams.selectedDatabasket.size = $scope.basketParams.items.length;
                });
            });
        };

        /* Remove file from databasket */
        $scope.addItemsDialog = function($event) {
            function AddItemsController($scope, $mdDialog, BasketService, FileService) {

                $scope.basketParams = BasketService.params.community;
                $scope.datasourceSelected = false;
                $scope.files = [];
                $scope.datasources = [
                    {
                        id: 1,
                        name: "Existing Products",
                        icon: "local_library",
                        description: "All preprocessed data",
                    }, {
                        id: 2,
                        name: "Reference",
                        icon: "streetview",
                        description: "All user uploaded data",
                    }
                ];

                /* Filters */
                $scope.itemSearch = { searchText: "" };

                $scope.quickSearch = function (item) {
                    if (item.name.toLowerCase().indexOf(
                        $scope.itemSearch.searchText.toLowerCase()) > -1) {
                        return true;
                    }
                    return false;
                };

                /* Get file list on datasource selection */
                $scope.selectDatasource = function (datasource) {
                    $scope.selectedDatasource = datasource;
                    $scope.files = [];
                    var fileType;

                    /* Set file type to query */
                    if (datasource.name === "Existing Products") {
                        fileType = 'OUTPUT_PRODUCT';
                    } else if (datasource.name === "Reference") {
                        fileType = 'REFERENCE_DATA';
                    }

                    /* Get correct list of files and filter out existing items */
                    FileService.getFtepFiles(fileType).then(function (data) {
                        $scope.files = data.filter(function (item) {
                            for (var i in $scope.basketParams.items) {
                                if (item.id === $scope.basketParams.items[i].id) {
                                    return false;
                                }
                            }
                            return true;
                        });
                    });

                };

                /* Deselect datasource and clear file list */
                $scope.deselectDatasource = function () {
                    $scope.selectedDatasource = undefined;
                    $scope.submitEnabled = false;
                    $scope.files = [];
                };

                /* Detect file selection to enable the dialog submit button */
                $scope.selectFile = function () {
                    $scope.submitEnabled = false;
                    for (var file in $scope.files) {
                        if ($scope.files[file].isChecked) {
                            $scope.submitEnabled = true;
                        }
                     }
                };

                /* Add selected files to databasket */
                $scope.updateDatabasket = function () {

                    /* Ensure list of files to add is clear */
                    $scope.addedFiles = [];

                    /* Push list of selected files and delete isChecked property */
                    for (var file in $scope.files) {
                        if ($scope.files[file].isChecked) {
                            $scope.addedFiles.push($scope.files[file]);
                        }
                        delete $scope.files[file].isChecked;
                    }

                    /* Disable submit button */
                    $scope.submitEnabled = false;

                    /* Add files to databasket and update selected databasket */
                    BasketService.addItemsV2($scope.basketParams.selectedDatabasket, $scope.addedFiles).then(function (data) {
                        BasketService.getItemsV2().then(function (data) {
                            $scope.basketParams.items = data;
                            /* Update file count in databaskets view */
                            $scope.basketParams.selectedDatabasket.size = $scope.basketParams.items.length;
                        });
                        $mdDialog.hide();
                    });

                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            AddItemsController.$inject = ['$scope', '$mdDialog', 'BasketService', 'FileService'];
            $mdDialog.show({
                controller: AddItemsController,
                templateUrl: 'views/community/manage/databaskets/templates/additems.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
           });
        };

    }]);
});
