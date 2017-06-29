/**
 * @ngdoc service
 * @name ftepApp.GeoService
 * @description
 * # GeoService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('GeoService', ['$http', '$rootScope', 'ftepProperties', '$q', 'MessageService', 'MapService', function ($http, $rootScope, ftepProperties, $q, MessageService, MapService) {

        var _this = this;

        /** Set the header defaults **/
        $http.defaults.headers.post['Content-Type'] = 'application/json';
        $http.defaults.withCredentials = true;

        /* private methods-variables */
        var resultCache;
        var ITEMS_PER_PAGE = 20;
        var MAX_ITEMS_ALLOWED = 100000;

        this.getMaxItemsAllowed = function () {
            return MAX_ITEMS_ALLOWED;
        };
        /* End of private methods-variables */

        this.setCache = function (results) {
            if (results && results.length > 0 && results[0].results.totalResults > 0) {
                resultCache = results;
            } else {
                resultCache = {};
            }
        };

        this.getGeoResults = function (pageNumber) {
            this.spinner.loading = true;
            var deferred = $q.defer();

            if (pageNumber) {
                this.pagingData.currentPage = pageNumber;
            } else {
                this.pagingData.currentPage = 1;
            }

            // set times to midnight, and increment end date by one day to make it inclusive
            var start = angular.copy(this.searchParameters.startTime);
            var end = angular.copy(this.searchParameters.endTime);

            start.setHours(0, 0, 0, 0);
            end.setHours(0, 0, 0, 0);
            var userTimezoneOffset = new Date().getTimezoneOffset() * 60000;
            start = new Date(start.getTime() - userTimezoneOffset);
            end = new Date(end.getTime() - userTimezoneOffset);
            end.setDate(end.getDate() + 1);
            ///////////////////////////////

            var params = {
                startDate: start,
                endDate: end,
                startPage: this.pagingData.currentPage,
                maximumRecords: ITEMS_PER_PAGE,
                sat: this.dataSources[0].id === this.searchParameters.selectedDatasource.id,
                tep: this.dataSources[1].id === this.searchParameters.selectedDatasource.id,
                ref: this.dataSources[2].id === this.searchParameters.selectedDatasource.id
            };

            var searchAOI = MapService.getPolygonWkt();
            if (searchAOI) {
                params.geometry = searchAOI;
            }

            this.params.resultsMission = angular.copy(this.searchParameters.mission);
            if (this.searchParameters.mission) {
                if (['Sentinel-1', 'Sentinel-2'].indexOf(this.searchParameters.mission.name) > -1) {
                    params.mission = this.searchParameters.mission.name;
                } else {
                    params.platform = this.searchParameters.mission.name;
                }

                if (this.searchParameters.mission.name === 'Sentinel-1' && this.searchParameters.polarisation) {
                    params.polarisation = this.searchParameters.polarisation.label;
                } else if (['Sentinel-2', 'Landsat-5', 'Landsat-7', 'Landsat-8'].indexOf(this.searchParameters.mission.name) > -1) {
                    params.maxCloudCoverPercentage = this.searchParameters.maxCloudCover;
                }
            }

            if (this.searchParameters.text && this.searchParameters.text != '') {
                params.name = this.searchParameters.text;
            }

            $http({
                method: 'GET',
                url: ftepProperties.URL + '/search',
                params: params,
            }).then(function (response) {
                var searchResults;
                if (!response || !response.data || !response.data.data || !response.data.data[0]) {
                    MessageService.addError('Search failed', 'Search result is empty.');
                    searchResults = [];
                }
                else {
                    if (response.data.data[0].results.totalResults > MAX_ITEMS_ALLOWED) {
                        MessageService.addWarning('Too many results', 'Search results limited to ' +
                            MAX_ITEMS_ALLOWED + '. Please refine the search parameters to get more precise results.');
                    }

                    searchResults = response.data.data;

                    // Convert paging data to APIv2-compatible format
                    var itemsPerPage = searchResults[0].results.itemsPerPage;
                    var totalResultsCount = searchResults[0].results.totalResults;
                    var startIndex = searchResults[0].results.startIndex;
                    var pageNumber = Math.floor(startIndex / itemsPerPage);
                    var totalPages = totalResultsCount - startIndex < itemsPerPage ? Math.floor(totalResultsCount / itemsPerPage) : Math.floor(totalResultsCount / itemsPerPage) + 1;

                    searchResults[0].results.page = {
                        "size": itemsPerPage,
                        "totalElements": totalResultsCount,
                        "totalPages": totalPages,
                        "number": pageNumber
                    };

                    // Transform APIv1 results to APIv2-compatible results
                    for (var i = 0; i < searchResults[0].results.entities.length; i++) {
                        var feature = searchResults[0].results.entities[i];
                        if (feature.usable === undefined) {
                            // Default product usability is allowed
                            feature.usable = true;
                        }
                    }
                }

                _this.setCache(searchResults);
                deferred.resolve(searchResults);
            }).catch(function (error) {
                deferred.reject();
                MessageService.addError('Search failed', error);
            });

            return deferred.promise;
        };

        this.getResultCache = function () {
            return resultCache;
        };

        // Default search time is a year until now
        var defaultStartTime = new Date();
        defaultStartTime.setMonth(defaultStartTime.getMonth() - 12);

        /** PRESERVE USER SELECTIONS **/
        this.searchParameters = {
            selectedDatasource: undefined,
            startTime: defaultStartTime,
            endTime: new Date(),
            text: undefined,
            mission: undefined,
            polarisation: undefined,
            maxCloudCover: 5 //default value of 5% for max cloudiness
        };

        this.resetSearchParameters = function () {
            this.searchParameters = {
                selectedDatasource: undefined,
                startTime: defaultStartTime,
                endTime: new Date(),
                text: undefined,
                mission: undefined,
                polarisation: undefined,
                maxCloudCover: 5
            };
            return this.searchParameters;
        };

        this.pagingData = {
            currentPage: 1,
            pageSize: ITEMS_PER_PAGE,
            total: 0
        };

        this.params = {
            geoResults: [],
            selectedResultItems: [],
            resultsMission: undefined
        };

        /** END OF PRESERVE USER SELECTIONS **/

        this.dataSources = [
            {
                id: 1,
                name: "Satellite",
                icon: "satellite",
                description: "All Satellite data",
                fields: {date: true, mission: true}
            }, {
                id: 2,
                name: "Existing Products",
                icon: "local_library",
                description: "All preprocessed data",
                fields: {name: true}
            }, {
                id: 3,
                name: "Reference",
                icon: "streetview",
                description: "All user uploaded data",
                fields: {date: true, name: true}
            }
        ];

        this.missions = [
            {
                name: "Sentinel-1",
                id: 0
            }, {
                name: "Sentinel-2",
                id: 1
            }, {
                name: "Landsat-5",
                id: 2
            }, {
                name: "Landsat-7",
                id: 3
            }, {
                name: "Landsat-8",
                id: 4
            }
        ];

        this.polarisations = [
            {
                label: "None",
            }, {
                label: "HH",
                id: 0
            }, {
                label: "HV",
                id: 1
            }, {
                label: "VV",
                id: 2
            }, {
                label: "VH",
                id: 3
            }, {
                label: "HH+HV",
                id: 4
            }, {
                label: "VV+VH",
                id: 5
            }
        ];

        this.spinner = {loading: false};

        this.getResultsNameExtention = function () {
            var nameExtention = '';
            if (this.searchParameters.mission) {
                nameExtention = ': ' + this.searchParameters.mission.name;
            }
            else if (this.searchParameters.selectedDatasource) {
                nameExtention = ': ' + this.searchParameters.selectedDatasource.name;
            }
            return nameExtention;
        };

        return this;
    }]);

});
