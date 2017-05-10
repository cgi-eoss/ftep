/**
 * @ngdoc function
 * @name ftepApp.controller:SearchbarCtrl
 * @description
 * # SearchbarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('SearchbarCtrl', ['$scope', '$rootScope', '$http', 'CommonService', 'BasketService', 'GeoService', 'JobService', 'ProductService', 'ProjectService', '$sce', '$timeout',
                                 function ($scope, $rootScope, $http, CommonService, BasketService, GeoService, JobService, ProductService, ProjectService, $sce, $timeout) {


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
                $scope.updateSlider();
            };

            $scope.closeDataSource = function () {
                $scope.searchParameters = GeoService.resetSearchParameters();
            };

            /** ----- DATE PICKERS ----- **/

            // Set months for slider.
            var monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"];

            // Set maximum search period range
            var searchPeriod = new Date();
            searchPeriod.setFullYear(searchPeriod.getFullYear() - 10);
            $scope.minDate = searchPeriod;
            $scope.maxDate = new Date();

            // Initialise time slider
            $scope.timeRangeSlider = {
                options: {
                    floor: $scope.minDate.getTime(),
                    ceil: $scope.maxDate.getTime(),
                    translate: function (value) {
                        var date = new Date(value);
                        var str = monthNames[date.getMonth()] + ' ' + date.getFullYear();
                        return str;
                    },
                    getSelectionBarColor: function () {
                        return '#ff80ab';
                    },
                    getTickColor: function () {
                        return '#ff80ab';
                    },
                    getPointerColor: function () {
                        return '#ff80ab';
                    },
                    onChange: onSliderChange,
                    showTicks: false
                },
                minValue: $scope.searchParameters.startTime.getTime(),
                maxValue: $scope.searchParameters.endTime.getTime()
            };

            // Update input values on slider change
            function onSliderChange() { // jshint ignore:line
                $scope.searchParameters.startTime = new Date($scope.timeRangeSlider.minValue);
                $scope.searchParameters.endTime = new Date($scope.timeRangeSlider.maxValue);
            }

            // Update slider values on input change
            $scope.updateSlider = function () {
                $scope.timeRangeSlider.minValue = $scope.searchParameters.startTime.getTime();
                $scope.timeRangeSlider.maxValue = $scope.searchParameters.endTime.getTime();
                $timeout(function () {
                    $scope.$broadcast('rzSliderForceRender');
                }, 50);
            };

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
