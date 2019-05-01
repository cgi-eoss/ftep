/**
 * @ngdoc function
 * @name ftepApp.controller:UiGridCtrl
 * @description
 * # UiGridCtrl
 * Controller of the ftepApp
 */
"use strict";

define(['../../ftepmodules'], function(ftepmodules) {
    
    ftepmodules.controller('UiGridCtrl', [ '$scope',  'FileService', 'CommonService', function($scope, FileService, CommonService) {

            // Service storage
            $scope.filesParams = FileService.params.files;

            $scope.uiGridOptions = {
                paginationPageSizes: [20, 50, 75],
                paginationPageSize: 50,
                useExternalPagination: true,
                useExternalSorting: true,
                expandableRowTemplate: '../../../views/files/ui-grid-subnav.html',
                expandableRowHeight: 420,
                expandableRowScope: {
                    currentRowData: {},
                    copyToClipboard: copyToClipboard,
                    saveFile: saveFile,
                    parseDatabasketsAsList: parseDatabasketsAsList,
                    parseGroupsAsList: parseGroupsAsList,
                    share: CommonService.shareObjectDialog,
                    estimateDownloadCost: CommonService.estimateDownloadCost
                },
                onRegisterApi: gridUpdate,
                columnDefs: [
                    { name: 'id', visible: false },
                    { name: 'uri', visible: false },
                    { name: 'filename' },
                    { name: 'owner' },
                    { name: 'type'},
                    { name: 'filesize'},
                    { name: 'inDatabasket'},
                ]
            };

            // API data => table mapping
            function apiMap(response) {
                return {
                    id: response.id,
                    uri: response.uri,
                    filename: response.filename.substring(response.filename.indexOf("/") + 1),
                    owner: response.owner ? response.owner.name : 'Not set',
                    type: response.type,
                    filesize: (response.filesize / 1073741824).toFixed(2) + ' GB',
                    inDatabasket: response.inDatabasket
                };
            }

            // Initial pagination options
            var paginationOptions = {
                page: 0,
                size: 50,
                sort: null
            };

            /* Initial request for data */
            setTimeout(function() { getFtepFilesWithParams($scope.filesParams.params); }, 300);

            /* Update files when polling */
            $scope.$on('poll.ftepfiles', function(_, data) {
               // checkForParamsThenGet(data);      // Disabled as resets table 
            });

            function getDetailedFile(row) {
                FileService.getFile(row.entity, 'detailedFtepFileWorkspace').then(function (file) {
                    if (row.isExpanded) {
                        $scope.uiGridOptions.expandableRowScope.currentRowData[row.entity.id] = file;
                        $scope.uiGridOptions.expandableRowScope.currentRowData[row.entity.id].geometry = JSON.stringify(file.geometry);
                    }
                });
            }

            $scope.$on('group.updated', function(_, row) {
                getDetailedFile(row);
            });
            
            /* Stop Polling */
            $scope.$on('$destroy', function() {
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
                    return apiMap(thisFile);
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
                    paginationOptions.page = newPage -1;
                    paginationOptions.size = size;
                    checkForParamsThenGet();
                });

                // On row expand
                gridApi.expandable.on.rowExpandedStateChanged($scope, function (row) {
                    getDetailedFile(row);
                });
            }

            function saveFile(file) {
                file.metadata.geometry = JSON.parse(file.metadata.geometry);
                console.log(file);
                FileService.updateFtepFile(file).then(function (data) {
                    getFtepFilesWithParams($scope.filesParams.params);
                });
            }

            // Copy text to clipboard
            function copyToClipboard(text) {
                var textHolder = document.createElement('textarea');
                document.body.appendChild(textHolder);
                textHolder.value = text;
                textHolder.select();
                document.execCommand('copy');
                document.body.removeChild(textHolder);
            }

            function parseDatabasketsAsList(databaskets) {
                var list = [];
                for (var basket in databaskets) {
                    list.push(databaskets[basket].name);
                }
                return list.toString();
            }

            function parseGroupsAsList(groups) {
                var list = [];
                for (var group in groups) {
                    list.push(groups[group].group.name + ' [' + groups[group].permission + ']');
                }
                return list.toString();
            }

        }
    ]);
});
