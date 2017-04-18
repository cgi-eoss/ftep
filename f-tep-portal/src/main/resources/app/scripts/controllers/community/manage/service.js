/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageServiceCtrl
 * @description
 * # CommunityServiceCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageServiceCtrl', ['ProductService', '$scope', function (ProductService, $scope) {

        /* Get stored Services & Contents details */
        $scope.serviceParams = ProductService.params.community;
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
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Remove file from service */
        $scope.removeItem = function(files, file) {
            ProductService.removeItem($scope.projectParams.selectedProject, files, file).then(function (data) {
                ProductService.refreshServices("community");
                /* TODO: Implement removeItem in ProductService */
            });
        };

    }]);
});
