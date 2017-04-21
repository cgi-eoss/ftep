/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareGroupCtrl
 * @description
 * # CommunityShareGroupCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareGroupCtrl', ['GroupService', 'CommunityService', '$scope', function (GroupService, CommunityService, $scope) {

        /* Get stored Group & Community details */
        $scope.groupParams = GroupService.params.community;
        $scope.permissions = CommunityService.permissionTypes;

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.groupParams.sharedGroupsDisplayFilters = !$scope.groupParams.sharedGroupsDisplayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.groupParams.sharedGroupsSearchText
        };

        $scope.quickSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshGroup = function() {
            GroupService.refreshSelectedGroup('community');
        };

    }]);
});
