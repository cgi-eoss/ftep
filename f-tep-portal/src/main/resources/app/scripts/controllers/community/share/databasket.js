/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareDatabasketCtrl
 * @description
 * # CommunityShareDatabasketCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareDatabasketCtrl', ['BasketService', 'CommunityService', '$scope', function (BasketService, CommunityService, $scope) {

        /* Get stored Databasket & Community details */
        $scope.basketParams = BasketService.params.community;
        $scope.permissions = CommunityService.permissionTypes;

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.basketParams.sharedGroupsDisplayFilters = !$scope.basketParams.sharedGroupsDisplayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.basketParams.sharedGroupsSearchText
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
