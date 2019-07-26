/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityJobsCtrl
 * @description
 * # CommunityJobsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunitySystematicProcessingsCtrl', ['SystematicService', 'CommonService', 'TabService', 'JobService', '$scope', '$http', '$mdDialog', function (SystematicService, CommonService, TabService, JobService, $scope, $http, $mdDialog) {

        $scope.systematicParams = SystematicService.params.community;
        $scope.ownershipFilters = SystematicService.ownershipFilters;
        $scope.item = "Systematic processings";

        SystematicService.refreshSystematicProcessings('community');

        $scope.$on('poll.systematicprocs', function (event, data) {
            $scope.systematicParams.systematicProcessings = data;
        });

        $scope.$on("$destroy", function() {
            SystematicService.stopPolling();
        });

        $scope.filter = function() {
            SystematicService.getSystematicProcessingsByFilter('community');
        }

        $scope.getPage = function(url){
            SystematicService.getSystematicProcessingsPage('community', url);
        };

        $scope.selectSystematicProcessing = function (item) {
            $scope.systematicParams.selectedSystematicProcessing = item;
            SystematicService.refreshSelectedSystematicProcessing('community');
        };

        $scope.goToParentJobPage = function(systematicProcessing) {
            // TODO: temporary solution to remove "{?projection}" from the end
            var url = systematicProcessing._links.parentJob.href;
            $http.get(url.replace('{?projection}', '')).then(function(response) {
                JobService.params.community.selectedOwnershipFilter = JobService.jobOwnershipFilters.ALL_JOBS;
                JobService.params.community.searchText = response.data.id;
                TabService.navInfo.community.activeSideNav = TabService.getCommunityNavTabs().JOBS;
                JobService.params.community.selectedJob = response.data;
                JobService.refreshSelectedJob('community');
            }, function() {

            });
        };

        /* Remove File */
        $scope.terminateSystematicProcessing = function (event, key, item) {
            CommonService.confirm(event, 'Are you sure you want to terminate this systematic processing?').then(function (confirmed) {
                if (confirmed !== false) {
                    SystematicService.terminateSystematicProcessing(item).then(function() {
                        $scope.selectSystematicProcessing(null);
                    });
                }
            });
        };

    }]);
});
