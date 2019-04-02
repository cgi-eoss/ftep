/**
 * @ngdoc function
 * @name ftepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('WorkspaceCtrl', [ '$scope', 'JobService', 'ProductService', 'MapService', 'CommonService', 'SearchService', function ($scope, JobService, ProductService, MapService, CommonService, SearchService) {

        $scope.serviceParams = ProductService.params.explorer;
        $scope.searchParams = SearchService.params;
        $scope.resultParams = $scope.searchParams.results;
        $scope.isWorkspaceLoading = false;

        $scope.$on('update.selectedService', function(event, config) {

            $scope.isWorkspaceLoading = true;
            $scope.serviceParams.inputValues = {};
            $scope.serviceParams.label = config.label;
            $scope.serviceParams.parallelParameters = config.parallelParameters;
            if(config.inputs){
                for (var key in config.inputs) {
                    $scope.serviceParams.inputValues[key] = config.inputs[key];
                }
            }

            // Converts parallelParameters into correct format for data-binding
            $scope.serviceParams.parallelParameters = [];
            if (config.parallelParameters) {
                for (var k in config.parallelParameters) {
                    let key = config.parallelParameters[k];
                    $scope.serviceParams.parallelParameters[key] = true;
                }
            }

            ProductService.getService(config.service).then(function(detailedService){
                $scope.serviceParams.selectedService = detailedService;
                $scope.isWorkspaceLoading = false;
            });
        });

        $scope.getDefaultValue = function(fieldDesc){
            return $scope.serviceParams.inputValues[fieldDesc.id] ? $scope.serviceParams.inputValues[fieldDesc.id] : fieldDesc.defaultAttrs.value;
        };

        $scope.launchProcessing = function($event) {
            $scope.requiredFields = $scope.serviceParams.selectedService.serviceDescriptor.dataInputs;

            var iparams={};
            for(var key in $scope.serviceParams.inputValues){
                var value = $scope.serviceParams.inputValues[key];
                // Grab the selection value from service-input fields (e.g. radiometric index algorithm)
                if (typeof value === 'object' && !Array.isArray(value)) {
                    value = value[0];
                }
                if(value === undefined){
                    value = '';
                }
                // All parameters should be stored in arrays
                if (Array.isArray(value)) {
                    iparams[key] = value;
                } else {
                    iparams[key] = [value];
                }

            }

            var parallelParameters = [];

            for(var k in $scope.serviceParams.parallelParameters) {
                if ($scope.serviceParams.parallelParameters[k] === true) {
                    parallelParameters.push(k);
                }
            }

            // Create job config format to parse to createJobConfig
            var jobParams = {
                "service": $scope.serviceParams.selectedService._links.self.href,
                "inputs" : iparams,
                "label" : $scope.serviceParams.label,
                "systematicParameter" : null,
                "parallelParameters" : parallelParameters,
                "searchParameters" : [ ]
            };

            JobService.createJobConfig(jobParams).then(function(jobConfig){
                JobService.estimateJob(jobConfig, $event).then(function(estimation){
                    var currency = ( estimation.estimatedCost === 1 ? 'coin' : 'coins' );
                    CommonService.confirm($event, 'This job will cost ' + estimation.estimatedCost + ' ' + currency + '.' +
                            '\nAre you sure you want to continue?').then(function (confirmed) {
                        if (confirmed === false) {
                            return;
                        }
                        JobService.broadcastNewjob();
                        $scope.displayTab($scope.bottomNavTabs.JOBS);
                        JobService.launchJob(jobConfig, $scope.serviceParams.selectedService).then(function () {
                            JobService.refreshJobs("explorer", "Create");
                        });
                    });
                },
                function (error) {
                    CommonService.infoBulletin($event, 'The cost of this job exceeds your balance. This job cannot be run.' +
                                               '\nYour balance: ' + error.currentWalletBalance + '\nCost estimation: ' + error.estimatedCost);
                });
            });
        };

        $scope.pastePolygon = function(identifier){
            $scope.serviceParams.inputValues[identifier] = MapService.getPolygonWkt();
        };

        function addValue(fieldId, file) {
            if (!$scope.serviceParams.inputValues[fieldId]) {
                $scope.serviceParams.inputValues[fieldId] = [];
            }
            if ($scope.serviceParams.inputValues[fieldId].indexOf(file)  < 0) {
                $scope.serviceParams.inputValues[fieldId].push(file);
            }
            $scope.serviceParams.inputValues[fieldId] = angular.copy($scope.serviceParams.inputValues[fieldId]);
        }

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

        $scope.addItem = function (fieldId, file) {
            addValue(fieldId, file);
        };

        $scope.removeSelectedItem = function(fieldId, item) {
            var index = $scope.serviceParams.inputValues[fieldId].indexOf(item);
            $scope.serviceParams.inputValues[fieldId].splice(index, 1);

            // Way to force the DOM to update
            $scope.serviceParams.inputValues[fieldId] = angular.copy($scope.serviceParams.inputValues[fieldId]);
        };

    }]);
});
