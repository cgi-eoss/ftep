/**
 * @ngdoc function
 * @name ftepApp.controller:ServiceCtrl
 * @description
 * # ServiceCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ServiceCtrl', ['$scope', 'ProductService', 'CommonService', '$mdDialog', function ($scope, ProductService, CommonService, $mdDialog) {

        $scope.serviceParams = ProductService.params.development;
        $scope.serviceOwnershipFilters = ProductService.serviceOwnershipFilters;
        $scope.serviceForms = {files: {title: 'Files'}, dataInputs: {title: 'Input Definitions'}, dataOutputs: {title: 'Output Definitions'}};
        $scope.serviceParams.activeArea = $scope.serviceForms.files;
        $scope.constants = {
            serviceFields: ['dataInputs', 'dataOutputs'],
            fieldTypes: [{type: 'LITERAL'}, {type: 'COMPLEX'}], //{type: 'BOUNDING_BOX'}],
            literalTypes: [{dataType: 'string'}, {dataType: 'integer'}, {dataType: 'double'}]
        };
        $scope.serviceTypes = {
            APPLICATION: { id: 0, name: 'Application', value: 'APPLICATION'},
            PROCESSOR: { id: 0, name: 'Processor', value: 'PROCESSOR'},
            BULK_PROCESSOR: { id: 0, name: 'Bulk Processor', value: 'BULK_PROCESSOR'},
        };

        $scope.toggleServiceFilter = function(){
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        ProductService.refreshServices('development');

        $scope.selectService = function(service) {
            $scope.serviceParams.displayRight = true;
            $scope.serviceParams.selectedService = service;
            ProductService.refreshSelectedService('development');
        };

        /* Update Services when polling */
        $scope.$on('poll.services', function (event, data) {
            $scope.serviceParams.services = data;
        });

        $scope.$on("$destroy", function() {
            ProductService.stopPolling();
        });

        /* Paging */
        $scope.getPage = function(url){
            ProductService.getServicesPage('development', url);
        };

        $scope.filter = function(){
            ProductService.getServicesByFilter('development');
        };

        $scope.openFile = function(file) {
            $scope.serviceParams.openedFile = file;
        };

        // The modes
        $scope.modes = ['Text', 'Dockerfile', 'Javascript', 'Perl', 'PHP', 'Python', 'Properties', 'Shell', 'XML', 'YAML' ];
        $scope.mode = {};

        $scope.updateMode = function() {
            ProductService.setFileType();
        };

        $scope.refreshMirror = function() {
            // Set mode to default if not yet assigned
            if (!$scope.serviceParams.activeMode) {
                $scope.serviceParams.activeMode = $scope.modes[0];
            }
            if($scope.editor) {
                $scope.editor.setOption("mode", $scope.serviceParams.activeMode.toLowerCase());
            }
        };

        $scope.editorOptions = {
            lineWrapping: true,
            lineNumbers: true,
            autofocus: true
        };

        $scope.codemirrorLoaded = function (editor) {

            // Set mode to default if not yet assigned
            if (!$scope.serviceParams.activeMode) {
                $scope.serviceParams.activeMode = $scope.modes[0];
            }

            // Editor part
            var doc = editor.getDoc();
            editor.focus();

            // Apply mode to editor
            $scope.editor = editor;
            editor.setOption("mode", $scope.serviceParams.activeMode.toLowerCase());

            // Options
            editor.setOption('firstLineNumber', 1);
            doc.markClean();

            editor.on("scrollCursorIntoView", function(){
                $scope.editor = editor;
                editor.setOption("mode", $scope.serviceParams.activeMode.toLowerCase());
            });
            editor.on("focus", function(){
                $scope.editor = editor;
                editor.setOption("mode", $scope.serviceParams.activeMode.toLowerCase());
            });
        };

        $scope.removeService = function(event, service){
            CommonService.confirm(event, 'Are you sure you want to delete this service: "' + service.name + '"?').then(function (confirmed){
                if(confirmed === false){
                    return;
                }
                ProductService.removeService(service).then(function(){
                    ProductService.refreshServices('development', 'Remove', service);
                });
            });
        };

        $scope.createService = function($event) {
                function CreateServiceController($scope, $mdDialog) {

                    $scope.createService = function () {
                        ProductService.createService($scope.newItem.name, $scope.newItem.description, $scope.newItem.title).then(function (newService) {
                            ProductService.refreshServices('development', 'Create', newService);
                        });
                        $mdDialog.hide();
                    };

                    $scope.closeDialog = function () {
                        $mdDialog.hide();
                    };
                }

                CreateServiceController.$inject = ['$scope', '$mdDialog'];
                $mdDialog.show({
                    controller: CreateServiceController,
                    templateUrl: 'views/developer/templates/createservice.tmpl.html',
                    parent: angular.element(document.body),
                    targetEvent: $event,
                    clickOutsideToClose: true
                });
        };

        $scope.saveService = function(){
            ProductService.saveService($scope.serviceParams.selectedService).then(function(service){
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
                        ProductService.getFileList('development');
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
            CommonService.confirm(event, 'Are you sure you want to delete the file ' + file.filename + "?").then(function(confirmed) {
                if (confirmed === false) {
                    return;
                }
                ProductService.removeServiceFile(file).then(function(){
                    ProductService.refreshSelectedService('development');
                    $scope.serviceParams.openedFile = $scope.serviceParams.selectedService.files[0];
                });
            });
        };

        $scope.addNewRow = function(key){
            // Create a descriptor if none exists
            if(!$scope.serviceParams.selectedService.serviceDescriptor){
                $scope.serviceParams.selectedService.serviceDescriptor = {
                        id: $scope.serviceParams.selectedService.name,
                        serviceProvider: $scope.serviceParams.selectedService.name
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
                },
                minOccurs: 0,
                maxOccurs: 0
            });
        };

        $scope.removeRow = function(list, item){
            var index = list.indexOf(item);
            list.splice(index, 1);
        };

        $scope.editTypeDialog = function($event, fieldDescriptor, constants){

            function EditTypeController($scope, $mdDialog) {
                var backupCopy = angular.copy(fieldDescriptor);

                $scope.input = fieldDescriptor;
                $scope.constants = constants;

                $scope.updateAttrs = function(){
                    if($scope.input.data === 'LITERAL'){
                        delete $scope.input.defaultAttrs.mimeType;
                        delete $scope.input.defaultAttrs.extension;
                        delete $scope.input.defaultAttrs.asReference;
                        $scope.input.defaultAttrs.dataType = 'string';
                    }
                    else if($scope.input.data === 'COMPLEX'){
                        delete $scope.input.defaultAttrs.dataType;
                        $scope.input.defaultAttrs.asReference = false;
                    }
                };

                $scope.setAsReference = function(str){
                    $scope.input.defaultAttrs.asReference = (str === 'true');
                };

                $scope.addAllowedValue = function(newAllowedValue){
                    if($scope.input.defaultAttrs.allowedValues && $scope.input.defaultAttrs.allowedValues !== ''){
                        var array = $scope.input.defaultAttrs.allowedValues.split(',');
                        if(array && array.indexOf(newAllowedValue) > -1){
                            return;
                        }
                        else {
                            $scope.input.defaultAttrs.allowedValues += ',' + newAllowedValue;
                        }
                    }
                    else{
                         $scope.input.defaultAttrs.allowedValues = newAllowedValue;
                    }
                };

                $scope.removeAllowedValue = function(item){
                    var array = $scope.input.defaultAttrs.allowedValues.split(',');
                    var index = array.indexOf(item);
                    array.splice(index, 1);
                    $scope.input.defaultAttrs.allowedValues = array.toString();
                };

                $scope.closeDialog = function (save) {
                    // When user clicked Cancel, revert the changes
                    if(!save){
                        fieldDescriptor.data = backupCopy.data;
                        fieldDescriptor.defaultAttrs = backupCopy.defaultAttrs;
                    }
                    $mdDialog.hide();
                };
            }

            EditTypeController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: EditTypeController,
                templateUrl: 'views/developer/templates/edittype.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: false
            });
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
