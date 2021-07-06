/**
 * @ngdoc service
 * @name ftepApp.SearchService
 * @description
 * # SearchService
 * Service in the ftepApp.
 */
define(['../ftepmodules', 'traversonHal', 'moment'], function (ftepmodules, TraversonJsonHalAdapter, moment) {

    'use strict';

    ftepmodules.service('SearchService', ['ftepProperties', '$http', '$q', 'MessageService', 'traverson', function (ftepProperties, $http, $q, MessageService, traverson) {

        var _this = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();

        this.spinner = { loading: false };

        this.params = {
            explorer: {
                savedSearch: {}
            },
            pagingData: {},
            results: {}
        };

        /* Get Groups for share functionality to fill the combobox */
        this.getSearchParameters = function() {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/search/parameters')
                .newRequest()
                .getResource()
                .result
                .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Search Data', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        /* Get search name to display in the bottombar tab */
        this.getSearchName = function() {
            var searchName = '';
            if (this.params.explorer.savedSearch.activeSearch.mission) {
                searchName += ': ' + this.params.explorer.savedSearch.activeSearch.mission;
                if (this.params.explorer.savedSearch.activeSearch.mission == 'sentinel1' && this.params.explorer.savedSearch.activeSearch.s1ProcessingLevel) {
                    searchName += ' L' + this.params.explorer.savedSearch.activeSearch.s1ProcessingLevel;
                }
                else if (this.params.explorer.savedSearch.activeSearch.mission == 'sentinel2' && this.params.explorer.savedSearch.activeSearch.s2ProcessingLevel) {
                    searchName += ' L' + this.params.explorer.savedSearch.activeSearch.s2ProcessingLevel;
                }
                else if (this.params.explorer.savedSearch.activeSearch.mission == 'sentinel3' && this.params.explorer.savedSearch.activeSearch.s3ProcessingLevel) {
                    searchName += ' L' + this.params.explorer.savedSearch.activeSearch.s3ProcessingLevel;
                }
                else if (this.params.explorer.savedSearch.activeSearch.mission == 'landsat' && this.params.explorer.savedSearch.activeSearch.landsatProcessingLevel) {
                    searchName += ' L' + this.params.explorer.savedSearch.activeSearch.landsatProcessingLevel;
                }
            }
            return searchName;
        };

        /* Get results by page */
        this.getResultsPage = function (url) {
            this.spinner.loading = true;
            var deferred = $q.defer();

            halAPI.from(url)
                .newRequest()
                .getResource()
                .result
                .then(
                function (response) {
                    _this.spinner.loading = false;
                    deferred.resolve(response);
                }, function (error) {
                    _this.spinner.loading = false;
                    MessageService.addError('Search failed', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        /* Submit search and get results */
        this.submit = function(searchParameters){
            this.spinner.loading = true;
            var deferred = $q.defer();

            halAPI.from(rootUri + '/search')
                .newRequest()
                .withRequestOptions({ qs: searchParameters })
                .getResource()
                .result
                .then(
                function (response) {
                    _this.spinner.loading = false;
                    deferred.resolve(response);
                }, function (error) {
                    _this.spinner.loading = false;
                    MessageService.addError('Search failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        /* Determine if a field should be displayed based on current search selections */
        function displayField(search, field) {
            if (!field.onlyIf) {
                return true;
            }
            for (var cond in field.onlyIf) {
                if (Object.keys(search).includes(cond) && field.onlyIf[cond].includes(search[cond])) {
                    return true;
                }
            }
            return false;
        }

        /* Get a list of all visible fields with all their properties from the catalogue */
        this.getVisibleFields = function(catalogues, search) {
            var visibleFields = [];
            for (var field in catalogues) {
                if (displayField(search, catalogues[field])) {
                    visibleFields.push(catalogues[field]);
                }
            }
            return visibleFields;
        };

        /* Set the default value for an individual form element */
        function setDefaultValue(field, dateType) {
            // Set default value for date range types
            if (dateType) {
                var date = new Date();
                if(dateType === 'start') {
                    date.setUTCMonth(date.getUTCMonth() + parseInt(field.defaultValue[0]));
                } else if (dateType === 'end' ) {
                    date.setUTCMonth(date.getUTCMonth() + parseInt(field.defaultValue[1]));
                }
                return date;
            // Set default value for select types
            } else if (field.defaultValue && field.type === 'select') {
                for (var item in field.allowed.values) {
                    if (field.defaultValue === field.allowed.values[item].value) {
                        return field.allowed.values[item].value;
                    }
                }
            // Set default value for of types if they have a default value
            } else if (field.defaultValue) {
                return field.defaultValue;
            } else {
                return null;
            }
        }

        /* Set the default value for each unset field value in the current search */
        this.getDefaultValues = function(catalogues, activeSearch, visibleFields, visibleList) {

            var search = angular.copy(activeSearch);

            for (var visField in visibleList) {
                var field = catalogues[visibleFields[visField].id];
                var key = visibleList[visField];

                if (key !== 'catalogue' && !search[key]) {
                    if (field.type === 'daterange') {
                        search[key] = {
                            start: setDefaultValue(field, 'start'),
                            end: setDefaultValue(field, 'end')
                        };
                    } else if (field.type === 'int' && search[key] === 0) {
                        // Don't overwrite 0 in int fields
                    } else {
                        search[key] = setDefaultValue(field);
                    }
                }
            }
            return search;
        };

        /* Removes any items in the search that are no longer visible */
        this.searchCleanup = function(activeSearch, visibleList) {
            var search = angular.copy(activeSearch);
            for (var item in search) {
                if (!visibleList.includes(item) && item !== 'catalogue') {
                    delete search[item];
                }
            }
            return search;
        };

        /* Gets all allowed values for a select form field based on other form selections */
        function getAllowedFields(search, field) {
            var displayValues = [];
            var vals = field.allowed.values;
            for (var val in vals) {
                // If value is not dependant on another add to list
                if (!vals[val].onlyIf) {
                    displayValues.push(vals[val]);
                } else {
                    // For each dependency in the allowed values list
                    for (var dep in vals[val].onlyIf) {
                        // If the dependency is active in the search add it
                        if (vals[val].onlyIf[dep].includes(search[dep])) {
                            displayValues.push(vals[val]);
                        }
                    }
                }
            }
            return displayValues;
        }

        /* Gets a list of valid values for every select input in a form */
        this.getSelectValues = function(search, visibleFields) {
            var values = {};
            for (var field in visibleFields) {
                if (visibleFields.hasOwnProperty(field) && visibleFields[field].type === 'select') {
                    values[visibleFields[field].title] = getAllowedFields(search, visibleFields[field]);
                }
            }
            return values;
        };

        function formatDateRanges(date, dateType) {
            if(dateType === 'start') {
                return moment(date).format('YYYY-MM-DD') + 'T00:00:00.000Z';
            } else if (dateType === 'end' ) {
                return moment(date).format('YYYY-MM-DD') + 'T23:59:59.999Z';
            }
        }

        this.formatSearchRequest = function(activeSearch) {
            var search = angular.copy(activeSearch);
            for (var key in search) {
                // Format date ranges correctly
                if (search[key] && search[key].start && search[key].end) {
                    search[key + 'Start'] = formatDateRanges(search[key].start, 'start');
                    search[key + 'End'] = formatDateRanges(search[key].end, 'end');
                    delete search[key];
                }
            }
            return search;
        };

        return this;
    }]);
});
