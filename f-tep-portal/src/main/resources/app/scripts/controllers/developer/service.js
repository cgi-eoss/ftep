/**
 * @ngdoc function
 * @name ftepApp.controller:ServiceCtrl
 * @description
 * # ServiceCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ServiceCtrl', ['$scope', 'ProductService', 'CommonService', function ($scope, ProductService, CommonService) {

        $scope.serviceParams = ProductService.params.development;

        $scope.toggleServiceFilter = function(){
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        $scope.userServices = ProductService.getUserServicesCache();

        ProductService.getUserServices().then(function(result){
            $scope.userServices = result;
        });

        $scope.selectService = function(service){
            ProductService.selectService(service);
        };

        $scope.removeService = function(service){
            ProductService.removeService(service).then(function(){
                if($scope.serviceParams.selectedService && $scope.serviceParams.selectedService.id === service.id){
                    $scope.serviceParams.selectedService = undefined;
                    $scope.serviceParams.displayRight = false;
                }

                ProductService.getUserServices().then(function(result){
                    $scope.userServices = result;
                });
            });
        };

        $scope.createService = function($event) {
            CommonService.createItemDialog($event, 'ProductService', 'createService').then(function (newService) {
                ProductService.getUserServices().then(function(result){
                    $scope.userServices = result;
                });
            });
        };

        $scope.updateService = function(){
            ProductService.updateService().then(function(service){
                ProductService.getUserServices().then(function(result){
                    $scope.userServices = result;
                });
            });
        };

    }]);

});