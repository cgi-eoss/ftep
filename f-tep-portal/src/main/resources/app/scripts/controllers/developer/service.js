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

        var editmode = false;

        $scope.serviceParams = ProductService.params.development;
        $scope.serviceOwnershipFilters = ProductService.serviceOwnershipFilters;
        $scope.serviceTypeFilters = ProductService.serviceTypeFilters;
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
            BULK_PROCESSOR: { id: 0, name: 'Bulk Processor', value: 'BULK_PROCESSOR'}
        };

        $scope.toggleServiceFilter = function(){
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        ProductService.refreshServices('development');

        function discardChangesMessage(event) {
            if (editmode) {
                var answer = confirm("Are you sure you want to leave this page? Unsaved changes will be discarded.");
                if (!answer) {
                    event.preventDefault();
                    return false;
                } else {
                    editmode = false;

                    return true;
                }
            } else {
                return true;
            }
        }

        $scope.toggleEditMode = function(state) {
            editmode = state;
        };

        $scope.$on('$locationChangeStart', function( event ) {
            discardChangesMessage(event);
        });

        $scope.selectService = function(service, event) {
            if(discardChangesMessage(event)) {
                $scope.serviceParams.displayRight = true;
                $scope.serviceParams.selectedService = service;
                ProductService.refreshSelectedService('development');
            }
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

        $scope.openFile = function(file, event) {
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
                editmode = false;
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
                        ProductService.refreshSelectedService('development');
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

        $scope.deleteFileDialog = function(file, event){
            CommonService.confirm(event, 'Are you sure you want to delete the file ' + file.name + "?").then(function(confirmed) {
                if (confirmed === false) {
                    return;
                }
                ProductService.removeServiceFile(file).then(function(){
                    ProductService.refreshSelectedService('development');
                });
            });
        };

        $scope.removeRow = function(list, item){
            var index = list.indexOf(item);
            list.splice(index, 1);
        };

        $scope.definitionDialog = function($event, serviceDescriptor, key, index) {
            function DefinitionController($scope, $mdDialog, ProductService) {

                $scope.serviceParams = ProductService.params.development;
                $scope.idUnique = true;
                $scope.constants = {
                    serviceFields: ['dataInputs', 'dataOutputs'],
                    fieldTypes: [{type: 'LITERAL'}, {type: 'COMPLEX'}], //{type: 'BOUNDING_BOX'}],
                    literalTypes: [{dataType: 'string'}, {dataType: 'integer'}, {dataType: 'double'}]
                };

                // Create a descriptor if none exists
                if (!$scope.serviceParams.selectedService.serviceDescriptor) {
                    $scope.serviceParams.selectedService.serviceDescriptor = {
                        id: $scope.serviceParams.selectedService.name,
                        serviceProvider: $scope.serviceParams.selectedService.name
                    };
                }

                // Initialize the Input/Output array if none exists
                if (!$scope.serviceParams.selectedService.serviceDescriptor[key]) {
                    $scope.serviceParams.selectedService.serviceDescriptor[key] = [];
                }

                // Initialize default values or populate from existing
                if (!index && index !== 0) {
                    $scope.input = {
                        data: 'LITERAL',
                        defaultAttrs: {
                            "dataType" : "string"
                        },
                        minOccurs: 0,
                        maxOccurs: 0,
                        dataReference: false,
                        searchParameter: false,
                        parallelParameter: false
                    };
                    index = $scope.serviceParams.selectedService.serviceDescriptor[key].length;
                } else {
                    $scope.input = angular.copy(serviceDescriptor[key][index]);
                }

                /* Check that field id is unique*/
                $scope.isValidFieldId = function(id){
                    $scope.idUnique = true;
                    for (var i = 0; i < serviceDescriptor[key].length; i++) {
                        if (id === serviceDescriptor[key][i].id && index !== i) {
                            $scope.idUnique = false;
                            break;
                        }
                    }
                };

                /* Update attributes to save based on datatype */
                $scope.updateAttrs = function() {
                    if ($scope.input.data === 'LITERAL'){
                        delete $scope.input.defaultAttrs.mimeType;
                        delete $scope.input.defaultAttrs.extension;
                        delete $scope.input.defaultAttrs.asReference;
                        $scope.input.defaultAttrs.dataType = 'string';
                    } else if ($scope.input.data === 'COMPLEX'){
                        delete $scope.input.defaultAttrs.dataType;
                        $scope.input.defaultAttrs.asReference = false;
                    }
                };

                $scope.setAsReference = function(str) {
                    $scope.input.defaultAttrs.asReference = (str === 'true');
                };

                /* Add new allowed value for LITERAL data types */
                $scope.addAllowedValue = function(newAllowedValue) {
                    if ($scope.input.defaultAttrs.allowedValues && $scope.input.defaultAttrs.allowedValues !== '') {
                        var array = $scope.input.defaultAttrs.allowedValues.split(',');
                        if (array && array.indexOf(newAllowedValue) > -1) {
                            return;
                        } else {
                            $scope.input.defaultAttrs.allowedValues += ',' + newAllowedValue;
                        }
                    } else {
                        $scope.input.defaultAttrs.allowedValues = newAllowedValue;
                    }
                    $scope.newAllowedVal = '';
                };

                /* Remove allowed value for LITERAL data types */
                $scope.removeAllowedValue = function(item) {
                    var array = $scope.input.defaultAttrs.allowedValues.split(',');
                    array.splice(array.indexOf(item), 1);
                    $scope.input.defaultAttrs.allowedValues = array.toString();
                };

                $scope.closeDialog = function (save) {
                    if(save) {
                        $scope.serviceParams.selectedService.serviceDescriptor[key][index] = angular.copy($scope.input);
                    }
                    $mdDialog.hide();
                };
            }

            DefinitionController.$inject = ['$scope', '$mdDialog', 'ProductService'];
            $mdDialog.show({
                controller: DefinitionController,
                templateUrl: 'views/developer/templates/definition.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

    }]);

});
