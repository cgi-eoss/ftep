/**
 * @ngdoc function
 * @name ftepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules', 'hgn!zoo-client/assets/tpl/ftep_describe_process'], function (ftepmodules, tpl_describeProcess) {
    'use strict';

    ftepmodules.controller('WorkspaceCtrl', [ '$scope', '$rootScope', '$mdDialog', '$sce', '$document', 'WpsService', 'JobService', 'ProductService',
                                function ($scope, $rootScope, $mdDialog, $sce, $document, WpsService, JobService, ProductService) {

        $scope.selectedService;
        $scope.serviceDescription;
        $scope.isWpsLoading = false;
        $scope.info;
        $scope.outputValues = {};
        $scope.dropList = { items: []};
        $scope.inputValues = {};
        $scope.dropLists = {};

        var polygonWkt = undefined;
        $scope.$on('update.searchPolygonWkt', function(event, wkt) {
            polygonWkt = wkt;
        });

        $scope.pastePolygon = function(identifier){
            $scope.inputValues[identifier] = polygonWkt;
        }

        $scope.onDrop = function(items, fieldId) {
            if($scope.dropLists[fieldId] === undefined){
                $scope.dropLists[fieldId] = [];
            }
            if(items) {
                var itemsList = items.split(",");
                for(var i in itemsList){
                    if($scope.dropLists[fieldId].indexOf(itemsList[i]) < 0){
                        $scope.dropLists[fieldId].push(itemsList[i]);
                    }
                }
                $scope.inputValues[fieldId] = $scope.dropLists[fieldId].toString();
                return true;
            }
            else {
                return false;
            }
        };

        $scope.removeSelectedItem = function(fieldId, item){
            var index = $scope.dropLists[fieldId].indexOf(item);
            $scope.dropLists[fieldId].splice(index, 1);
            $scope.inputValues[fieldId] = $scope.dropLists[fieldId].toString();
        };

        $scope.getInputType = function(fieldDesc){
            if(fieldDesc.LiteralData && fieldDesc.LiteralData.DataType.__text === 'integer'){
                return 'number';
            }
            return (fieldDesc.LiteralData && fieldDesc.LiteralData.DataType) ? fieldDesc.LiteralData.DataType.__text : 'string';
        };

        $scope.$on('update.selectedService', function(event, service) {
            updateService(service);
        });

        function updateService(service){
            $scope.outputValues = {};
            $scope.inputValues = {};
            $scope.dropLists = {};
            delete $scope.info;

            if($scope.selectedService === undefined || $scope.selectedService.id != service.id){
                $scope.selectedService = service;
                $scope.isWpsLoading = true;

                WpsService.getDescription(service.attributes.name).then(function(data){

                    data["Identifier1"]=data.ProcessDescriptions.ProcessDescription.Identifier.__text.replace(/\./g,"__");
                    data.ProcessDescriptions.ProcessDescription.Identifier1=data.ProcessDescriptions.ProcessDescription.Identifier.__text.replace(/\./g,"__");
                    for(var i in data.ProcessDescriptions.ProcessDescription.DataInputs.Input){
                        if(data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i]._minOccurs === "0") {
                            data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i].optional=true;
                        } else {
                            data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i].optional=false;
                        }
                    }
                    for(var j in data.ProcessDescriptions.ProcessDescription.ProcessOutputs.Output){
                        var fieldDesc = data.ProcessDescriptions.ProcessDescription.ProcessOutputs.Output[j];
                        if(fieldDesc.ComplexOutput && fieldDesc.ComplexOutput.Default){
                            $scope.outputValues[fieldDesc.Identifier] = fieldDesc.ComplexOutput.Default.Format;
                        }
                    }

                    $scope.serviceDescription = data.ProcessDescriptions.ProcessDescription;
                    $scope.isWpsLoading = false;
                });
            }
        }

        $scope.$on('rerun.service', function(event, inputs, serviceId){
            var service = ProductService.getServiceById(serviceId);
            updateService(service);
            $scope.inputValues = inputs;
        });

        $scope.launchProcessing = function() {
            console.log('Process..');
            var aProcess = $scope.selectedService.attributes.name;
            notify('Running '+aProcess+' service..');
            var iparams=[];

            for(var key in $scope.inputValues){
                console.log(key);
                iparams.push({
                    identifier: key,
                    value: $scope.inputValues[key],
                    dataType: "string"
                });
            }

            var oparams=[];
            for(var keyout in $scope.outputValues){
                oparams.push({
                    identifier: keyout,
                    mimeType: $scope.outputValues[keyout],
                    asReference: "true"
                });
            }

            console.log("----In ----");
            console.log(iparams);
            console.log("----Out ----");
            console.log(oparams);

            WpsService.execute(aProcess, iparams, oparams).then(function(data){
               JobService.getJobs(true);
            }, function(error) {
                notify(error);
            });
        };

        function notify(text){
            $scope.info = text;
        }

        $scope.getShortName = function(label){
            var from = (label.length - 8 > 0) ? label.length - 8: 0;
            var str = label.substr(from);
            return '..'.concat(str);
        };

    }]);
});
