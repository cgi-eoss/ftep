/**
 * @ngdoc function
 * @name ftepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules', 'hgn!zoo-client/assets/tpl/ftep_describe_process'], function (ftepmodules, tpl_describeProcess) {
    'use strict';

    ftepmodules.controller('WorkspaceCtrl', [ '$scope', '$rootScope', '$sce', '$document', 'WpsService', 'JobService',
                                              'ProductService', 'MapService', 'CommonService',
                                function ($scope, $rootScope, $sce, $document, WpsService, JobService, ProductService, MapService,
                                        CommonService) {

        $scope.serviceParams = ProductService.params.explorer;
        $scope.serviceDescription = undefined;
        $scope.isWpsLoading = false;
        $scope.info = undefined;
        $scope.outputValues = {};
        $scope.inputValues = {};
        $scope.dropLists = {};

        $scope.pastePolygon = function(identifier){
            console.log(MapService.getPolygonWkt());
            $scope.inputValues[identifier] = MapService.getPolygonWkt();
        };

        $scope.onDrop = function(items, fieldId) {
            if($scope.dropLists[fieldId] === undefined){
                $scope.dropLists[fieldId] = [];
            }
            if(items) {
                var pathStr = getPaths($scope.dropLists[fieldId]);
                for(var i = 0; i < items.length; i++){
                    var path = getFilePath(items[i]);

                    if(pathStr.indexOf(path) < 0){
                        $scope.dropLists[fieldId].push(items[i]);
                    }
                }
                $scope.inputValues[fieldId] = getPaths($scope.dropLists[fieldId]);
                return true;
            }
            else {
                return false;
            }
        };

        function getPaths(files){
            var str = '';
            for(var i = 0; i < files.length; i++){
                var path = getFilePath(files[i]);
                str = str.concat(',', path);
            }
            return str.substr(1);
        }

        function getFilePath(file){
            var path = '';
            if(file.type === 'files'){
                path = file.attributes.properties.details.file.path;
            }
            else if(file.type === 'file'){
                path = CommonService.getOutputLink(file.attributes.link);
            }
            else{
                path = file.link;
            }
            return path;
        }

        $scope.removeSelectedItem = function(fieldId, item){
            var index = $scope.dropLists[fieldId].indexOf(item);
            $scope.dropLists[fieldId].splice(index, 1);
            $scope.inputValues[fieldId] = getPaths($scope.dropLists[fieldId]);
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

            $scope.serviceParams.selectedService = service;
            $scope.isWpsLoading = true;

            WpsService.getDescription(service.attributes.name).then(function(data){

                data["Identifier1"]=data.ProcessDescriptions.ProcessDescription.Identifier.__text.replace(/\./g,"__");
                data.ProcessDescriptions.ProcessDescription.Identifier1=data.ProcessDescriptions.ProcessDescription.Identifier.__text.replace(/\./g,"__");
                for(var i = 0; i < data.ProcessDescriptions.ProcessDescription.DataInputs.Input.length; i++){
                    if(data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i]._minOccurs === "0") {
                        data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i].optional=true;
                    } else {
                        data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i].optional=false;
                    }
                }
                for(var j = 0; j < data.ProcessDescriptions.ProcessDescription.ProcessOutputs.Output.length; j++){
                    var fieldDesc = data.ProcessDescriptions.ProcessDescription.ProcessOutputs.Output[j];
                    if(fieldDesc.ComplexOutput && fieldDesc.ComplexOutput.Default){
                        $scope.outputValues[fieldDesc.Identifier] = fieldDesc.ComplexOutput.Default.Format;
                    }
                }

                $scope.serviceDescription = data.ProcessDescriptions.ProcessDescription;
                $scope.isWpsLoading = false;
            });
        }

        $scope.$on('rerun.service', function(event, inputs, serviceId){
            var service = ProductService.getServiceById(serviceId);
            updateService(service);
            $scope.inputValues = inputs;
        });

        $scope.launchProcessing = function() {
            console.log('Process..');
            var aProcess = $scope.serviceParams.selectedService.attributes.name;
            notify('Running '+aProcess+' service..');
            var iparams=[];

            for(var key in $scope.inputValues){
                if ($scope.inputValues.hasOwnProperty(key)) {
                    iparams.push({
                        identifier: key,
                        value: $scope.inputValues[key],
                        dataType: "string"
                    });
                }
            }

            var oparams=[];
            for(var keyout in $scope.outputValues){
                if($scope.outputValues.hasOwnProperty(key)){
                    oparams.push({
                        identifier: keyout,
                        mimeType: $scope.outputValues[keyout],
                        asReference: "true"
                    });
                }
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

        $scope.getShortName = function(file){
            var name = getFileName(file);
            var from = (name.length - 18 > 0) ? name.length - 18: 0;
            var str = name.substr(from);
            return '..'.concat(str);
        };

        function getFileName(file){
            var name = '';
            if(file.type === 'files'){
                name = file.attributes.name;
            }
            else if(file.type === 'file'){
                name = file.attributes.fname;
            }
            else{
                name = file.identifier;
            }
            return name;
        }

        var popover = {};
        $scope.getDroppedFilePopover = function (file) {

            var name, start, stop, bytes;
            if(file.type === 'files'){
                name = file.attributes.name;
                start = file.attributes.properties.start;
                stop = file.attributes.properties.stop;
                bytes = file.attributes.properties.size;
            }
            else if(file.type === 'file'){
                name = file.attributes.fname;
                start = '';
                stop = '';
                bytes = '';
            }
            else {
                name = file.identifier;
                start = file.start;
                stop = file.stop;
                bytes = file.size;
            }

            var sizeInGb = '';
            if (isNaN(bytes) || bytes < 1) {
                sizeInGb = bytes;
            } else {
                sizeInGb = (bytes / 1073741824).toFixed(2) + ' GB';
            }

            var html =
                '<div>' +
                    '<div class="row">' +
                        '<div class="col-sm-2">Name:</div>' +
                        '<div class="col-sm-10">' + name + '</div>' +
                    '</div>' +
                    '<div class="row">' +
                        '<div class="col-sm-2">Start:</div>' +
                        '<div class="col-sm-10">' + start + '</div>' +
                    '</div>' +
                    '<div class="row">' +
                        '<div class="col-sm-2">End:</div>' +
                        '<div class="col-sm-10">' + stop + '</div>' +
                    '</div>' +
                    '<div class="row">' +
                        '<div class="col-sm-2">Size:</div>' +
                        '<div class="col-sm-10">' +  sizeInGb + '</div>' +
                    '</div>' +
                '</div>';
            return popover[html] || (popover[html] = $sce.trustAsHtml(html));
        };

        function setup(){
            if($scope.serviceParams.selectedService){
                updateService($scope.serviceParams.selectedService);
            }
        }
        setup();

    }]);
});
