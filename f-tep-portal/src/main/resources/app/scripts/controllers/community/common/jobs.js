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

        /* Get jobs */
        JobService.refreshJobs('community');

        /* Update jobs when polling */
        $scope.$on('poll.jobs', function (event, data) {
            $scope.jobParams.jobs = data;
        });

        /* Paging */
        $scope.getPage = function(url){
            JobService.getJobsPage('community', url);
        };

        /* Stop Polling */
        $scope.$on("$destroy", function() {
            JobService.stopPolling();
        });

        /* Select a Job */
        $scope.selectJob = function (item) {
            $scope.jobPermission = item;
            $scope.jobParams.selectedJob = item;
            JobService.refreshSelectedJob('community');
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.jobParams.displayFilters = !$scope.jobParams.displayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.jobParams.searchText
        };

        $scope.quickSearch = function (item) {
            if (item.id.toString().indexOf($scope.itemSearch.searchText) > -1) {
                return true;
            }
            return false;
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

    }]);
});
