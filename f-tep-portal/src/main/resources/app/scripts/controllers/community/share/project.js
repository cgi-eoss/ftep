/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareProjectCtrl
 * @description
 * # CommunityProjectCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareProjectCtrl', ['ProjectService', 'CommunityService', '$scope', function (ProjectService, CommunityService, $scope) {

        /* Get stored Project & Community details */
        $scope.projectParams = ProjectService.params.community;
        $scope.permissions = CommunityService.permissionTypes;

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.projectParams.sharedGroupsDisplayFilters = !$scope.projectParams.sharedGroupsDisplayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.projectParams.sharedGroupsSearchText
        };

        $scope.quickSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshProject = function() {
            ProjectService.refreshSelectedProject('community');
        };

    }]);
});
