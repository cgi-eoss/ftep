'use strict';

/**
 * @ngdoc function
 * @name ftepApp.controller:SearchbarCtrl
 * @description
 * # SearchbarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function(ftepmodules) {

    ftepmodules.controller('SearchbarCtrl', ['$scope', '$rootScope', '$http', 'CommonService', 'BasketService', 'MapService', 'SearchService', 'moment', function($scope, $rootScope, $http, CommonService, BasketService, MapService, SearchService, moment) {

        $scope.searchParams = SearchService.params;
        $scope.mapAoi = MapService.aoi;
        $scope.allowedValues = {};

        SearchService.getSearchParameters().then(function(data) {
            $scope.catalogues = data;
        });

        $scope.getCatalogIcon = function(catalog) {
            if (catalog === 'SATELLITE') {
                return 'satellite';
            } else if (catalog === 'REF_DATA') {
                return 'local_library';
            } else {
                return 'streetview';
            }
        };

        $scope.setDefaultValue = function(field, index) {
            if (field.defaultValue) {
                if (field.type === 'text' || field.type === 'int' || field.type === 'polygon') {
                    $scope.searchParams.savedSearch[index] = field.defaultValue;
                } else if (field.type === 'select') {
                    for (var item in field.allowed.values) {
                        if (field.defaultValue === field.allowed.values[item].value) {
                            $scope.searchParams.savedSearch[index] = field.allowed.values[item].value;
                        }
                    }
                } else if (field.type === 'daterange') {
                    /* Daterange requires an additional check as start/end are stored in their own props  */
                    if(!$scope.searchParams.savedSearch[index + 'Start']) {
                        var startPeriod = new Date();
                        startPeriod.setMonth(startPeriod.getMonth() + parseInt(field.defaultValue[0]));
                        $scope.searchParams.savedSearch[index + 'Start'] = startPeriod;
                    }
                    if(!$scope.searchParams.savedSearch[index + 'End']) {
                        var endPeriod = new Date();
                        endPeriod.setMonth(endPeriod.getMonth() + parseInt(field.defaultValue[1]));
                        $scope.searchParams.savedSearch[index + 'End'] = endPeriod;
                    }
                }
            }
        };

        $scope.selectCatalog = function(field, catalog) {
            $scope.searchParams.savedSearch[field.type] = catalog.value;
            $scope.searchParams.selectedCatalog = catalog;
        };

        $scope.closeCatalog = function(field) {
            $scope.searchParams.selectedCatalog = {};
        };

        /* If field has no dependencies display it.
         * If field has dependencies, for each find the matching field.
         * Check if any of the condition values matche the current dependency value */
        $scope.displayField = function(field, type) {
            if (field.type === type) {
                if (!field.onlyIf) {
                    return true;
                } else {
                    for (var condition in field.onlyIf) {
                        for (var item in $scope.searchParams.savedSearch) {
                            if (item === condition) {
                                for (var value in field.onlyIf[condition]) {
                                    if (field.onlyIf[condition][value] === $scope.searchParams.savedSearch[item]) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return false;
        };

        $scope.getValues = function() {
            for (var field in $scope.catalogues) {

                // Remove values for fields no longer displayed
                if (!$scope.displayField($scope.catalogues[field], $scope.catalogues[field].type)) {
                    delete $scope.searchParams.savedSearch[field];
                }

                if ($scope.catalogues[field].type === 'select') {
                    // Get list of allowed values
                    var allowedValues = getAllowedFields($scope.catalogues[field]);
                    $scope.allowedValues[$scope.catalogues[field].title] = allowedValues;
                    // Clear any fields set with an invalid value
                    removeInvalidValues(field, allowedValues);
                }
            }
        };

        $scope.$watch('mapAoi.wkt', function(wkt) {
            $scope.searchParams.savedSearch.aoi = wkt;
        });

        /* For all values*/
        function getAllowedFields(field) {
            var displayValues = [];
            var allFieldValues = field.allowed.values;

            for (var value in allFieldValues) {
                // If value is not dependant on another add to list
                if (!allFieldValues[value].onlyIf) {
                    displayValues.push(allFieldValues[value]);
                    // If value depends on anothers
                } else {
                    for (var depField in allFieldValues[value].onlyIf) {
                        var allowedValues = allFieldValues[value].onlyIf[depField];
                        for (var item in allowedValues) {
                            if ($scope.searchParams.savedSearch[depField]) {
                                if ($scope.searchParams.savedSearch[depField] === allowedValues[item]) {
                                    displayValues.push(allFieldValues[value]);
                                }
                            }
                        }
                    }
                }
            }
            return displayValues;
        }

        function removeInvalidValues(field, allowedValues) {
            if ($scope.catalogues[field]) {
                var match = false;
                for (var v in allowedValues) {
                    if ($scope.searchParams.savedSearch[field]) {
                        if ($scope.searchParams.savedSearch[field] === allowedValues[v].value) {
                            match = true;
                        }
                    }
                }
                if (!match) {
                    delete $scope.searchParams.savedSearch[field];
                }
            }
        }

        $scope.search = function() {
            SearchService.submit($scope.searchParams.savedSearch).then(function(searchResults) {
                $rootScope.$broadcast('update.geoResults', searchResults);
            }).catch(function() {
                $rootScope.$broadcast('update.geoResults');
            });
        };
    }]);
});
