/**
 * @ngdoc function
 * @name ftepApp.controller:ResultsCtrl
 * @description
 * # ResultsCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('ResultsCtrl',
            [ '$scope', '$rootScope', '$anchorScroll', '$timeout', 'CommonService', 'GeoService', 'TabService',
                  function($scope, $rootScope, $anchorScroll, $timeout, CommonService, GeoService, TabService) {

                $scope.resultPaging = GeoService.pagingData;
                $scope.spinner = GeoService.spinner;
                $scope.resultParams = GeoService.params;

                $scope.getPagingValues = function(resultPaging) {
                    var start = (resultPaging.currentPage * resultPaging.pageSize) - resultPaging.pageSize + 1;
                    var end = resultPaging.currentPage * resultPaging.pageSize;
                    return "Showing " + start + " - " + end + " of " + resultPaging.total;
                };

                $scope.$on('update.geoResults', function(event, results) {
                    $scope.spinner.loading = false;
                    setResults(results);
                    TabService.activeBottomNav = TabService.getBottomNavTabs().RESULTS;
                    TabService.resultTab.nameExtention = GeoService.getResultsNameExtention();
                    $scope.resultParams.selectedResultItems = [];
                    scrollResults();
                });

                function scrollResults(anchorId){
                    if(anchorId){
                        $timeout(function () {
                            $anchorScroll(anchorId);
                        }, 50);
                    }
                    else{ // scroll to top
                        $timeout(function () {
                            if(document.getElementById('resultDiv')){
                                document.getElementById('resultDiv').scrollTop = 0;
                            }
                        }, 50);
                    }
                }

                function setResults(results){
                    if(results && results.length >0){
                        if(results[0].results.totalResults > 0){
                            $scope.geoResults = results;
                        }
                        else{
                            delete $scope.geoResults;
                        }
                        $scope.resultPaging.total = results[0].results.totalResults;

                        $scope.pageFrom = $scope.resultPaging.currentPage * $scope.resultPaging.pageSize - $scope.resultPaging.pageSize + 1;
                        $scope.pageTo = $scope.resultPaging.currentPage * $scope.resultPaging.pageSize;
                        if ($scope.pageTo > $scope.resultPaging.total) {
                            $scope.pageTo = $scope.resultPaging.total;
                        }
                    }
                    else{
                        delete $scope.geoResults;
                        $scope.resultPaging.currentPage = 1;
                        $scope.resultPaging.total = 0;
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

                $scope.$on('map.item.toggled', function(event, items) {
                    scrollResults();
                    $scope.resultParams.selectedResultItems = items;
                    if(items && items.length > 0){
                        scrollResults(items[items.length-1].identifier);
                    }
                });

                $scope.toggleSelection = function(item, fromMap) {
                        if(item){
                            var index = $scope.resultParams.selectedResultItems.indexOf(item);

                            if (index < 0) {
                                $scope.resultParams.selectedResultItems.push(item);
                                if(fromMap){
                                    scrollResults(item.identifier);
                                }
                            } else {
                                $scope.resultParams.selectedResultItems.splice(index, 1);
                            }
                            if(fromMap == undefined){
                                $rootScope.$broadcast('results.item.selected', item, index < 0);
                            }
                        }
                };

                $scope.clearSelection = function() {
                    $scope.resultParams.selectedResultItems = [];
                    $rootScope.$broadcast('results.select.all', false);
                };

                $scope.selectAll = function() {
                    $scope.resultParams.selectedResultItems = [];
                    for(var i = 0; i < $scope.geoResults.length; i++) {
                        if($scope.geoResults[i].results != null && $scope.geoResults[i].results.entities.length > 0){
                            var list = $scope.geoResults[i].results.entities.slice();
                            $scope.resultParams.selectedResultItems = $scope.resultParams.selectedResultItems.concat(list);
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
                                if($scope.resultParams.selectedResultItems.indexOf(list[e]) < 0){
                                    newSelection.push(list[e]);
                                }
                            }
                        }
                    }
                    $scope.resultParams.selectedResultItems = newSelection;
                    $rootScope.$broadcast('results.invert', $scope.resultParams.selectedResultItems);
                };

                $scope.getSelectedItemsLinks = function(item){
                    if($scope.resultParams.selectedResultItems.indexOf(item) < 0){
                        $scope.toggleSelection(item);
                    }
                    var links = $scope.resultParams.selectedResultItems[0].link;
                    for(var i = 1; i < $scope.resultParams.selectedResultItems.length; i++){
                        links = links.concat(',', $scope.resultParams.selectedResultItems[i].link);
                    }

                    return links;
                };

    } ]);
});
