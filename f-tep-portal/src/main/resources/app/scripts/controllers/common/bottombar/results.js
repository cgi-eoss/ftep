/**
 * @ngdoc function
 * @name ftepApp.controller:ResultsCtrl
 * @description
 * # ResultsCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ResultsCtrl', [ '$scope', '$rootScope', '$anchorScroll', '$timeout', 'GeoService', 'TabService', function($scope, $rootScope, $anchorScroll, $timeout, GeoService, TabService) {

        $scope.resultPaging = GeoService.pagingData;
        $scope.spinner = GeoService.spinner;
        $scope.resultParams = GeoService.params;

        $scope.$on('update.geoResults', function(event, results) {
            setResults(results);
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
                $scope.resultParams.geoResults = results;

                //NB! this is cause we get multiple sets, each from a different datasource.
                var biggestSetCount = 0, startIndex = 0, elementCount = 0;
                $scope.total = 0;
                for(var i = 0; i < results.length; i++){
                    var currentTotal = parseInt(results[i].results.totalResults);

                    if(currentTotal > 0){
                        var currentIndex = parseInt(results[i].results.startIndex);
                        $scope.total += currentTotal;

                        //For the paging to work properly, we need to take the biggest set as our guide
                        if(biggestSetCount < currentTotal){
                            biggestSetCount = currentTotal;
                        }

                        if(currentIndex > currentTotal){
                            startIndex += currentTotal;
                        }
                        else {
                            startIndex += (currentIndex - 1);
                        }
                        elementCount += results[i].results.entities.length;
                    }
                }
                $scope.pageFrom = startIndex + 1;
                $scope.pageTo = startIndex + elementCount;

                /*//Re-enable this section, once we have only one set of items returned
                $scope.pageFrom = $scope.resultPaging.currentPage * $scope.resultPaging.pageSize - $scope.resultPaging.pageSize + 1;
                $scope.pageTo = $scope.resultPaging.currentPage * $scope.resultPaging.pageSize;
                if ($scope.pageTo > $scope.resultPaging.total) {
                    $scope.pageTo = $scope.resultPaging.total;
                }*/

                $scope.resultPaging.total = biggestSetCount;
                if($scope.resultPaging.total > GeoService.getMaxItemsAllowed()){
                    $scope.resultPaging.total = GeoService.getMaxItemsAllowed();
                }
            }
            else{
                delete $scope.resultParams.geoResults;
                $scope.resultPaging.currentPage = 1;
                $scope.resultPaging.total = 0;
            }

            TabService.navInfo.explorer.activeBottomNav = TabService.getBottomNavTabs().RESULTS;
            TabService.navInfo.explorer.resultTabNameExtention = GeoService.getResultsNameExtention();
            $scope.resultParams.selectedResultItems = [];
            scrollResults();
            $scope.spinner.loading = false;
        }

        if(GeoService.getResultCache()) {
            setResults(GeoService.getResultCache());
        }

        $scope.fetchResultsPage = function(pageNumber){
            GeoService.getGeoResults(pageNumber).then(function(data) {
                $rootScope.$broadcast('update.geoResults', data);
           })
           .catch(function(fallback) {
               $rootScope.$broadcast('update.geoResults');
           });
        };

        $scope.clearAll = function(){
            $rootScope.$broadcast('update.geoResults');
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

        /* Clear results when map is reset */
        $scope.$on('map.cleared', function () {
            $scope.resultParams.geoResults = [];
            cleanResults();
        });

        $scope.clearSelection = function() {
            $scope.resultParams.selectedResultItems = [];
            $rootScope.$broadcast('results.select.all', false);
        };

        function cleanResults() {
            if(!$scope.resultParams.geoResults || $scope.resultParams.geoResults.length < 1) {
                $scope.clearSelection();
                $scope.clearAll();
            }
        }
        cleanResults();

        $scope.selectAll = function() {
            $scope.resultParams.selectedResultItems = [];
            for(var i = 0; i < $scope.resultParams.geoResults.length; i++) {
                if($scope.resultParams.geoResults[i].results != null && $scope.resultParams.geoResults[i].results.entities.length > 0){
                    var list = $scope.resultParams.geoResults[i].results.entities.slice();
                    $scope.resultParams.selectedResultItems = $scope.resultParams.selectedResultItems.concat(list);
                }
            }
            $rootScope.$broadcast('results.select.all', true);
        };

        $scope.invertSelection = function() {
            var newSelection = [];
            for(var i = 0; i < $scope.resultParams.geoResults.length; i++) {
                if($scope.resultParams.geoResults[i].results != null && $scope.resultParams.geoResults[i].results.entities.length > 0){
                    var list = $scope.resultParams.geoResults[i].results.entities.slice();
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

        /* GET THE DRAGGABLE OBJECT WITH SELECTED ITEMS */
        $scope.getSelectedItemsLinks = function(item){
            if($scope.resultParams.selectedResultItems.indexOf(item) < 0){
                $scope.toggleSelection(item);
            }

            var dragObject = {
                    type: 'results',
                    selectedItems: $scope.resultParams.selectedResultItems
            };
            return dragObject;
        };

        $scope.hasCloudCoverage = function(item){
            return item.details && item.details.misc && item.details.misc.quality_info && item.details.misc.quality_info['Cloud Coverage Assessment'];
        };

    } ]);
});
