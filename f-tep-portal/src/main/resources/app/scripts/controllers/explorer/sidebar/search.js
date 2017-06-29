'use strict';

/**
 * @ngdoc function
 * @name ftepApp.controller:SearchbarCtrl
 * @description
 * # SearchbarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('SearchbarCtrl', ['$scope', '$rootScope', '$http', 'CommonService', 'BasketService', 'GeoService', 'MapService', 'SearchService', function ($scope, $rootScope, $http, CommonService, BasketService, GeoService, MapService, SearchService) {

        /** ----- DATA ----- **/

        $scope.dataSources = GeoService.dataSources;
        $scope.missions = GeoService.missions;
        $scope.polarisations = GeoService.polarisations;

        // Initialise object to store data to send to GeoService
        $scope.searchParameters = GeoService.searchParameters;
        $scope.searchParametersV2 = {
            repo: undefined,
            resultsPerPage: undefined,
            page: undefined
            // additional arbitrary parameters are permitted based on datasource
        };

        /** ----- DATASOURCES ----- **/

        // Hide datasources and show search form.
        $scope.selectDataSource = function (dataSource) {
            $scope.searchParameters.selectedDatasource = dataSource;

            // new APIv2 search enabled for dataSource 3 & 2 (refData & existing products) only
            if (dataSource.id === 3) {
                $scope.searchParametersV2.repo = 'REF_DATA';
            } else if (dataSource.id === 2) {
                $scope.searchParametersV2.repo = 'FTEP_PRODUCTS';
            } else {
                if (dataSource.fields.mission) {
                    // Set the first mission as default
                    $scope.searchParameters.mission = $scope.missions[1];
                }
            }

            $scope.updateMissionParameters($scope.searchParameters.mission);
        };

        $scope.closeDataSource = function () {
            $scope.searchParameters = GeoService.resetSearchParameters();
            $scope.searchParametersV2 = {};
        };

        /** ----- DATE PICKERS ----- **/

            // Set maximum search period range
        var searchPeriod = new Date();
        searchPeriod.setFullYear(searchPeriod.getFullYear() - 10);
        $scope.minDate = searchPeriod;
        $scope.maxDate = new Date();

        /** ----- MISSIONS ----- **/

        // Display additional parameters based on mission selection
        $scope.missionDetails = {
            showPolar: ($scope.searchParameters.mission && $scope.searchParameters.mission.name === 'Sentinel-1' ? true : false),
            showCoverage: ($scope.searchParameters.mission && ['Sentinel-2', 'Landsat-5', 'Landsat-7', 'Landsat-8'].indexOf($scope.searchParameters.mission.name) > -1 ? true : false)
        };

        $scope.updateMissionParameters = function (mission) {
            // Display polorisation or coverage parameters based on selection
            $scope.missionDetails.showPolar = (mission && mission.name === 'Sentinel-1' ? true : false);
            $scope.missionDetails.showCoverage = (mission && ['Sentinel-2', 'Landsat-5', 'Landsat-7', 'Landsat-8'].indexOf(mission.name) > -1 ? true : false);
        };

        /** ----- SEARCH BUTTON ----- **/

        // Send search parameters to GeoService to process
        $scope.search = function () {
            GeoService.getGeoResults().then(function (data) {
                $rootScope.$broadcast('update.geoResults', data);
            }).catch(function () {
                $rootScope.$broadcast('update.geoResults');
            });
        };

        // Send search parameters to SearchService to process
        $scope.searchV2 = function () {
            var searchAOI = MapService.getPolygonWkt();
            if (searchAOI) {
                $scope.searchParametersV2.geometry = searchAOI;
            } else {
                delete $scope.searchParametersV2.geometry;
            }

            SearchService.submit($scope.searchParametersV2).then(function (searchResults) {
                $rootScope.$broadcast('update.geoResults', searchResults);
            }).catch(function () {
                $rootScope.$broadcast('update.geoResults');
            });
        };

    }]);

});
