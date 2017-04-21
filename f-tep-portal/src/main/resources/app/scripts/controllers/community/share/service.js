/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareServiceCtrl
 * @description
 * # CommunityServiceCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareServiceCtrl', ['ProductService', 'CommunityService', '$scope', function (ProductService, CommunityService, $scope) {

        /* Get stored Service & Community details */
        $scope.serviceParams = ProductService.params.community;
        $scope.permissions = CommunityService.permissionTypes;

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.serviceParams.sharedGroupsDisplayFilters = !$scope.serviceParams.sharedGroupsDisplayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.serviceParams.sharedGroupsSearchText
        };

        $scope.quickSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshService = function() {
            ProductService.refreshSelectedService('community');
        };

    }]);
});
