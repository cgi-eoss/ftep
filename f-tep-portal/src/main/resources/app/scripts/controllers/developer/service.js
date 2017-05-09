/**
 * @ngdoc function
 * @name ftepApp.controller:ServiceCtrl
 * @description
 * # ServiceCtrl
 * Controller of the ftepApp
 */
'use strict';
//require([
//         "codemirror",
//         "codemirror/lib/codemirror",
//         "codemirror/mode/dockerfile/dockerfile",
//         "codemirror/mode/javascript/javascript",
//         "codemirror/mode/perl/perl",
//         "codemirror/mode/php/php",
//         "codemirror/mode/properties/properties",
//         "codemirror/mode/python/python",
//         "codemirror/mode/shell/shell",
//         "codemirror/mode/xml/xml",
//         "codemirror/mode/yaml/yaml"
//       ], function(CodeMirror) {
//           window.CodeMirror = CodeMirror;
//});

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ServiceCtrl', ['$scope', 'ProductService', 'CommonService', '$mdDialog',
                                           function ($scope, ProductService, CommonService, $mdDialog) {

        $scope.serviceParams = ProductService.params.development;
        $scope.serviceForms = {files: {title: 'Files'}, dataInputs: {title: 'Input Definitions'}, dataOutputs: {title: 'Output Definitions'}};
        $scope.serviceParams.activeArea = $scope.serviceForms.files;
        $scope.constants = {
                serviceFields: ['dataInputs', 'dataOutputs'],
                fieldTypes: [{type: 'LITERAL'}, {type: 'COMPLEX'}],
                literalTypes: [{dataType: 'string'}, {dataType: 'integer'}]
        };

        $scope.toggleServiceFilter = function(){
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        ProductService.refreshServices('development');

        $scope.selectService = function(service){
            $scope.serviceParams.displayRight = true;
            $scope.serviceParams.selectedService = service;
            ProductService.refreshSelectedService('development');
        };

        /* Update Services when polling */
        $scope.$on('poll.services', function (event, data) {
            $scope.serviceParams.services = data;
        });

        /* Paging */
        $scope.getPage = function(url){
            ProductService.getServicesPage('development', url);
        };

        $scope.$on("$destroy", function() {
            ProductService.stopPolling();
        });

//        // The modes
//        $scope.modes = ['Scheme', 'Dockerfile', 'Javascript', 'Perl', 'PHP', 'Python', 'Properties', 'Shell', 'XML', 'YAML' ];
//        $scope.mode = {};
//
//        $scope.selectDefaultMode = function(file){
//            $scope.mode.active = $scope.modes[7];
//
//            if(!file){
//                file = $scope.serviceParams.selectedService.files[0];
//            }
//
//            if(file && file.filename.toLowerCase() === 'dockerfile'){
//                $scope.mode.active = $scope.modes[1];
//            }
//            else if(file && file.filename.toLowerCase() === 'workflow.sh'){
//                $scope.mode.active = $scope.modes[7];
//            }
//        };
//
//        $scope.refreshMirror = function(){
//            if($scope.editor) {
//                $scope.editor.setOption("mode", $scope.mode.active.toLowerCase());
//            }
//        };
//
//        $scope.editorOptions = {
//            lineWrapping: true,
//            lineNumbers: true,
//            autofocus: true
//        };

//        $scope.codemirrorLoaded = function (editor) {
//            // Editor part
//            var doc = editor.getDoc();
//            editor.focus();
//
//            // Apply mode to editor
//            $scope.editor = editor;
//            editor.setOption("mode", $scope.mode.active.toLowerCase());
//
//            // Options
//            editor.setOption('firstLineNumber', 1);
//            doc.markClean();
//
//            editor.on("scrollCursorIntoView", function(){
//                $scope.editor = editor;
//                editor.setOption("mode", $scope.mode.active.toLowerCase());
//            });
//            editor.on("focus", function(){
//                $scope.editor = editor;
//                editor.setOption("mode", $scope.mode.active.toLowerCase());
//            });
//        };

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
                    }
                    else if($scope.input.data === 'COMPLEX'){
                        delete $scope.input.defaultAttrs.dataType;
                        $scope.input.defaultAttrs.asReference = false;
                    }
                };

                $scope.setAsReference = function(str){
                    $scope.input.defaultAttrs.asReference = (str === 'true');
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
