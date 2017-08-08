/**
 * @ngdoc function
 * @name ftepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('WorkspaceCtrl', [ '$scope', 'JobService', 'ProductService', 'MapService', 'CommonService', function ($scope, JobService, ProductService, MapService, CommonService) {

        $scope.serviceParams = ProductService.params.explorer;
        $scope.isWorkspaceLoading = false;

        $scope.$on('update.selectedService', function(event, service, inputs, label) {
            $scope.isWorkspaceLoading = true;
            $scope.serviceParams.inputValues = {};
            $scope.serviceParams.label = label;
            $scope.serviceParams.dropLists = {};
            if(inputs){
                for (var key in inputs) {
                    $scope.serviceParams.inputValues[key] = inputs[key][0]; //First value is the actual input

                    //if value has links in it, add also to dropList to show file chips
                    if(inputs[key][0].indexOf('://') > -1){
                        var list = inputs[key][0].split(',');
                        $scope.serviceParams.dropLists[key] = [];
                        for(var index in list){
                            $scope.serviceParams.dropLists[key].push({ link: list[index] });
                        }
                    }
                }
            }

            ProductService.getService(service).then(function(detailedService){
                $scope.serviceParams.selectedService = detailedService;
                $scope.isWorkspaceLoading = false;
            });
        });

        $scope.getDefaultValue = function(fieldDesc){
            return $scope.serviceParams.inputValues[fieldDesc.id] ? $scope.serviceParams.inputValues[fieldDesc.id] : fieldDesc.defaultAttrs.value;
        };

        $scope.launchProcessing = function($event) {
            var iparams={};

            for(var key in $scope.serviceParams.inputValues){
                var value = $scope.serviceParams.inputValues[key];
                if(value === undefined){
                    value = '';
                }
                iparams[key] = [value];
            }

            JobService.createJobConfig($scope.serviceParams.selectedService, iparams, $scope.serviceParams.label).then(function(jobConfig){
                JobService.estimateJob(jobConfig, $event).then(function(estimation){

                    var currency = ( estimation.estimatedCost === 1 ? 'coin' : 'coins' );
                    CommonService.confirm($event, 'This job will cost ' + estimation.estimatedCost + ' ' + currency + '.' +
                            '\nAre you sure you want to continue?').then(function (confirmed) {
                        if (confirmed === false) {
                            return;
                        }
                        $scope.displayTab($scope.bottomNavTabs.JOBS, false);
                        JobService.launchJob(jobConfig, $scope.serviceParams.selectedService, 'explorer').then(function () {
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

        /** DRAG-AND-DROP FILES TO THE INPUT FIELD **/
        $scope.onDrop = function(dropObject, fieldId) {
            if($scope.serviceParams.dropLists[fieldId] === undefined){
                $scope.serviceParams.dropLists[fieldId] = [];
            }

            var file = {};

            if(dropObject && dropObject.type === 'outputs') {
                for(var i = 0; i < dropObject.selectedOutputs.length; i++){
                    file = {
                        name: dropObject.selectedOutputs[i]._links.ftep.href,
                        link: dropObject.selectedOutputs[i]._links.ftep.href,
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
                        name: dropObject.selectedItems[j].properties._links.ftep.href,
                        link: dropObject.selectedItems[j].properties._links.ftep.href,
                        start: dropObject.selectedItems[j].properties.extraParams.ftepStartTime,
                        stop: dropObject.selectedItems[j].properties.extraParams.ftepEndTime,
                        bytes: dropObject.selectedItems[j].properties.filesize
                    };
                    if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                        $scope.serviceParams.dropLists[fieldId].push(file);
                    }
                }
                setFilesInputString(fieldId);
            }
            else if(dropObject && dropObject.type === 'databasket') {
                file = {
                    name: "Databasket: " + dropObject.basket.name,
                    link: dropObject.basket._links.ftep.href
                };
                if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                    $scope.serviceParams.dropLists[fieldId].push(file);
                }
                setFilesInputString(fieldId);
                return true;
            }
            else if(dropObject && dropObject.type === 'basketItems') {
                for(var k = 0; k < dropObject.selectedItems.length; k++) {
                    file = {
                        name: dropObject.selectedItems[k]._links.ftep.href,
                        link: dropObject.selectedItems[k]._links.ftep.href
                    };
                    if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                        $scope.serviceParams.dropLists[fieldId].push(file);
                    }
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

        $scope.updateDropList = function(fieldId){
            if($scope.serviceParams.inputValues[fieldId] && $scope.serviceParams.inputValues[fieldId].indexOf('://') > -1) {
                var csvList = $scope.serviceParams.inputValues[fieldId].split(',');
                if($scope.serviceParams.dropLists[fieldId] === undefined){
                    $scope.serviceParams.dropLists[fieldId] = [];
                }
                var newDropList = [];
                for(var index in csvList){
                    var exists = false;
                    for(var i in $scope.serviceParams.dropLists[fieldId]){
                        if($scope.serviceParams.dropLists[fieldId][i].link === csvList[index]){
                            newDropList.push($scope.serviceParams.dropLists[fieldId][i]);
                            exists = true;
                        }
                    }
                    if(!exists && csvList[index] !== ''){
                        newDropList.push({link: csvList[index]});
                    }
                }
                $scope.serviceParams.dropLists[fieldId] = newDropList;
            } else {
                $scope.serviceParams.dropLists[fieldId] = undefined;
            }
        };

    }]);
});
