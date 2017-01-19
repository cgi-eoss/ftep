/**
 * @ngdoc function
 * @name ftepApp.controller:JobsCtrl
 * @description
 * # JobsCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('JobsCtrl', ['$scope', '$rootScope', 'CommonService', 'JobService', '$sce',
                                 function ($scope, $rootScope, CommonService, JobService, $sce) {

            $scope.jobStatuses = [
                {
                    name: "Succeeded",
                    value: true
                }, {
                    name: "Failed",
                    value: true
                }, {
                    name: "Running",
                    value: true
                }
            ];

            $scope.jobGroup = {};

            $scope.getColor = function (status) {
                return CommonService.getColor(status);
            };

            $scope.jobs = [];
            $scope.jobServices = [];

            function loadJobs() {
                JobService.getJobs().then(function (result) {
                    $scope.jobs = result.data;
                    $scope.jobServices = result.included;
                    groupJobs();
                });
            }
            loadJobs();

            $scope.$on('refresh.jobs', function (event, result) {
                $scope.jobs = result.data;
                $scope.jobServices = result.included;
                groupJobs();
            });

            // Group jobs by their service id
            $scope.groupedJobs = {};

            function groupJobs() {
                $scope.groupedJobs = {};
                if ($scope.jobs) {
                    for (var i = 0; i < $scope.jobs.length; i++) {
                        var job = $scope.jobs[i];
                        var category = $scope.groupedJobs[job.relationships.service.data[0].id];
                        if (!category) {
                            $scope.groupedJobs[job.relationships.service.data[0].id] = [];
                        }
                        if ($scope.jobGroup[job.relationships.service.data[0].id] === undefined) {
                            $scope.jobGroup[job.relationships.service.data[0].id] = {
                                opened: false
                            };
                        }
                        $scope.groupedJobs[job.relationships.service.data[0].id].push(job);
                    }
                }
            }
            groupJobs();

            $scope.isJobGroupOpened = function (serviceId) {
                return $scope.jobGroup[serviceId].opened;
            };

            $scope.toggleJobGroup = function (serviceId) {
                $scope.jobGroup[serviceId].opened = !$scope.jobGroup[serviceId].opened;
                $scope.$broadcast('rebuild:scrollbar');
            };

            $scope.displayFilters = false;

            $scope.toggleFilters = function () {
                $scope.displayFilters = !$scope.displayFilters;
                $scope.$broadcast('rebuild:scrollbar');
            };

            $scope.filteredJobStatuses = [];

            $scope.filterJobs = function () {
                $scope.filteredJobStatuses = [];
                for (var i = 0; i < $scope.jobStatuses.length; i++) {
                    if ($scope.jobStatuses[i].value === true) {
                        $scope.filteredJobStatuses.push($scope.jobStatuses[i].name);
                    }
                }
            };
            $scope.filterJobs();

            $scope.selectJob = function (job) {
                $rootScope.$broadcast('select.job', job);
                var container = document.getElementById('bottombar');
                container.scrollTop = 0;
            };

            $scope.removeJob = function (event, job) {
                CommonService.confirm(event, 'Are you sure you want to delete job ' + job.id + "?").then(function (confirmed) {
                    if (confirmed === false) {
                        return;
                    }

                    JobService.removeJob(job).then(function () {
                        console.log('remove ', job);
                        $rootScope.$broadcast('delete.job', job);
                        var i = $scope.jobs.indexOf(job);
                        $scope.jobs.splice(i, 1);
                        groupJobs();
                        console.log($scope.groupedJobs[job.relationships.service.data[0].id]);
                        if ($scope.groupedJobs[job.relationships.service.data[0].id] === undefined) {
                            console.log('last job in the group, remove group');
                            for (var serviceIndex = 0; serviceIndex < $scope.jobServices.length; serviceIndex++) {
                                if ($scope.jobServices[serviceIndex].id === job.relationships.service.data[0].id) {
                                    $scope.jobServices.splice(serviceIndex, 1);
                                    break;
                                }
                            }
                        }
                    });
                });
            };

            $scope.hasGuiEndPoint = function (endPoint) {
                if (endPoint && endPoint.includes("http")) {
                    return true;
                }
                return false;
            };

            function getLiItems(object) {
                var result = '';
                if (object instanceof Object === true && Object.keys(object).length > 0) {
                    for (var key in object) {
                        result = result + '<li>' +
                            '<div class="row">' +
                            '<div class="col-md-2">' + key +
                            '</div>' +
                            '<div class="col-md-10 wrap-text">' + object[key] +
                            '</div>' +
                            '</div>' +
                            '</li>';
                    }
                } else {
                    result = '<li>-</li>';
                }
                return result;
            }

    }]);
});
