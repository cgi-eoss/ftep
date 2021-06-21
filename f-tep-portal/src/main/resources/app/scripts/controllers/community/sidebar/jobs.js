/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityJobsCtrl
 * @description
 * # CommunityJobsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityJobsCtrl', ['JobService', '$scope', '$mdDialog', function (JobService, $scope, $mdDialog) {

        /* Get stored Jobs details */
        $scope.jobParams = JobService.params.community;
        $scope.jobOwnershipFilters = JobService.jobOwnershipFilters;
        $scope.item = "Job";
        $scope.jobStatuses = JobService.JOB_STATUSES;

        /* Get jobs */
        JobService.refreshJobs('community');

        /* Update jobs when polling */
        $scope.$on('poll.jobs', function(event, data) {
            $scope.jobParams.jobs = data;
        });

        /* Stop Polling */
        $scope.$on("$destroy", function() {
            JobService.stopPolling();
        });

        /* Paging */
        $scope.getPage = function(url){
            JobService.getJobsPage('community', url);
        };

        $scope.filter = function(){
            JobService.getJobsByFilter('community');
        };

        /* Select a Job */
        $scope.selectJob = function(item) {
            $scope.jobParams.selectedJob = item;
            JobService.refreshSelectedJob('community');
            if (JobService.params.community.jobTab === 'OUTPUTS') {
                $scope.fetchJobOutputs();
            } else if (JobService.params.community.jobTab === 'LOGS') {
                $scope.fetchJobLogs();
            }
        };

        $scope.setParentJobFilter = function(job) {
            $scope.jobParams.parentId = job ? job.id : null;
            JobService.getJobsByFilter('community');
        };

        /* Filters */
        $scope.toggleFilters = function() {
            $scope.jobParams.displayFilters = !$scope.jobParams.displayFilters;
        };

        $scope.fetchJobOutputs = function() {
            JobService.fetchJobOutputs('community');
        };

        $scope.fetchJobLogs = function() {
            JobService.fetchJobLogs('community');
        };

    }]);
});
