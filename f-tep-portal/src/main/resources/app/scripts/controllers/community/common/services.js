/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityServicesCtrl
 * @description
 * # CommunityServicesCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityServicesCtrl', ['ProductService', 'CommonService', '$scope', function (ProductService, CommonService, $scope) {

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

        /* Stop polling */
        $scope.$on("$destroy", function() {
            ProductService.stopPolling();
        });

        /* Paging */
        $scope.getPage = function(url){
            ProductService.getServicesPage('community', url);
        };

        $scope.filter = function(){
            ProductService.getServicesByFilter('community');
        };

        /* Select a Service */
        $scope.selectService = function (item) {
            $scope.serviceParams.selectedService = item;
            ProductService.refreshSelectedService("community");
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        /* Remove Service */
        $scope.removeServiceItem = function (key, item) {
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
