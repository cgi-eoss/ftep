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
            $scope.selectedSource = GeoService.selectedSource;
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
                $scope.selectedSource = dataSource;
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

            // Set initial selected missions to none
            $scope.selected = [];
            $scope.searchParameters.mission = $scope.selected;
            $scope.default_mission = "Sentinel-1A";

            // Set for displaying error message when no missions are selected
            $scope.missionNotSelected = true;

            // Toggle checkbox when selected
            $scope.toggle = function (item, list) {
                var idx = list.indexOf(item);
                if (idx > -1) {
                    list.splice(idx, 1);
                } else {
                    list.push(item);
                }
            };

            // Sets the check attribute
            $scope.exists = function (item, list) {
                return list.indexOf(item) > -1;
            };

            // Toggle all checkboxes when selected
            $scope.toggleAll = function () {
                if ($scope.selected.length === $scope.missions.length) {
                    $scope.selected = [];
                } else {
                    $scope.selected = $scope.missions.slice(0);
                }
                $scope.searchParameters.mission = $scope.selected;
            };

            // Set select all value when not all or none
            $scope.isIndeterminate = function () {
                return ($scope.selected.length !== 0 && $scope.selected.length !== $scope.missions.length);
            };

            // Detect if all are selected to display correct message
            $scope.isChecked = function () {
                return $scope.selected.length === $scope.missions.length;
            };

            // Display additional parameters based on mission selection
            $scope.updateMissionParameters = function () {

                // Set for when to display further parameters
                var polarValid = true;
                var coverageValid = true;

                // If no missions are selected display error
                if ($scope.selected.length > 0) {
                    $scope.missionNotSelected = false;
                } else {
                    $scope.missionNotSelected = true;
                }

                // Detect if all missions contain a 1 or 2
                $scope.searchParameters.mission.forEach(function (mission) {
                    if (mission.name.indexOf('1') === -1) {
                        polarValid = false;
                    } else if (mission.name.indexOf('2') === -1) {
                        coverageValid = false;
                    }
                });

                // Display polorisation or coverage parameters based on selection
                if (polarValid && $scope.searchParameters.mission.length > 0) {
                    $scope.showPolar = true;
                } else if (coverageValid && $scope.searchParameters.mission.length > 0) {
                    $scope.showCoverage = true;
                    $scope.refreshSlider();
                } else {
                    $scope.showPolar = false;
                    $scope.showCoverage = false;
                }

                $scope.$broadcast('rebuild:scrollbar');
            };

            /** ----- COVERAGE ----- **/

            // Set initial cloud range
            $scope.searchParameters.mission.minCloudCover = 0;
            $scope.searchParameters.mission.maxCloudCover = 100;

            // Initialise cloud slider
            $scope.cloudRangeSlider = {
                options: {
                    floor: 0,
                    ceil: 100,
                    hideLimitLabels: true,
                    translate: function (value) {
                        return value + '%';
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
                minValue: $scope.searchParameters.mission.minCloudCover,
                maxValue: $scope.searchParameters.mission.maxCloudCover
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
