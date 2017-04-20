/**
 * @ngdoc function
 * @name ftepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('WorkspaceCtrl', [ '$scope', '$rootScope', '$sce', 'JobService', 'ProductService', 'MapService', 'CommonService',
                                function ($scope, $rootScope, $sce, JobService, ProductService, MapService, CommonService) {

        $scope.serviceParams = ProductService.params.explorer;
        $scope.isWorkspaceLoading = false;

        $scope.$on('update.selectedService', function(event, serviceId, inputs) {
            $scope.isWorkspaceLoading = true;
            $scope.serviceParams.inputValues = {};
            $scope.serviceParams.dropLists = {};
            if(inputs){
                $scope.serviceParams.inputValues = inputs;
            }

            ProductService.getService(serviceId).then(function(detailedService){
                $scope.serviceParams.selectedService = detailedService;
                $scope.isWorkspaceLoading = false;
            });
        });

        $scope.getDefaultValue = function(fieldDesc){
            return $scope.serviceParams.inputValues[fieldDesc.id] ? $scope.serviceParams.inputValues[fieldDesc.id] : fieldDesc.defaultAttrs.value;
        };

        $scope.launchProcessing = function() {
            var iparams={};

            for(var key in $scope.serviceParams.inputValues){
                if ($scope.serviceParams.inputValues.hasOwnProperty(key)) {
                    iparams[key] = [$scope.serviceParams.inputValues[key]];
                }
            }

            JobService.launchJob($scope.serviceParams.selectedService, iparams).then(function(data){
               JobService.refreshJobs("explorer");
            });
        };

        $scope.pastePolygon = function(identifier){
            $scope.serviceParams.inputValues[identifier] = MapService.getPolygonWkt();
        };

        /** DRAG-AND-DROP FILES TO THE INPUT FIELD **/
        $scope.onDrop = function(items, fieldId) {
            if($scope.serviceParams.dropLists[fieldId] === undefined){
                $scope.serviceParams.dropLists[fieldId] = [];
            }
            if(items) {
                var pathStr = getPaths($scope.serviceParams.dropLists[fieldId]);
                for(var i = 0; i < items.length; i++){
                    var path = getFilePath(items[i]);

                    if(pathStr.indexOf(path) < 0){
                        $scope.serviceParams.dropLists[fieldId].push(items[i]);
                    }
                }
                $scope.serviceParams.inputValues[fieldId] = getPaths($scope.serviceParams.dropLists[fieldId]);
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

        $scope.getFileName = function(file) {
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
        };

        $scope.removeSelectedItem = function(fieldId, item){
            var index = $scope.serviceParams.dropLists[fieldId].indexOf(item);
            $scope.serviceParams.dropLists[fieldId].splice(index, 1);
            $scope.serviceParams.inputValues[fieldId] = getPaths($scope.serviceParams.dropLists[fieldId]);
        };

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
        /** END OF DRAG-AND-DROP FILES TO THE INPUT FIELD **/


    }]);
});
