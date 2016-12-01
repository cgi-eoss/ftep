/**
 * @ngdoc function
 * @name ftepApp.controller:BottombarCtrl
 * @description
 * # BottombarCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('BottombarCtrl', [ '$scope', '$rootScope', 'CommonService', 'JobService', 'GeoService','BasketService', function($scope, $rootScope, CommonService, JobService, GeoService, BasketService) {

                $scope.pageNumber = 1;
                $scope.resultsCurrentPage = 1;
                $scope.resultsPageSize = 0;
                $scope.resultsTotal = 0;
                $scope.spinner = GeoService.spinner;

                $scope.resultTabs = {selected: 0};
                var selectedResultItems = [];

                $scope.$on('update.geoResults', function(event, results) {
                    $scope.spinner.loading = false;
                    setResults(results);
                    $scope.resultTabs = {selected: 0};
                    selectedResultItems = [];
                });

                function setResults(results){
                    if(results && results.length >0){
                        if(results[0].results.totalResults > 0){
                            $scope.geoResults = results;
                        }
                        else{
                            delete $scope.geoResults;
                        }
                        $scope.resultsTotal = results[0].results.totalResults;
                        $scope.resultsPageSize = results[0].results.itemsPerPage;
                        $scope.resultsCurrentPage = GeoService.parameters.pageNumber;
                    }
                    else{
                        delete $scope.geoResults;
                    }
                }
                setResults(GeoService.getResultCache());

                $scope.fetchResultsPage = function(pageNumber){
                    GeoService.getGeoResults(pageNumber).then(function(data) {
                        $rootScope.$broadcast('update.geoResults', data);
                   })
                   .catch(function(fallback) {
                       GeoService.spinner.loading = false;
                   });
                };

                $scope.$on('map.item.toggled', function(event, item) {
                    $scope.toggleSelection(item, true);
                });

                $scope.toggleSelection = function(item, fromMap) {
                    if(item){
                        var index = selectedResultItems.indexOf(item);
                        if (index < 0) {
                            selectedResultItems.push(item);
                        } else {
                            selectedResultItems.splice(index, 1);
                        }
                        if(fromMap == undefined){
                            $rootScope.$broadcast('results.item.selected', item, index < 0);
                        }
                    }
                };

                $scope.isSelected = function(item) {
                    var selected = false;
                    if(selectedResultItems){
                        for(var i = 0; i < selectedResultItems.length; i++){
                            if(angular.equals(item, selectedResultItems[i])){
                                selected = true;
                                break;
                            }
                        }
                    }
                    return selected;
                };

                $scope.clearSelection = function() {
                    selectedResultItems = [];
                    $rootScope.$broadcast('results.select.all', false);
                };

                $scope.selectAll = function() {
                    selectedResultItems = [];
                    for(var i = 0; i < $scope.geoResults.length; i++) {
                        if($scope.geoResults[i].results != null && $scope.geoResults[i].results.entities.length > 0){
                            var list = $scope.geoResults[i].results.entities.slice();
                            selectedResultItems.push.apply(selectedResultItems, list);
                        }
                    }
                    $rootScope.$broadcast('results.select.all', true);
                };

                $scope.invertSelection = function() {
                    var newSelection = [];
                    for(var i = 0; i < $scope.geoResults.length; i++) {
                        if($scope.geoResults[i].results != null && $scope.geoResults[i].results.entities.length > 0){
                            var list = $scope.geoResults[i].results.entities.slice();
                            for(var e = 0; e < list.length; e++){
                                if(selectedResultItems.indexOf(list[e]) == -1){
                                    newSelection.push(list[e]);
                                }
                            }
                        }
                    }
                    selectedResultItems = [];
                    selectedResultItems.push.apply(selectedResultItems, newSelection);
                    $rootScope.$broadcast('results.invert', selectedResultItems);
                };

                $scope.getLink = function(item){
                    return CommonService.getLink(item, $scope.geoResults);
                };

                $scope.getSelectedItemsLinks = function(item){
                    if($scope.isSelected(item) == false){
                        $scope.toggleSelection(item);
                    }
                    var links = selectedResultItems[0].link;
                    for(var i = 1; i < selectedResultItems.length; i++){
                        links = links.concat(',', selectedResultItems[i].link);
                    }

                    return links;
                };

                function getKeyByValue(object, value) {
                    return Object.keys(object).find(function (key) {
                        return object[key] === value;
                    });
                  }

                /* Selected Databasket */
                $scope.selectedDatabasket = undefined;

                $scope.$on('update.databasket', function(event, basket, items) {
                    $scope.resultTabs.selected = 1;
                    $scope.selectedDatabasket = basket;
                    $scope.selectedDatabasket.items= items;
                });

                $scope.addToDatabasket = function() {
                    for (var i = 0; i < selectedResultItems.length; i++) {
                        var found = false;
                        for(var k = 0; k < $scope.selectedDatabasket.items.length; k++){
                            if(angular.equals(selectedResultItems[i], $scope.selectedDatabasket.items[k])){
                                found = true;
                                break;
                            }
                            else if($scope.selectedDatabasket.items[k].name && $scope.selectedDatabasket.items[k].name == selectedResultItems[i].identifier){
                                found = true;
                                break;
                            }
                        }
                        if(!found){
                            $scope.selectedDatabasket.items.push(selectedResultItems[i]);
                        }
                    }
                    BasketService.addBasketItems($scope.selectedDatabasket, $scope.selectedDatabasket.items);
                };

                $scope.addOutputsToDatabasket = function() {
                    for (var i = 0; i < jobSelectedOutputs.length; i++) {
                        if($scope.selectedDatabasket.items.indexOf(jobSelectedOutputs[i]) < 0){
                            $scope.selectedDatabasket.items.push(jobSelectedOutputs[i]);
                        }
                    }
                    //$scope.resultTabs.selected = 1;
                };

                $scope.clearDatabasket = function() {
                    $scope.selectedDatabasket.items = [];
                };

                $scope.removeItemFromBasket = function(item) {
                    if(item.name){
                        BasketService.removeRelation($scope.selectedDatabasket, item).then(function() {
                            removeFromBasket(item);
                        });
                    }
                    else{
                        removeFromBasket(item);
                    }
                };

                function removeFromBasket(item){
                    var i = $scope.selectedDatabasket.items.indexOf(item);
                    $scope.selectedDatabasket.items.splice(i, 1);
                }

                $scope.createNewBasket = function($event){
                    switch($scope.resultTabs.selected) {
                        case 0:
                            $scope.createDatabasketDialog($event, selectedResultItems);
                            break;
                        case 1:
                            var itemsList = $scope.selectedDatabasket ? $scope.selectedDatabasket.items : [];
                            $scope.createDatabasketDialog($event, itemsList);
                            break;
                        case 2:
                            $scope.createDatabasketDialog($event, jobSelectedOutputs);
                            break;
                    }
                };

                $scope.$on('delete.databasket', function(event, basket) {
                    if(angular.equals(basket, $scope.selectedDatabasket)){
                        delete $scope.selectedDatabasket;
                    }
                });

                $scope.getBasketItem = function(item){
                    if(item.properties){
                        return item.properties.details.file.path;
                    }
                    return '';
                };
                /* End of Selected Databasket */

                /* Selected Job */
                $scope.selectedJob = undefined;
                $scope.jobOutputs = [];
                var jobSelectedOutputs = [];

                $scope.$on('select.job', function(event, job) {
                    $scope.selectedJob = job;
                    $scope.resultTabs.selected = 2;
                    jobSelectedOutputs = [];
                    JobService.getOutputs(job.id).then(function(data){
                        $scope.jobOutputs = data;
                    });
                });

                $scope.$on('refresh.jobs', function(event, result) {
                    if($scope.selectedJob){
                        for(var i = 0; i < result.data.length; i++){
                            if($scope.selectedJob.id == result.data[i].id){
                                $scope.selectedJob = result.data[i];
                                JobService.getOutputs($scope.selectedJob.id).then(function(data){
                                    $scope.jobOutputs = data;
                                });
                            }
                        }
                    }
                });

                $scope.getJobInputs = function(job) {
                    $scope.$broadcast('rebuild:scrollbar');
                    if (job.attributes.inputs instanceof Object && Object.keys(job.attributes.inputs).length > 0) {
                        return job.attributes.inputs;
                    }
                    else{
                        return undefined;
                    }
                };

                $scope.getOutputLink = function(link){
                    return CommonService.getOutputLink(link);
                };

                $scope.getSelectedOutputFiles = function(file) {
                    if (jobSelectedOutputs.indexOf(file) < 0) {
                        jobSelectedOutputs.push(file);
                    }
                    var links = CommonService.getOutputLink(jobSelectedOutputs[0].attributes.link);
                    for(var i=1; i < jobSelectedOutputs.length; i++){
                        links = links.concat(",", CommonService.getOutputLink(jobSelectedOutputs[i].attributes.link));
                    }

                    return links;
                };

                $scope.selectOutputFile = function(item){
                    var index = jobSelectedOutputs.indexOf(item);
                    if (index < 0) {
                        jobSelectedOutputs.push(item);
                    } else {
                        jobSelectedOutputs.splice(index, 1);
                    }
                };

                $scope.selectAllOutputs = function(){
                    jobSelectedOutputs = [];
                    jobSelectedOutputs.push.apply(jobSelectedOutputs, $scope.jobOutputs);
                };

                $scope.clearOutputsSelection = function(){
                    jobSelectedOutputs = [];
                };

                $scope.invertOutputsSelection = function(){
                    var newSelection = [];
                    for(var i = 0; i < $scope.jobOutputs.length; i++) {
                        if(jobSelectedOutputs.indexOf($scope.jobOutputs[i]) < 0){
                             newSelection.push($scope.jobOutputs[i]);
                        }
                    }
                    jobSelectedOutputs = [];
                    jobSelectedOutputs.push.apply(jobSelectedOutputs, newSelection);
                };

                $scope.isOutputSelected = function(item) {
                    return jobSelectedOutputs.indexOf(item) > -1;
                };

                $scope.$on('delete.job', function(event, job) {
                    if(angular.equals(job, $scope.selectedJob)){
                        delete $scope.selectedJob;
                    }
                });

                /* End of Selected Job */

                $scope.getColor = function(status){
                    return CommonService.getColor(status);
                };

    }]);
});
