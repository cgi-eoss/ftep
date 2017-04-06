/**
 * @ngdoc function
 * @name ftepApp.controller:ServiceCtrl
 * @description
 * # ServiceCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ServiceCtrl', ['$scope', 'ProductService', 'CommonService', '$mdDialog',
                                           function ($scope, ProductService, CommonService, $mdDialog) {

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
                ProductService.selectService(newService);
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

        $scope.createFileDialog = function($event){
            function CreateFileController($scope, $mdDialog, ProductService) {

                $scope.addFile = function () {
                    var newFile = {
                            filename: $scope.file.filename,
                            content: btoa('# ' + $scope.file.filename),
                            service: ProductService.params.development.selectedService._links.self.href,
                            executable: $scope.file.executable
                    };
                    ProductService.addFile(newFile).then(function(data){
                        ProductService.params.development.selectedService.files.push(data);
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }

            CreateFileController.$inject = ['$scope', '$mdDialog', 'ProductService'];
            $mdDialog.show({
                controller: CreateFileController,
                templateUrl: 'views/developer/templates/createfile.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        $scope.deleteFileDialog = function(event, file){
            CommonService.confirm(event, 'Are you sure you want to delete the file ' + file.filename + "?").then(function (confirmed) {
                if (confirmed === false) {
                    return;
                }
                ProductService.removeServiceFile(file).then(function(){
                    var index = $scope.serviceParams.selectedService.files.indexOf(file);
                    $scope.serviceParams.selectedService.files.splice(index,1);
                });
            });
        };

    }]);

});