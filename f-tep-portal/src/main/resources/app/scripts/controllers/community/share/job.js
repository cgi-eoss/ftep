/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareJobCtrl
 * @description
 * # CommunityShareJobCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareJobCtrl', ['JobService', 'CommunityService', '$scope', function (JobService, CommunityService, $scope) {

         /* Get stored Group & Community details */
        $scope.jobParams = JobService.params.community;
        $scope.permissions = CommunityService.permissionTypes;

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.jobParams.sharedGroupsDisplayFilters = !$scope.jobParams.sharedGroupsDisplayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.jobParams.sharedGroupsSearchText
        };

        $scope.quickSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

    }]);
});

