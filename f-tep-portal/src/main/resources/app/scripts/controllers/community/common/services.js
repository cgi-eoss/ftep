/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityServicesCtrl
 * @description
 * # CommunityServicesCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityServicesCtrl', ['ProductService', 'CommonService', '$scope', '$sce', function (ProductService, CommonService, $scope, $sce) {

        /* Get stored Service details */
        $scope.serviceParams = ProductService.params.community;
        $scope.serviceOwnershipFilters = ProductService.serviceOwnershipFilters;
        $scope.item = "Service";

        /* Get Services */
        ProductService.refreshServices("community");

        /* Update Services when polling */
        $scope.$on('poll.services', function (event, data) {
            $scope.serviceParams.services = data;
        });

        /* Select a Service */
        $scope.selectService = function (item) {
            $scope.serviceParams.selectedService = item;
            ProductService.refreshSelectedService("community");
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.serviceParams.searchText
        };

        $scope.quickSearch = function (item) {
            if (item.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Service Description Popup */
        var popover = {};
        $scope.getDescription = function (item) {
            if (!item.description) {
                item.description = "No description.";
            }
            var html =
                '<div class="metadata">' +
                    '<div class="row">' +
                        '<div class="col-sm-12">' + item.description + '</div>' +
                    '</div>' +
                '</div>';
            return popover[html] || (popover[html] = $sce.trustAsHtml(html));
        };

        /* Remove Service */
        $scope.removeItem = function (key, item) {
             ProductService.removeService(item).then(function (data) {
                 ProductService.refreshServices("community", "Remove", item);
            });
        };

        /* Edit Service */
        $scope.editItemDialog = function ($event, item) {
            CommonService.editItemDialog($event, item, 'ProductService', 'updateService').then(function (updatedService) {
                ProductService.refreshServices("community");
            });
        };

    }]);
});
