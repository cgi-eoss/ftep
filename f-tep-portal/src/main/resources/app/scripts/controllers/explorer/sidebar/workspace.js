/**
 * @ngdoc function
 * @name ftepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('WorkspaceCtrl', [ '$scope', '$rootScope', '$sce', '$filter', 'JobService', 'ProductService', 'MapService', 'BasketService',
                                function ($scope, $rootScope, $sce, $filter, JobService, ProductService, MapService, BasketService) {

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
               JobService.refreshJobs("explorer", "Create");
            });
        };

        $scope.pastePolygon = function(identifier){
            $scope.serviceParams.inputValues[identifier] = MapService.getPolygonWkt();
        };

        /** DRAG-AND-DROP FILES TO THE INPUT FIELD **/
        $scope.onDrop = function(dropObject, fieldId) {
            if($scope.serviceParams.dropLists[fieldId] === undefined){
                $scope.serviceParams.dropLists[fieldId] = [];
            }

            var file = {};

            if(dropObject && dropObject.type === 'outputs') {
                for(var i = 0; i < dropObject.selectedOutputs.length; i++){
                    file = {
                        name: dropObject.selectedOutputs[i],
                        link: dropObject.selectedOutputs[i],
                        start: dropObject.job.startTime,
                        stop: dropObject.job.endTime
                    };
                    if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                        $scope.serviceParams.dropLists[fieldId].push(file);
                    }
                }
                setFilesInputString(fieldId);
            }
            else if(dropObject && dropObject.type === 'results') {
                for(var j = 0; j < dropObject.selectedItems.length; j++){
                    file = {
                        name: dropObject.selectedItems[j].identifier,
                        link: dropObject.selectedItems[j].link,
                        start: dropObject.selectedItems[j].start,
                        stop: dropObject.selectedItems[j].stop,
                        bytes: dropObject.selectedItems[j].size
                    };
                    if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                        $scope.serviceParams.dropLists[fieldId].push(file);
                    }
                }
                setFilesInputString(fieldId);
            }
            else if(dropObject && dropObject.type === 'databasket') {
                BasketService.getDatabasketContents(dropObject.basket).then(function(files){
                    for(var i = 0; i < files.length; i++){
                        file = {
                            name: files[i].filename,
                            link: files[i]._links.self.href
                        };
                        if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                            $scope.serviceParams.dropLists[fieldId].push(file);
                        }
                    }
                    setFilesInputString(fieldId);
                });
                return true;
            }
            else if(dropObject && dropObject.type === 'basketItem') {
                file = {
                    name: dropObject.item.filename,
                    link: dropObject.item._links.self.href
                };
                if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                    $scope.serviceParams.dropLists[fieldId].push(file);
                }
                setFilesInputString(fieldId);
            }
            else {
                return false;
            }
            return true;
        };

        function setFilesInputString(fieldId){
            var pathsStr = '';
            for(var i = 0; i < $scope.serviceParams.dropLists[fieldId].length; i++){
                pathsStr += ',' + $scope.serviceParams.dropLists[fieldId][i].link;
            }
            $scope.serviceParams.inputValues[fieldId] = pathsStr.substring(1);
        }

        $scope.removeSelectedItem = function(fieldId, item){
            var index = $scope.serviceParams.dropLists[fieldId].indexOf(item);
            $scope.serviceParams.dropLists[fieldId].splice(index, 1);
            setFilesInputString(fieldId);
        };

        var popover = {};
        $scope.getDroppedFilePopover = function (listItem) {
            var html =
                '<div>' +
                    '<div class="row">' +
                        '<div class="col-sm-2">Name:</div>' +
                        '<div class="col-sm-10">' + listItem.name + '</div>' +
                    '</div>' +
                    '<div class="row">' +
                        '<div class="col-sm-2">Link:</div>' +
                        '<div class="col-sm-10">' + listItem.link + '</div>' +
                    '</div>';
            if(listItem.start){
                html +=
                    '<div class="row">' +
                        '<div class="col-sm-2">Start:</div>' +
                        '<div class="col-sm-10">' + $filter('formatDateTime')(listItem.start) + '</div>' +
                    '</div>';
            }

            if(listItem.stop){
                html +=
                    '<div class="row">' +
                        '<div class="col-sm-2">End:</div>' +
                        '<div class="col-sm-10">' + $filter('formatDateTime')(listItem.stop) + '</div>' +
                    '</div>';
            }

            if(listItem.bytes){
                html +=
                    '<div class="row">' +
                        '<div class="col-sm-2">Size:</div>' +
                        '<div class="col-sm-10">' +  $filter('bytesToGB')(listItem.bytes) + '</div>' +
                    '</div>';
            }

            html += '</div>';
            return popover[html] || (popover[html] = $sce.trustAsHtml(html));
        };
        /** END OF DRAG-AND-DROP FILES TO THE INPUT FIELD **/


    }]);
});
