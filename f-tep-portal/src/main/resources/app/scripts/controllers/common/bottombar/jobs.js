/**
 * @ngdoc function
 * @name ftepApp.controller:JobsCtrl
 * @description
 * # JobsCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('JobsCtrl', ['$scope', '$rootScope', 'CommonService', 'JobService', '$sce', function ($scope, $rootScope, CommonService, JobService, $sce) {

            $scope.jobParams = JobService.params.explorer;
            $scope.jobOwnershipFilters = JobService.jobOwnershipFilters;
            $scope.jobStatuses = JobService.JOB_STATUSES;

            /* Get jobs */
            JobService.refreshJobs('explorer');

            $scope.$on('poll.jobs', function (event, data) {
                $scope.jobParams.jobs = data;

                // Refresh active job if running
                if ($scope.jobParams.selectedJob && $scope.jobParams.selectedJob.status === "RUNNING") {
                    JobService.refreshSelectedJob('explorer');
                }
            });

            /* Paging */
            $scope.getPage = function(url){
                JobService.getJobsPage('explorer', url);
            };

            /* Stop Polling */
            $scope.$on("$destroy", function() {
                JobService.stopPolling();
            });

            $scope.toggleFilters = function () {
                $scope.jobParams.displayFilters = !$scope.jobParams.displayFilters;
            };

            $scope.filter = function () {
                JobService.getJobsByFilter('explorer');
            };

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
                $scope.toggleAllWMS(false);
                document.getElementById('job-container').scrollTop = 0;
            };

            $scope.deselectJob  = function () {
                $scope.jobParams.selectedJob = undefined;
            };

            $scope.wmsVisibile = false;
            $scope.wmsItemVisibile = [];
            var wmsItems = [];

            $scope.$on('map.cleared', function () {
                if( $scope.jobParams.selectedJob) {
                    $scope.toggleAllWMS(false);
                }
            });

            /* Toggles display of all wms items in job*/
            $scope.toggleAllWMS = function (show) {
                $scope.wmsVisibile = show;
                if($scope.jobParams.selectedJob.wmsLinks) {
                    if (show) {
                        $rootScope.$broadcast('update.wmslayer',  $scope.jobParams.selectedJob.wmsLinks);
                        for (var i = 0; i < $scope.jobParams.selectedJob.wmsLinks.length; i++) {
                            $scope.wmsItemVisibile[i] = true;
                        }
                    } else {
                        $rootScope.$broadcast('update.wmslayer',  []);
                        for (var j = 0; j < $scope.jobParams.selectedJob.wmsLinks.length; j++) {
                            $scope.wmsItemVisibile[j] = false;
                        }
                    }
                }
            };

            /* Toggles display of a wms item */
            $scope.toggleWMS = function (key, show) {
                $scope.wmsItemVisibile[key] = show;
                setDisplayAllStatus();
                if (show) {
                    wmsItems.push($scope.jobParams.selectedJob.wmsLinks[key]);
                } else {
                    wmsItems.pop($scope.jobParams.selectedJob.wmsLinks[key]);
                }
                $rootScope.$broadcast('update.wmslayer', wmsItems);
            };

            /* Sets the status of the display all WMS images button */
            function setDisplayAllStatus() {
                $scope.wmsVisibile = true;
                for (var i = 0; i < $scope.wmsItemVisibile.length; i++) {
                    if($scope.wmsItemVisibile[i] !== true) {
                       $scope.wmsVisibile = false;
                    }
                }
            }

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

            /* Split files for input tab */
            $scope.splitInputFiles = function(link) {
                return link.split(',');
            };

    }]);
});
