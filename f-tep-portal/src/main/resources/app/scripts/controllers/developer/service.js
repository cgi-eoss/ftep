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
        $scope.serviceForms = {files: {title: 'Files'}, dataInputs: {title: 'Input Definitions'}, dataOutputs: {title: 'Output Definitions'}};
        $scope.serviceParams.activeArea = $scope.serviceForms.files;
        $scope.constants = { serviceFields: ['dataInputs', 'dataOutputs'], fieldTypes: [{type: 'string'}, {type: 'integer'}] };

        $scope.toggleServiceFilter = function(){
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        ProductService.refreshServices('development');

        $scope.selectService = function(service){
            $scope.serviceParams.selectedService = service;
            ProductService.refreshSelectedService('development');
        };

        $scope.removeService = function(service){
            ProductService.removeService(service).then(function(){
                ProductService.refreshServices('development', 'Remove', service);
            });
        };

        $scope.createService = function($event) {
            CommonService.createItemDialog($event, 'ProductService', 'createService').then(function (newService) {
                ProductService.refreshServices('development', 'Create');
            });
        };

        $scope.updateService = function(){
            ProductService.updateService($scope.serviceParams.selectedService).then(function(service){
                ProductService.refreshServices('development');
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

        $scope.addNewRow = function(key){
            // Create a descriptor if none exists
            if(!$scope.serviceParams.selectedService.serviceDescriptor){
                $scope.serviceParams.selectedService.serviceDescriptor = {
                        id: $scope.serviceParams.selectedService.name,
                        description: $scope.serviceParams.selectedService.description,
                        version: '1.0',
                        serviceProvider: $scope.serviceParams.selectedService.name
                        //TODO ...
                };
            }

            // Initialize the Input/Output array if none exists
            if(!$scope.serviceParams.selectedService.serviceDescriptor[key]){
                $scope.serviceParams.selectedService.serviceDescriptor[key] = [];
            }

            $scope.serviceParams.selectedService.serviceDescriptor[key].push({
                data: 'LITERAL',
                defaultAttrs: {
                    "dataType" : "string"
                }
            });
        };

        $scope.removeRow = function(list, item){
            var index = list.indexOf(item);
            list.splice(index, 1);
        };

        /* Check that field id is unique*/
        $scope.isValidFieldId = function(field, key){
            var isValid = true;
            for(var i = 0; i < $scope.serviceParams.selectedService.serviceDescriptor[key].length; i++){
                if(field !== $scope.serviceParams.selectedService.serviceDescriptor[key][i] &&
                   field.id === $scope.serviceParams.selectedService.serviceDescriptor[key][i].id){
                    isValid = false;
                    break;
                }
            }
            return isValid;
        };

    }]);

});
