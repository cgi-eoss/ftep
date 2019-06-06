/**
 * @ngdoc function
 * @name ftepApp.controller:UiGridCtrl
 * @description
 * # UiGridCtrl
 * Controller of the ftepApp
 */
"use strict";

define(['../../ftepmodules'], function(ftepmodules) {

    ftepmodules.controller('UiGridCtrl', [ '$scope',  'FileService', 'UserService', 'CommonService', function($scope, FileService, UserService, CommonService) {

            // Service storage
            $scope.filesParams = FileService.params.files;

            $scope.uiGridOptions = {
                paginationPageSizes: [20, 50, 75, 100, 500],
                paginationPageSize: 50,
                useExternalPagination: true,
                useExternalSorting: true,
                enableFiltering: false,
                enableGridMenu: false,
                enableInfiniteScroll: false,
                enableColumnMenus: false,
                infiniteScrollDown: false,
                infiniteScrollUp: false,
                virtualizationThreshold: 500,
                expandableRowTemplate: 'views/files/ui-grid-subnav.html',
                expandableRowHeight: 500,
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
                    { name: 'owner', width: '300' },
                    { name: 'type', width: '200'},
                    { name: 'filesize', width: '150'}
                ]
            };

            $scope.userInitialised = false;
            var userInitUnbind = null;

            // API data => table mapping
            function apiMap(response) {
                return {
                    id: response.id,
                    uri: response.uri,
                    filename: response.filename,
                    owner: response.owner ? response.owner.name : 'Not set',
                    type: response.type,
                    filesize: (response.filesize / 1073741824).toFixed(3) + ' GB',
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
            if (typeof UserService.params.activeUser === 'object') {
                $scope.userInitialised = true;
                getFtepFilesWithParams($scope.filesParams.params);
            } else {
                // no current user in the app - defer the API call until there is one
                userInitUnbind = $scope.$on('active.user', function() {
                    $scope.userInitialised = true;
                    getFtepFilesWithParams($scope.filesParams.params);
                });
            }

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
                if (!$scope.userInitialised) {
                    // no point continuing if the current user is not available
                    return;
                } else {
                    // stop waiting for the 'active.user' broadcast; we don't need to hit it every time
                    if (userInitUnbind) userInitUnbind();
                }

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
                    if (sortColumns.length === 0) {
                        paginationOptions.sort = null;
                    } else {
                        paginationOptions.sort = sortColumns[0].colDef.name  + ',' + sortColumns[0].sort.direction;
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

            // Download file
            function saveFile(file) {
                file.metadata.geometry = JSON.parse(file.metadata.geometry);

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

            // Parse databaskets to string list
            function parseDatabasketsAsList(databaskets) {
                if (!databaskets || databaskets.length === 0) return '(Not within any databaskets)';

                return databaskets.map(function (thisBasket) {
                    return thisBasket.name;
                }).join('\n');
            }

            // Parse groups to string list
            function parseGroupsAsList(groups) {
                if (!groups || groups.length === 0) return '(Not currently sharing)';

                return groups.map(function (thisgroup) {
                    return thisgroup.group.name;
                }).join('\n');
            }
        }
    ]);
});
