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

            var jobs = [];
            $scope.serviceGroups = [];

            function loadJobs() {
                JobService.getJobs().then(function (result) {
                    jobs = result.data;
                    $scope.serviceGroups = result.included;
                    groupJobs();
                });
            }
            loadJobs();

            $scope.$on('refresh.jobs', function (event, result) {
                jobs = result.data;
                $scope.serviceGroups = result.included;
                groupJobs();

                //update selected job
                if($scope.jobParams.selectedJob){
                    for(var i = 0; i < result.data.length; i++){
                        if($scope.jobParams.selectedJob.id == result.data[i].id){
                            $scope.jobParams.selectedJob = result.data[i];
                            JobService.getOutputs($scope.jobParams.selectedJob.id).then(function(data){
                                $scope.jobParams.jobOutputs = data;
                            });
                            break;
                        }
                    }
                }
                $scope.$broadcast('rebuild:scrollbar');
            });

            // Group jobs by their service id
            $scope.groupedJobs = {};

            function groupJobs() {
                $scope.groupedJobs = {};
                if (jobs) {
                    for (var i = 0; i < jobs.length; i++) {
                        var job = jobs[i];
                        var category = $scope.groupedJobs[job.relationships.service.data[0].id];
                        if (!category) {
                            $scope.groupedJobs[job.relationships.service.data[0].id] = [];
                        }
                        if ($scope.jobParams.jobGroupInfo[job.relationships.service.data[0].id] === undefined) {
                            $scope.jobParams.jobGroupInfo[job.relationships.service.data[0].id] = {
                                opened: false
                            };
                        }
                        $scope.groupedJobs[job.relationships.service.data[0].id].push(job);
                    }
                }
            }
            groupJobs();

            $scope.isJobGroupOpened = function (serviceId) {
                return $scope.jobParams.jobGroupInfo[serviceId].opened;
            };

            $scope.toggleJobGroup = function (serviceId) {
                $scope.jobParams.jobGroupInfo[serviceId].opened = !$scope.jobParams.jobGroupInfo[serviceId].opened;
                $scope.$broadcast('rebuild:scrollbar');
            };

            $scope.toggleFilters = function () {
                $scope.jobParams.displayFilters = !$scope.jobParams.displayFilters;
                $scope.$broadcast('rebuild:scrollbar');
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

            $scope.removeJob = function (event, job) {
                CommonService.confirm(event, 'Are you sure you want to delete job ' + job.id + "?").then(function (confirmed) {
                    if (confirmed === false) {
                        return;
                    }

                    JobService.removeJob(job).then(function () {
                        var i = jobs.indexOf(job);
                        jobs.splice(i, 1);
                        groupJobs();
                        if ($scope.groupedJobs[job.relationships.service.data[0].id] === undefined) {
                            console.log('last job in the group, remove group');
                            for (var serviceIndex = 0; serviceIndex < $scope.serviceGroups.length; serviceIndex++) {
                                if ($scope.serviceGroups[serviceIndex].id === job.relationships.service.data[0].id) {
                                    $scope.serviceGroups.splice(serviceIndex, 1);
                                    break;
                                }
                            }
                        }
                        if(angular.equals(job, $scope.jobParams.selectedJob)){
                            delete $scope.jobParams.selectedJob;
                        }
                        $scope.$broadcast('rebuild:scrollbar');
                    });
                });
            };

            $scope.repeatJob = function(job){
                $rootScope.$broadcast('rerun.service', job.attributes.inputs, job.relationships.service.data[0].id);
            };

            $scope.hasGuiEndPoint = function (endPoint) {
                if (endPoint && endPoint.includes("http")) {
                    return true;
                }
                return false;
            };

            $scope.isVisible = function(job, service){
                var jobOwnershipFilter = false;
                if($scope.jobParams.selectedOwnershipFilter === $scope.jobOwnershipFilters.MY_JOBS){
                    jobOwnershipFilter = (job.attributes.permissionLevel === 'OWNER');
                }
                else if($scope.jobParams.selectedOwnershipFilter === $scope.jobOwnershipFilters.SHARED_JOBS){
                    jobOwnershipFilter = (job.attributes.permissionLevel !== 'OWNER');
                }
                else{
                    jobOwnershipFilter = true;
                }

                return $scope.isJobGroupOpened(service.id) && $scope.filteredJobStatuses.indexOf(job.attributes.status) > -1 && jobOwnershipFilter;
            };

            /* Selected Job */
            $scope.selectJob = function (job) {
                $scope.jobParams.selectedJob = job;
                $scope.jobParams.jobSelectedOutputs = [];
                JobService.getOutputs(job.id).then(function(data){
                    $scope.jobParams.jobOutputs = data;
                    $rootScope.$broadcast('show.products', job.id, data);
                });

                $scope.$broadcast('rebuild:scrollbar');
                var container = document.getElementById('bottombar');
                container.scrollTop = 0;
            };

            $scope.getJobInputs = function(job) {

                $scope.$broadcast('rebuild:scrollbar');

                if (job.attributes.inputs instanceof Object && Object.keys(job.attributes.inputs).length > 0) {
                    return job.attributes.inputs;
                }
                else {
                    return undefined;
                }
            };

            $scope.getOutputLink = function(link){
                return CommonService.getOutputLink(link);
            };

            $scope.getSelectedOutputFiles = function(file) {

                if ($scope.jobParams.jobSelectedOutputs.indexOf(file) < 0) {
                    $scope.jobParams.jobSelectedOutputs.push(file);
                }
                $scope.$broadcast('rebuild:scrollbar');

                return $scope.jobParams.jobSelectedOutputs;
            };

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
                $scope.jobParams.jobSelectedOutputs.push.apply($scope.jobParams.jobSelectedOutputs, $scope.jobParams.jobOutputs);
            };

            $scope.clearOutputsSelection = function(){
                $scope.jobParams.jobSelectedOutputs = [];
            };

            $scope.invertOutputsSelection = function(){
                var newSelection = [];
                for(var i = 0; i < $scope.jobParams.jobOutputs.length; i++) {
                    if($scope.jobParams.jobSelectedOutputs.indexOf($scope.jobParams.jobOutputs[i]) < 0){
                         newSelection.push($scope.jobParams.jobOutputs[i]);
                    }
                }
                $scope.jobParams.jobSelectedOutputs = [];
                $scope.jobParams.jobSelectedOutputs.push.apply($scope.jobParams.jobSelectedOutputs, newSelection);
            };

            $scope.isOutputSelected = function(item) {
                return $scope.jobParams.jobSelectedOutputs.indexOf(item) > -1;
            };

            /* End of Selected Job */

    }]);
});
