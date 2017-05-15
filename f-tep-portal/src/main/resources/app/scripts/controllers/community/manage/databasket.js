/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageDatabasketCtrl
 * @description
 * # CommunityManageDatabasketCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageDatabasketCtrl', ['BasketService', 'FileService', '$scope', '$mdDialog', function (BasketService, FileService, $scope, $mdDialog) {

        /* Get stored Databaskets & Files details */
        $scope.basketParams = BasketService.params.community;
        $scope.item = "File";

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.basketParams.itemDisplayFilters = !$scope.basketParams.itemDisplayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.basketParams.itemSearchText
        };

        $scope.quickSearch = function (item) {
            if (item.filename.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshDatabasket = function() {
            BasketService.refreshSelectedBasket('community');
        };

        /* Remove file from databasket */
        $scope.removeDatabasketItem = function(files, file) {
            BasketService.removeDatabasketItem($scope.basketParams.selectedDatabasket, files, file).then(function (data) {
                BasketService.refreshDatabaskets("community");
            });
        };

        /* Remove all files from databasket */
        $scope.clearDatabasket = function() {
            BasketService.clearDatabasket($scope.basketParams.selectedDatabasket).then(function (data) {
                BasketService.refreshDatabaskets("community");
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
                    if (item.filename.toLowerCase().indexOf(
                        $scope.itemSearch.searchText.toLowerCase()) > -1) {
                        return true;
                    }
                    return false;
                };

                /* Get file list on datasource selection */
                $scope.selectDatasource = function (datasource) {
                    $scope.selectedDatasource = datasource;
                    $scope.files = [];

                    /* Set file type to query */
                    if (datasource.name === "Existing Products") {
                        FileService.params.community.activeFileType = 'OUTPUT_PRODUCT';
                    } else if (datasource.name === "Reference") {
                        FileService.params.community.activeFileType = 'REFERENCE_DATA';
                    }

                    /* Get correct list of files and filter out existing items */
                    FileService.getFtepFiles(FileService.params.community.activeFileType).then(function (data) {
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
                    var addedFiles = [];

                    /* Push list of selected files and delete isChecked property */
                    for (var file in $scope.files) {
                        if ($scope.files[file].isChecked) {
                            addedFiles.push($scope.files[file]._links.self.href);
                        }
                        delete $scope.files[file].isChecked;
                    }

                    /* Disable submit button */
                    $scope.submitEnabled = false;

                    /* Add files to databasket and update selected databasket */
                    BasketService.addItems($scope.basketParams.selectedDatabasket, addedFiles).then(function (data) {
                        BasketService.refreshDatabaskets("community");
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
