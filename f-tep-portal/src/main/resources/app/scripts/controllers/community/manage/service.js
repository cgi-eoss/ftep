/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageServiceCtrl
 * @description
 * # CommunityServiceCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageServiceCtrl', ['CommunityService', 'ProductService', '$scope', function (CommunityService, ProductService, $scope) {

        /* Get stored Services & Contents details */
        $scope.serviceParams = ProductService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "Service File";

        /* Filters */
        $scope.toggleContentsFilters = function () {
            $scope.serviceParams.displayContentsFilters = !$scope.serviceParams.displayContentsFilters;
        };

        $scope.contentsSearch = {
            searchText: $scope.serviceParams.contentsSearchText
        };

        $scope.contentsQuickSearch = function (item) {
            if (item.filename.toLowerCase().indexOf(
                $scope.contentsSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.toggleSharingFilters = function () {
            $scope.serviceParams.sharedGroupsDisplayFilters = !$scope.serviceParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.serviceParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshService = function() {
            ProductService.refreshSelectedService('community');
        };

        /* Remove file from service */
        $scope.removeServiceItem = function(files, file) {
            ProductService.removeServiceItem($scope.projectParams.selectedProject, files, file).then(function (data) {
                ProductService.refreshServices("community");
                /* TODO: Implement removeServiceItem in ProductService */
            });
        };

    }]);
});
