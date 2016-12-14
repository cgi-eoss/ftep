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
            $scope.selectedValues = GeoService.selectedValues;
            $scope.missions = GeoService.missions;
            $scope.polarisations = GeoService.polarisations;

            // Initialise object to store data to send to GeoService
            $scope.searchParameters = GeoService.parameters;

            /** ----- DATASOURCES ----- **/

            // Set default display value to true
            $scope.showDataSources = true;

            // Hide datasources and show search form.
            $scope.selectDataSource = function (dataSource) {
                $scope.showDataSources = false;
                $scope.selectedValues.datasource = dataSource;
                $scope.$broadcast('rebuild:scrollbar');
            };

            $scope.closeDataSource = function () {
                $scope.showDataSources = true;
                $scope.$broadcast('rebuild:scrollbar');
            };

            /** ----- DATE PICKERS ----- **/

            // Set months for slider.
            var monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"];

            // Set maximum search period range
            var searchPeriod = new Date();
            searchPeriod.setFullYear(searchPeriod.getFullYear() - 10);
            $scope.minDate = searchPeriod;
            $scope.maxDate = new Date();

            // Set default search period range for slider
            var defaultSearchPeriod = new Date();
            defaultSearchPeriod.setMonth(defaultSearchPeriod.getMonth() - 12);
            $scope.startDate = defaultSearchPeriod;
            $scope.endDate = new Date();

            // Set default search period range for inputs
            $scope.searchParameters.startTime = defaultSearchPeriod;
            $scope.searchParameters.endTime = new Date();

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

            // Refresh slider when unhidden
            $scope.refreshSlider = function () {
                $timeout(function () {
                    $scope.$broadcast('rzSliderForceRender');
                });
            };

            // Update input values on slider change
            function onSliderChange() { // jshint ignore:line
                $rootScope.$broadcast('update.timeRange', {
                    start: $scope.timeRangeSlider.minValue,
                    end: $scope.timeRangeSlider.maxValue
                });
            }

            $scope.$on('update.timeRange', function (event, range) {
                if (range.start > range.end) {
                    var end = range.start;
                    range.start = range.end;
                    range.end = end;
                }
                $scope.searchParameters.startTime = new Date(range.start);
                $scope.searchParameters.endTime = new Date(range.end);
            });

            // Update slider values on input change
            $scope.updateSlider = function () {
                $rootScope.$broadcast('update.timeslider', {
                    start: $scope.searchParameters.startTime,
                    end: $scope.searchParameters.endTime
                });
            };

            $scope.$on('update.timeslider', function (event, time) {
                $scope.timeRangeSlider.minValue = time.start.getTime();
                $scope.timeRangeSlider.maxValue = time.end.getTime();
            });

            /** ----- MISSIONS ----- **/

            // Set the first mission as default
            $scope.searchParameters.mission = $scope.missions[0];

            function isSentinel1(mission){
                if(mission && mission.name){
                    return mission.name.indexOf('1') > -1 ? true : false;
                } else {
                    return false;
                }
            }
            function isSentinel2(mission){
                if(mission && mission.name){
                    return mission.name.indexOf('2') > -1 ? true : false;
                } else {
                    return false;
                }
            }

            // Display additional parameters based on mission selection
            $scope.missionDetails = {
                showPolar: isSentinel1($scope.searchParameters.mission),
                showCoverage: isSentinel2($scope.searchParameters.mission)
            };

            $scope.updateMissionParameters = function (mission) {

                // Display polorisation or coverage parameters based on selection
                if (isSentinel1(mission)) {
                    $scope.missionDetails.showPolar = true;
                    $scope.missionDetails.showCoverage = false;
                } else {
                    $scope.missionDetails.showPolar = false;
                    $scope.missionDetails.showCoverage = true;
                    $scope.refreshSlider();
                }

                $scope.$broadcast('rebuild:scrollbar');
            };

            /** ----- POLYGON SELECTION ----- **/

            //TODO: Move polygon.drawn event here from map

            // Set search area to match polygon selection when drawn
            $scope.$on('polygon.drawn', function (event, polygon) {
                if (polygon) {
                    $scope.searchParameters.polygon = polygon;
                } else {
                    delete $scope.searchParameters.polygon;
                }
            });

            /** ----- SEARCH BUTTON ----- **/

            // Send search parameters to GeoService to process
            $scope.search = function () {
                GeoService.getGeoResults().then(function (data) {
                        $rootScope.$broadcast('update.geoResults', data);
                }).catch(function () {
                        GeoService.spinner.loading = false;
                });
            };

    }]);

});
