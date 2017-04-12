/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareFileCtrl
 * @description
 * # CommunityShareFileCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareFileCtrl', ['FileService', 'CommunityService', '$scope', function (FileService, CommunityService, $scope) {

        /* Get stored File & Community details */
        $scope.fileParams = FileService.params.community;
        $scope.permissions = CommunityService.permissionTypes;

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.fileParams.sharedGroupsDisplayFilters = !$scope.fileParams.sharedGroupsDisplayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.fileParams.sharedGroupsSearchText
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
