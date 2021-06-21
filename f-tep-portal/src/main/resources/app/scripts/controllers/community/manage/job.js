/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageJobCtrl
 * @description
 * # CommunityManageJobCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageJobCtrl', ['CommunityService', 'JobService', 'CommonService', '$scope', function (CommunityService, JobService, CommonService, $scope) {

        /* Get stored Jobs details */
        $scope.jobParams = JobService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "Job";

        /* Filters */
        $scope.toggleSharingFilters = function () {
            $scope.jobParams.sharedGroupsDisplayFilters = !$scope.jobParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.jobParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshJob = function() {
            JobService.refreshSelectedJob('community');
        };

        $scope.splitInputFiles = function(link) {
            return link.split(',');
        };

        $scope.estimateDownloadCost = function($event, file){
            CommonService.estimateDownloadCost($event, file);
        };

        $scope.updateJobTab = function(tab) {
            JobService.updateJobTab('community', tab);
        };

        $scope.fetchJobOutputs = function() {
            JobService.fetchJobOutputs('community');
        };

        $scope.fetchJobLogs = function() {
            JobService.fetchJobLogs('community');
        };

    }]);
});

