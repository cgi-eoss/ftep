/**
 * @ngdoc function
 * @name ftepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('WorkspaceCtrl', [ '$scope', 'JobService', 'SystematicService', 'ProductService', 'MapService', 'CommonService', 'SearchService', '$location', '$mdDialog', function ($scope, JobService, SystematicService, ProductService, MapService, CommonService, SearchService, $location, $mdDialog) {

        $scope.serviceParams = ProductService.params.explorer;
        $scope.runModes = ProductService.serviceRunModes;
        $scope.inputsValid = true;
        $scope.inputValidityMap = new Map();
        $scope.launchButtonTooltipText = "Launch";

        $scope.searchForm = {
            config: {},
            api: {},
            data: {
                catalogue: 'SATELLITE'
            }
        }

        SearchService.getSearchParameters().then(function(data){

            delete data.productDate;
            delete data.catalogue;

            var config = {
                productDateStart: {
                    type: 'date',
                    defaultValue: '0',
                    description: 'UTC',
                    title: 'Product start date'
                },
                productDateEnd: {
                    type: 'date',
                    defaultValue: '0',
                    description: 'UTC',
                    optional: true,
                    format: 'YYYY-MM-DD[T23:59:59Z]',
                    title: 'Product end date'
                }
            }

            delete data.catalogue;

            $scope.searchForm.config = Object.assign(config, data);
        });

        // Get page path to save the search params in the SearchService when changing tabs
        var page = $location.path().replace(/\W/g,'') ? $location.path().replace('/','') : 'explorer';
        $scope.serviceParams = {
            selectedService: undefined,
            config: {}
        };

        $scope.updateInputValidity = function() {
            $scope.inputsValid = !Array.from($scope.inputValidityMap.values()).includes(false);
        };

        $scope.updateLaunchButtonTooltipText = function() {
            // If a field exists that fails the input validation check, display the error in the launch button tooltip
            for (var fieldDesc of $scope.inputValidityMap.keys()) {
                if ($scope.inputValidityMap.get(fieldDesc) === false) {
                    $scope.launchButtonTooltipText = `Incorrect input count for field \"${fieldDesc.title}\" (expected: min ${fieldDesc.minOccurs}, max ${fieldDesc.maxOccurs})`;
                    return;
                }
            }
            $scope.launchButtonTooltipText = "Launch";
        };

        if (ProductService.params[page].selectedService) {
            $scope.serviceParams.selectedService = ProductService.params[page].selectedService;
        }
        if (ProductService.params[page].savedServiceConfig) {
            $scope.serviceParams.config = ProductService.params[page].savedServiceConfig;
        }
        if (ProductService.params[page].savedInputValidityMap) {
            $scope.inputValidityMap = ProductService.params[page].savedInputValidityMap;
            $scope.updateInputValidity();
            $scope.updateLaunchButtonTooltipText();
        }

        // Adds parallel parameters list into the correct format (key: true)
        function getParallelParams(params) {
            var paramList = [];
            if (params) {
                for (var key in params) {
                    paramList[params[key]] = true;
                }
            }
            return paramList;
        }

        // Update service config values on new service selection
        $scope.$on('update.selectedService', function(event, config, advancedMode, systematicParameter) {
            ProductService.getService(config.service).then(function(detailedService){
                $scope.serviceParams.selectedService = detailedService;
                $scope.serviceParams.config = {
                    inputValues: config.inputs ? config.inputs : {},
                    label: config.label,
                    parallelParameters: getParallelParams(config.parallelParameters),
                    advancedMode: advancedMode
                };
            });
            // Reset input validation data
            $scope.inputsValid = true;
            $scope.inputValidityMap = new Map();
            $scope.launchButtonTooltipText = "Launch";
        });

        $scope.onRunModeChange = function() {
            if ($scope.serviceParams.runMode === $scope.runModes.SYSTEMATIC.id) {
                $scope.serviceParams.systematicParameter = $scope.serviceParams.selectedService ? $scope.serviceParams.selectedService.serviceDescriptor.dataInputs[0].id : null;
            }
            else {
                delete $scope.serviceParams.systematicParameter;
            }
        }

        // Get default value for a given field
        $scope.getDefaultValue = function(fieldDesc) {
            var fieldValue = $scope.serviceParams.config.inputValues[fieldDesc.id];
            return fieldValue ? fieldValue : fieldDesc.defaultAttrs.value;
        };

        $scope.launchProcessing = function($event) {
            var iparams={};

            for (var key in $scope.serviceParams.config.inputValues) {
                var value = $scope.serviceParams.config.inputValues[key];
                if (value === undefined) {
                    value = '';
                }
                iparams[key] = [value];
            }

            var jobParams = null;

            // If easy mode doesn't exist run advanced mode
            if (!$scope.serviceParams.selectedService.easyModeServiceDescriptor || !$scope.serviceParams.selectedService.easyModeServiceDescriptor.dataInputs) {
                $scope.serviceParams.config.advancedMode = true;
            }

            if (!$scope.serviceParams.config.advancedMode) {
                jobParams = ProductService.generateEasyJobConfig(
                    $scope.serviceParams.config.inputValues,
                    $scope.serviceParams.selectedService.easyModeParameterTemplate,
                    $scope.serviceParams.selectedService._links.self.href,
                    $scope.serviceParams.config.label,
                    $scope.serviceParams.config.parallelParameters,
                    false
                );
            } else {
                jobParams = {
                    'service': $scope.serviceParams.selectedService._links.self.href,
                    'label' : $scope.serviceParams.config.label,
                    'inputs' : ProductService.formatInputs(angular.copy($scope.serviceParams.config.inputValues)),
                    'parallelParameters' : ProductService.formatParallelInputs($scope.serviceParams.config.parallelParameters),
                    'systematicParameter' : null,
                    'searchParameters' : [ ]
                };
            }

            if ($scope.serviceParams.runMode === $scope.runModes.SYSTEMATIC.id) {
                delete iparams[$scope.serviceParams.systematicParameter];

                var searchParams = $scope.searchForm.api.getFormData();
                searchParams.catalogue = 'SATELLITE';

                if (CommonService.coinsDisabled) {
                    $scope.displayTab($scope.bottomNavTabs.JOBS, false);
                    SystematicService.launchSystematicProcessing($scope.serviceParams.selectedService, $scope.serviceParams.systematicParameter, iparams, searchParams, $scope.serviceParams.config.label).then(function () {
                        JobService.refreshJobs('explorer', 'Create');
                    });
                }
                else {
                    SystematicService.estimateMonthlyCost($scope.serviceParams.selectedService, $scope.serviceParams.systematicParameter, iparams, searchParams).then(function(estimation) {
                        var currency = ( estimation.estimatedCost === 1 ? 'coin' : 'coins' );
                        CommonService.confirm($event, 'This job will approximately cost ' + estimation.estimatedCost + ' ' + currency + ' per month.' +
                                '\nAre you sure you want to continue?').then(function (confirmed) {
                            if (confirmed === false) {
                                return;
                            }
                            $scope.displayTab($scope.bottomNavTabs.JOBS, false);

                            SystematicService.launchSystematicProcessing($scope.serviceParams.selectedService, $scope.serviceParams.systematicParameter, iparams, searchParams, $scope.serviceParams.config.label).then(function () {
                                JobService.refreshJobs('explorer', 'Create');
                            });
                        });
                    });
                }
            }
            else {
                if (CommonService.coinsDisabled) {
                    JobService.createJobConfig(jobParams).then(function(jobConfig) {
                        JobService.broadcastNewjob();
                        $scope.displayTab($scope.bottomNavTabs.JOBS);
                        JobService.launchJob(jobConfig, $scope.serviceParams.selectedService).then(function () {
                            JobService.refreshJobs('explorer', 'Create');
                        }, function(error) {
                            $mdDialog.show(
                                $mdDialog.alert()
                                    .clickOutsideToClose(true)
                                    .title('Launch failure')
                                    .textContent(error.body.replaceAll('"', ''))
                                    .ariaLabel('Launch failure')
                                    .ok('OK')
                            );
                        });
                    });
                }
                else {
                    JobService.createJobConfig(jobParams).then(function(jobConfig) {
                        JobService.estimateJob(jobConfig, $event).then(function(estimation) {
                            var currency = estimation.estimatedCost === 1 ? 'coin' : 'coins';
                            CommonService.confirm($event, 'This job will cost ' + estimation.estimatedCost + ' ' + currency + '.' +
                                '\nAre you sure you want to continue?').then(function (confirmed) {
                                if (confirmed === false) {
                                    return;
                                }
                                JobService.broadcastNewjob();
                                $scope.displayTab($scope.bottomNavTabs.JOBS);
                                JobService.launchJob(jobConfig, $scope.serviceParams.selectedService).then(function () {
                                    JobService.refreshJobs('explorer', 'Create');
                                });
                            });
                        },
                        function (error) {
                            if (error && error.estimatedCost) {
                                CommonService.infoBulletin($event, 'The cost of this job exceeds your balance. This job cannot be run.' +
                                    '\nYour balance: ' + error.currentWalletBalance + '\nCost estimation: ' + error.estimatedCost);
                            } else if (error) {
                                CommonService.infoBulletin($event, error);
                            } else {
                                CommonService.infoBulletin($event, 'Error occurred. Could not get Job cost estimation.');
                            }
                        });
                    });
                }
            }
        };

        $scope.pastePolygon = function(identifier){
            $scope.serviceParams.config.inputValues[identifier] = MapService.getPolygonWkt();
            // For systematic processings, also assign the polygon to the search parameters
            if ($scope.serviceParams.runMode === $scope.runModes.SYSTEMATIC.id) {
                $scope.searchForm.data[identifier] = MapService.getPolygonWkt();
            }
        };

        function addValue(fieldId, file) {
            var storedValue = $scope.serviceParams.config.inputValues[fieldId];
            if (!storedValue) {
                storedValue = [];
            }
            if (storedValue.indexOf(file)  < 0) {
                storedValue.push(file);
            }
            $scope.serviceParams.config.inputValues[fieldId] = angular.copy(storedValue);
        }

        $scope.addItem = function (fieldId, file) {
            addValue(fieldId, file);
        };

        $scope.onDrop = function(item, fieldId) {
            var files = [];

            switch(item.type) {
                case 'outputs':
                    for (let i = 0; i < item.selectedOutputs.length; i++) {
                        files.push(item.selectedOutputs[i]._links.ftep.href);
                    }
                    break;
                case 'results':
                    for (let i = 0; i < item.selectedItems.length; i++) {
                        files.push(item.selectedItems[i].properties._links.ftep.href);
                    }
                    break;
                case 'databasket':
                    files.push(item.basket._links.ftep.href);
                    break;
                case 'basketItems':
                    for (let i = 0; i < item.selectedItems.length; i++) {
                        files.push(item.selectedItems[i]._links.ftep.href);
                    }
                    break;
                default:
                    break;
            }

            for (let i = 0; i < files.length; i++) {
                addValue(fieldId, files[i]);
            }
        };

        $scope.removeSelectedItem = function(fieldId, item) {
            var index = $scope.serviceParams.config.inputValues[fieldId].indexOf(item);
            $scope.serviceParams.config.inputValues[fieldId].splice(index, 1);

            // Way to force the DOM to update
            $scope.serviceParams.config.inputValues[fieldId] = angular.copy($scope.serviceParams.config.inputValues[fieldId]);
        };

        $scope.validateInputCount = function(fieldDesc) {

            // Update the corresponding value in the input validity map
            if ($scope.serviceParams.config.inputValues[fieldDesc.id]) {
                $scope.fieldInputCount = $scope.serviceParams.config.inputValues[fieldDesc.id].length;
            } else {
                // Field value not yet set, only the parallel parameters value changed
                $scope.fieldInputCount = 0;
            }

            if (fieldDesc.parallelParameter && $scope.serviceParams.config.parallelParameters[fieldDesc.id]) {
                // If parallel parameters is selected for the field, ignore maxOccurs for input validation
                $scope.inputValidityMap.set(fieldDesc, $scope.fieldInputCount >= fieldDesc.minOccurs);
            } else {
                $scope.inputValidityMap.set(fieldDesc, $scope.fieldInputCount >= fieldDesc.minOccurs && $scope.fieldInputCount <= fieldDesc.maxOccurs);
            }

            // Update the total form input validity
            $scope.updateInputValidity();

            // Update the launch button text if necessary
            $scope.updateLaunchButtonTooltipText();
        };

        // Save the active service configuration when changing tabs so it can be reaccessed
        $scope.$on('$destroy', function() {
            ProductService.params[page].selectedService = $scope.serviceParams.selectedService;
            ProductService.params[page].savedServiceConfig = $scope.serviceParams.config;
            ProductService.params[page].savedInputValidityMap = $scope.inputValidityMap;
        });

    }]);
});
