'use strict';

/**
 * @ngdoc function
 * @name ftepApp.controller:SearchbarCtrl
 * @description
 * # SearchbarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function(ftepmodules) {

    ftepmodules.controller('SearchbarCtrl', ['$scope', '$rootScope', '$http', 'CommonService', 'BasketService', 'MapService', 'SearchService', '$location', function($scope, $rootScope, $http, CommonService, BasketService, MapService, SearchService, $location) {

        // Get page path to save the search params in the SearchService when changing tabs
        var page = $location.path().replace(/\W/g,'') ? $location.path().replace('/','') : 'explorer';

        // Used for listening for drawn polygons on the map
        $scope.mapAoi = MapService.mapstore.aoi;

        $scope.searchParams = {
            selectedCatalog: {},
            activeSearch: {},
            allowedValues: {},
            catalogues: {},
            visibleList: {},
        };

        // Use a saved search if it exists
        if (SearchService.params[page].savedSearch) {
            $scope.searchParams = SearchService.params[page].savedSearch;
        }

        // Get all search input fields
        SearchService.getSearchParameters().then(function(data) {
            for (var field in data) {
                data[field].id = field;
            }
            $scope.searchParams.catalogues = data;
        });

        $scope.selectCatalog = function(field, catalog) {
            if (!$scope.searchParams.activeSearch) {
                $scope.searchParams.activeSearch = {};
            }
            $scope.searchParams.activeSearch[field.type] = catalog.value;
            $scope.searchParams.selectedCatalog = catalog;
            $scope.updateForm($scope.searchParams.catalogues, $scope.searchParams.activeSearch);
        };

        $scope.closeCatalog = function(field) {
            $scope.searchParams.activeSearch = {};
            $scope.searchParams.selectedCatalog = null;
        };

        $scope.$watch('mapAoi.wkt', function(wkt) {
            if (wkt) {
                if (!$scope.searchParams.activeSearch) {
                    $scope.searchParams.activeSearch = {};
                }
                $scope.searchParams.activeSearch.aoi = wkt;
            }
        });

        $scope.updateForm = function(catalogues, activeSearch) {
            // Get the list of fields from the catalog to display
            var visibleFields = SearchService.getVisibleFields(catalogues, activeSearch);
            $scope.searchParams.visibleList = visibleFields.map(function(a) {return a.id;});
            // Cleanup search params that are no longer visible
            $scope.searchParams.activeSearch = SearchService.searchCleanup(activeSearch, $scope.searchParams.visibleList);
            // Set default values in the active search
            $scope.searchParams.activeSearch = SearchService.getDefaultValues(catalogues, $scope.searchParams.activeSearch, visibleFields, $scope.searchParams.visibleList);
            // Get the list of allowed values for each select option (based on current selections)
            $scope.searchParams.allowedValues = SearchService.getSelectValues(activeSearch, visibleFields);
        };

        $scope.search = function() {
            // Format the query for submission (format date fields correctly)
            var searchQuery = SearchService.formatSearchRequest($scope.searchParams.activeSearch);

            // Save the search so the results controller can access the search name
            SearchService.params[page].savedSearch = $scope.searchParams;

            SearchService.submit(searchQuery).then(function(searchResults) {
                $rootScope.$broadcast('update.geoResults', searchResults);
            }).catch(function() {
                $rootScope.$broadcast('update.geoResults');
            });
        };

        // Save the search when changing tabs so it can be reaccessed
        $scope.$on('$destroy', function() {
            SearchService.params[page].savedSearch = $scope.searchParams;
        });

    }]);
});
