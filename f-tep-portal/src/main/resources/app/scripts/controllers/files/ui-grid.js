/**
 * @ngdoc function
 * @name ftepApp.controller:UiGridCtrl
 * @description
 * # UiGridCtrl
 * Controller of the ftepApp
 */
"use strict";

define(['../../ftepmodules'], function(ftepmodules) {
    
    ftepmodules.controller('UiGridCtrl', [ '$scope',  'FileService', function($scope, FileService) {

            // Service storage
            $scope.filesParams = FileService.params.files;

            $scope.uiGridOptions = {
                paginationPageSizes: [20, 50, 75],
                paginationPageSize: 50,
                useExternalPagination: true,
                useExternalSorting: true,
                expandableRowTemplate: '../../../views/files/ui-grid-subnav.html',
                expandableRowHeight: 100,
                expandableRowScope: {
                    currentRowData: null,
                    copyToClipboard: copyToClipboard
                },
                onRegisterApi: gridUpdate,
                columnDefs: [
                    { name: "uri", visible: false },
                    { name: "filename" },
                    { name: "owner" },
                    { name: "type"},
                    { name: "filesize"}
                ]
            };

            // API data => table mapping
            function apiMap(response) {
                return {
                    uri: response.uri,
                    filename: response.filename,
                    owner: response.owner ? response.owner.name : "Not set",
                    type: response.type,
                    filesize: response.filesize
                };
            }

            // Initial pagination options
            var paginationOptions = {
                page: 1,
                size: 50,
                sort: null
            };
  
            /* Initial request for data */
            getFtepFilesWithParams();
            
            /* Update files when polling */
            $scope.$on("poll.ftepfiles", function(_, data) {
               // checkForParamsThenGet(data);      // Disabled as resets table 
            });
            
            /* Stop Polling */
            $scope.$on("$destroy", function() {
                FileService.stopPolling();
            });

            // When sidebar form inputs change
            $scope.$on('filesParamsUpdated', function(_, inputParams) {
                getFtepFilesWithParams(inputParams);
            }); 

            // Checks if inputs set then updates data
            function checkForParamsThenGet(data) {
                // If search form inputs filled or no data
                if ($scope.filesParams.params || data == null) {
                    getFtepFilesWithParams($scope.filesParams.params);
                } 
                // Else use basic search
                else {
                    $scope.uiGridOptions.data = mapData(data);
                }
            }

            // Call file search with params
            function getFtepFilesWithParams(inputParams) {
                // Combine with pagination options
                var combinedParameters = Object.assign(inputParams ? inputParams : '', paginationOptions);

                FileService.getFtepFilesWithParams('files', combinedParameters).then(function(inputParams) {
                    var mappedData = mapData(inputParams);
                    $scope.uiGridOptions.data = mappedData;
                    FileService.params.files.files = mappedData;
                });
            }

            // Map API response
            function mapData(data) {
                $scope.uiGridOptions.totalItems = $scope.filesParams.pagingData.page.totalElements;
           
                return data.map(function(thisFile) {
                    return apiMap(thisFile)
                });
            }

            // When pagination controls changed
            function gridUpdate(gridApi) {
                $scope.gridApi = gridApi;

                // On sort change
                $scope.gridApi.core.on.sortChanged($scope, function(grid, sortColumns) {
                    if (sortColumns.length == 0) {
                        paginationOptions.sort = null;
                    } else {
                        paginationOptions.sort = sortColumns[0].sort.direction;
                    }
                    checkForParamsThenGet();
                });

                // On paginate change
                gridApi.pagination.on.paginationChanged($scope, function(newPage, size) {
                    paginationOptions.page = newPage;
                    paginationOptions.size = size;
                    checkForParamsThenGet();
                });

                // On row expand
                gridApi.expandable.on.rowExpandedStateChanged($scope, function (row) {
                    if (row.isExpanded) {
                        $scope.uiGridOptions.expandableRowScope.currentRowData = row.entity;
                    }
                });
            }

            // Copy text to clipboard
            function copyToClipboard(text) {
                var textHolder = document.createElement("textarea");
                document.body.appendChild(textHolder);
                textHolder.value = text;
                textHolder.select();
                document.execCommand("copy");
                document.body.removeChild(textHolder);
            }
        }
    ]);
});
