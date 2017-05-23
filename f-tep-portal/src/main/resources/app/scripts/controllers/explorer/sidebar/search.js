'use strict';

/**
 * @ngdoc function
 * @name ftepApp.controller:SearchbarCtrl
 * @description
 * # SearchbarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('SearchbarCtrl', ['$scope', '$rootScope', '$http', 'CommonService', 'BasketService', 'GeoService',
                                 function ($scope, $rootScope, $http, CommonService, BasketService, GeoService) {

            /** ----- DATA ----- **/

            $scope.dataSources = GeoService.dataSources;
            $scope.missions = GeoService.missions;
            $scope.polarisations = GeoService.polarisations;

            // Initialise object to store data to send to GeoService
            $scope.searchParameters = GeoService.searchParameters;

            /** ----- DATASOURCES ----- **/

            // Hide datasources and show search form.
            $scope.selectDataSource = function (dataSource) {
                $scope.searchParameters.selectedDatasource = dataSource;
                if(dataSource.fields.mission){
                    // Set the first mission as default
                    $scope.searchParameters.mission = $scope.missions[1];
                }
                $scope.updateMissionParameters($scope.searchParameters.mission);
            };

            $scope.closeDataSource = function () {
                $scope.searchParameters = GeoService.resetSearchParameters();
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
                showCoverage: ($scope.searchParameters.mission &&
                        ['Sentinel-2', 'Landsat-5', 'Landsat-7', 'Landsat-8'].indexOf($scope.searchParameters.mission.name) > -1 ? true : false)
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

    }]);

});
