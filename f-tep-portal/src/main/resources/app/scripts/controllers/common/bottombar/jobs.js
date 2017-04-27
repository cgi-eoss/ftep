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

            $scope.jobParams = JobService.params.explorer;
            $scope.jobOwnershipFilters = JobService.jobOwnershipFilters;

            JobService.getJobs('explorer').then(function (data) {
                $scope.jobParams.jobs = data;
                groupJobs();
            });

            $scope.$on('poll.jobs', function (event, data) {
                $scope.jobParams.jobs = data;
                groupJobs();

                // Refresh active job if running
                if ($scope.jobParams.selectedJob && $scope.jobParams.selectedJob.status === "RUNNING") {
                    JobService.refreshSelectedJob('explorer');
                }

            });

            // Group jobs by their service name
            $scope.groupedJobs = {};

            function groupJobs() {
                $scope.groupedJobs = {};
                if ($scope.jobParams.jobs) {
                    for (var i = 0; i < $scope.jobParams.jobs.length; i++) {
                        var job = $scope.jobParams.jobs[i];

                        if ($scope.groupedJobs[job.serviceName] === undefined) {
                            $scope.groupedJobs[job.serviceName] = [];
                        }

                        if ($scope.jobParams.jobCategoryInfo[job.serviceName] === undefined) {
                            $scope.jobParams.jobCategoryInfo[job.serviceName] = {
                                opened: false
                            };
                        }
                        $scope.groupedJobs[job.serviceName].push(job);
                    }
                }
            }
            groupJobs();

            $scope.$on('update.jobGroups', function (event, data) {
                groupJobs();
            });

            $scope.isJobGroupOpened = function (serviceName) {
                return $scope.jobParams.jobCategoryInfo[serviceName].opened;
            };

            $scope.toggleJobGroup = function (serviceName) {
                $scope.jobParams.jobCategoryInfo[serviceName].opened = !$scope.jobParams.jobCategoryInfo[serviceName].opened;
            };

            $scope.toggleFilters = function () {
                $scope.jobParams.displayFilters = !$scope.jobParams.displayFilters;
            };

            $scope.filteredJobStatuses = [];

            $scope.filterJobs = function () {
                $scope.filteredJobStatuses = [];
                for (var i = 0; i < $scope.jobParams.jobStatuses.length; i++) {
                    if ($scope.jobParams.jobStatuses[i].value === true) {
                        $scope.filteredJobStatuses.push($scope.jobParams.jobStatuses[i].name);
                    }
                }
            };
            $scope.filterJobs();

            $scope.repeatJob = function(job){
                JobService.getJobConfig(job).then(function(config){
                    $rootScope.$broadcast('update.selectedService', config._embedded.service, config.inputs);
                });
            };

            $scope.hasGuiEndPoint = function (job) {
                if (job._links && job._links.gui && job._links.gui.href && job._links.gui.href.includes("http")) {
                    return true;
                }
                return false;
            };

            $scope.selectJob = function (job) {
                $scope.jobParams.selectedJob = job;
                JobService.refreshSelectedJob('explorer');
//                    $rootScope.$broadcast('show.products', job.id, data); //TODO

                var container = document.getElementById('bottombar');
                container.scrollTop = 0;
            };

            $scope.deselectJob  = function () {
                $scope.jobParams.selectedJob = undefined;
            };

            /** GET DRAGGABLE JOB OUTPUTS **/
            $scope.getSelectedOutputFiles = function(file) {
                if ($scope.jobParams.jobSelectedOutputs.indexOf(file) < 0) {
                    $scope.jobParams.jobSelectedOutputs.push(file);
                }

                var dragObject = {
                        type: 'outputs',
                        selectedOutputs: $scope.jobParams.jobSelectedOutputs,
                        job: $scope.jobParams.selectedJob
                };
                return dragObject;
            };

            /** FOR HIGHLIGHTING THE SELECTED JOB OUTPUTS **/
            $scope.selectOutputFile = function(item){
                var index = $scope.jobParams.jobSelectedOutputs.indexOf(item);
                if (index < 0) {
                    $scope.jobParams.jobSelectedOutputs.push(item);
                } else {
                    $scope.jobParams.jobSelectedOutputs.splice(index, 1);
                }
            };

            $scope.selectAllOutputs = function(){
                $scope.jobParams.jobSelectedOutputs = [];
                $scope.jobParams.jobSelectedOutputs.push.apply($scope.jobParams.jobSelectedOutputs, $scope.jobParams.selectedJob.outputs.result);
            };

            $scope.clearOutputsSelection = function(){
                $scope.jobParams.jobSelectedOutputs = [];
            };

            $scope.invertOutputsSelection = function(){
                var newSelection = [];
                for(var i = 0; i < $scope.jobParams.selectedJob.outputs.result.length; i++) {
                    if($scope.jobParams.jobSelectedOutputs.indexOf($scope.jobParams.selectedJob.outputs.result[i]) < 0){
                         newSelection.push($scope.jobParams.selectedJob.outputs.result[i]);
                    }
                }
                $scope.jobParams.jobSelectedOutputs = [];
                $scope.jobParams.jobSelectedOutputs.push.apply($scope.jobParams.jobSelectedOutputs, newSelection);
            };

            $scope.isOutputSelected = function(item) {
                return $scope.jobParams.jobSelectedOutputs.indexOf(item) > -1;
            };

    }]);
});
