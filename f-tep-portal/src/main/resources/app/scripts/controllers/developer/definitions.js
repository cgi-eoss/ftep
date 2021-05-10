/**
 * @ngdoc function
 * @name ftepApp.controller:DefinitionsCtrl
 * @description
 * # DefinitionsCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('DefinitionsCtrl', ['$scope', 'ProductService', 'CommonService', '$mdDialog', function ($scope, ProductService, CommonService, $mdDialog) {

        $scope.serviceParams = ProductService.params.developer;
        $scope.editorOptions = { lineWrapping: true, lineNumbers: true, autofocus: true };
        // Get copy of original easyModeParameterTemplate
        var originalTemplate = $scope.serviceParams.selectedService.easyModeParameterTemplate;

        $scope.removeRow = function(easyMode, item, key){
            var serviceDescriptor = easyMode ?
                $scope.serviceParams.selectedService.easyModeServiceDescriptor.dataInputs : $scope.serviceParams.selectedService.serviceDescriptor[key];
            serviceDescriptor.splice(serviceDescriptor.indexOf(item), 1);
        };

        // Triggers a template refresh when simple mode is added to the service so the generated template is displayed
        $scope.$on('developer.definitions.editor.update', function () {
            setTimeout(function() {
                $scope.editor.refresh();
            },300);
        });

        // When the template changes, if it doesn't match the original set a flag requiring it to be validated before service save
        $scope.changeMirror = function() {
            if ($scope.serviceParams.config) {
                $scope.serviceParams.config.requiresValidation = originalTemplate !== $scope.serviceParams.selectedService.easyModeParameterTemplate;
            }
        };

        $scope.generateRequest = function() {

            var generatedEasyJobConfig = ProductService.generateEasyJobConfig(
                $scope.serviceParams.config.inputValues,
                $scope.serviceParams.selectedService.easyModeParameterTemplate,
                $scope.serviceParams.selectedService._links.self.href,
                $scope.serviceParams.config.label,
                $scope.serviceParams.config.parallelParameters,
                true
            );

            if (!generatedEasyJobConfig.error) {
                $scope.generatedJSONRequest = generatedEasyJobConfig;
                if ($scope.serviceParams.config) {
                    $scope.serviceParams.config.requiresValidation = false;
                }
            } else {
                $scope.generatedJSONRequest = generatedEasyJobConfig.error.toString();
                (function(ev) {
                    $mdDialog.show(
                        $mdDialog.alert()
                            .clickOutsideToClose(true)
                            .title('Template Error')
                            .textContent(generatedEasyJobConfig.error.toString())
                            .ariaLabel('Template Error')
                            .ok('OK')
                            .targetEvent(ev)
                    );
                })();
            }
        };

        $scope.codemirrorLoaded = function (editor) {

            // Editor part
            var doc = editor.getDoc();
            editor.focus();

            // Apply mode to editor
            $scope.editor = editor;
            editor.setOption('mode', 'javascript');

            // Options
            editor.setOption('firstLineNumber', 1);
            doc.markClean();

            editor.on('scrollCursorIntoView', function(){
                $scope.editor = editor;
                editor.setOption('mode', 'javascript');
            });
            editor.on('focus', function(){
                $scope.editor = editor;
                editor.setOption('mode', 'javascript');
            });
        };

        $scope.reformatCode = function() {
            var template = JSON.parse($scope.serviceParams.selectedService.easyModeParameterTemplate);
            $scope.serviceParams.selectedService.easyModeParameterTemplate = JSON.stringify(template,null,2);
        };

        $scope.definitionDialog = function($event, easyMode, keyVal, index) {
            function DefinitionController($scope, $mdDialog, ProductService) {

                $scope.serviceParams = ProductService.params.developer;
                var key = keyVal === 'easyMode'? 'dataInputs' : keyVal;
                var serviceDescriptor = easyMode ?
                    $scope.serviceParams.selectedService.easyModeServiceDescriptor : $scope.serviceParams.selectedService.serviceDescriptor;

                $scope.idUnique = true;

                // Create a descriptor if none exists
                if (!serviceDescriptor) {
                    serviceDescriptor = {
                        id: $scope.serviceParams.selectedService.name,
                        serviceProvider: $scope.serviceParams.selectedService.name
                    };
                }

                // Initialize the Input/Output array if none exists
                if (!serviceDescriptor[key]) {
                    serviceDescriptor[key] = [];
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
                    index = serviceDescriptor[key].length;
                } else {
                    $scope.input = angular.copy(serviceDescriptor[key][index]);
                }

                /* Check that field id is unique*/
                $scope.isValidFieldId = function(id) {
                    $scope.idUnique = true;
                    if (serviceDescriptor[key]) {
                        for (var i = 0; i < serviceDescriptor[key].length; i++) {
                            if (id === serviceDescriptor[key][i].id && index !== i) {
                                $scope.idUnique = false;
                                break;
                            }
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
                        var descriptor = $scope.serviceParams.activeArea === $scope.serviceParams.constants.tabs.easyMode ?
                            $scope.serviceParams.selectedService.easyModeServiceDescriptor.dataInputs : $scope.serviceParams.selectedService.serviceDescriptor[key];
                        if(!descriptor) {
                            descriptor = [];
                        }
                        descriptor[index] = angular.copy($scope.input);
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
