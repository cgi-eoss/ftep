/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityJobsCtrl
 * @description
 * # CommunityJobsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityJobsCtrl', ['JobService', 'MessageService', '$scope', '$mdDialog', function (JobService, MessageService, $scope, $mdDialog) {

        /* Get stored Jobs details */
        $scope.jobParams = JobService.params.community;
        $scope.item = "Job";

        /* Get jobs */
        JobService.getJobsV2().then(function (data) {
             $scope.jobParams.jobs = data;
        });

        /* Update jobs when polling */
        $scope.$on('poll.jobsV2', function (event, data) {
            $scope.jobParams.jobs = data;
        });

        /* Select a Job */
        $scope.selectJob = function (item) {
            $scope.jobParams.selectedJob = item;
            JobService.refreshSelectedJobV2("Community");
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.jobParams.displayFilters = !$scope.jobParams.displayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.jobParams.searchText
        };

        $scope.quickSearch = function (item) {
            if (item.extId.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
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
