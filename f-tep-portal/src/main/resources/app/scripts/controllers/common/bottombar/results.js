/**
 * @ngdoc function
 * @name ftepApp.controller:ResultsCtrl
 * @description
 * # ResultsCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ResultsCtrl', [ '$scope', '$rootScope', '$anchorScroll', '$timeout', 'SearchService', 'TabService', 'CommonService', function($scope, $rootScope, $anchorScroll, $timeout, SearchService, TabService, CommonService) {

        $scope.searchParams = SearchService.params;
        $scope.spinner = SearchService.spinner;
        $scope.resultParams = $scope.searchParams.results;

        /* Set results and display the results section */
        function setResults(results){
            $scope.resultParams.geoResults = results;

            /* Set paging info if there are results */
            if(results) {
                $scope.resultParams.pagingData = {};
                $scope.resultParams.pagingData.page = results.page;
                $scope.resultParams.pagingData._links = results._links;
            }

            /* Open results and name tab */
            TabService.navInfo.explorer.activeBottomNav = TabService.getBottomNavTabs().RESULTS;
            TabService.navInfo.explorer.resultTabNameExtention = SearchService.getSearchName();

            /* Clear previous selections, scroll to the top and hide the spinner */
            $scope.resultParams.selectedResultItems = [];
            scrollResults();
        }

        /* Paging */
        $scope.getPage = function(url){
            SearchService.getResultsPage(url).then(function (results) {
                $rootScope.$broadcast('update.geoResults', results);
            });
        };

        /* Get results */
        $scope.$on('update.geoResults', function(event, results) {
            TabService.navInfo.explorer.activeBottomNav = TabService.getBottomNavTabs().RESULTS;
            setResults(results);
        });

        /* Scroll to the top of the results section*/
        function scrollResults(anchorId) {
            if(anchorId){
                $timeout(function () {
                    $anchorScroll(anchorId);
                }, 50);
            }
        }

        /* Get the draggable object with selected items */
        $scope.getSelectedItemsLinks = function(item){
            if($scope.resultParams.selectedResultItems.indexOf(item) < 0){
                $scope.toggleSelection(item);
            }
            return {
                type: 'results',
                selectedItems: $scope.resultParams.selectedResultItems
            };
        };

        /* Toggle selection of result */
        $scope.toggleSelection = function(item, fromMap) {
            if(item) {
                var index = $scope.resultParams.selectedResultItems.indexOf(item);
                if (index < 0) {
                    $scope.resultParams.selectedResultItems.push(item);
                    if(fromMap){
                        scrollResults(item.properties.productIdentifier);
                    }
                } else {
                    $scope.resultParams.selectedResultItems.splice(index, 1);
                }
                if(!fromMap){
                    $rootScope.$broadcast('results.item.selected', item, index < 0);
                }
            }
        };

        /* Toggle result on map */
        $scope.$on('map.item.toggled', function(event, items) {
            scrollResults();
            $scope.resultParams.selectedResultItems = items;
            if(items && items.length > 0){
                scrollResults(items[items.length-1].properties.productIdentifier);
            }
        });

        /* Estimate Download cost */
        $scope.estimateDownloadCost = function($event, file) {
            CommonService.estimateDownloadCost($event, file);
        };

        /* BOTTOMBAR TAB BUTTONS */

        /* Invert result selection */
        $scope.invertSelection = function() {
            var newSelection = [];
            for(var item in $scope.resultParams.geoResults.features) {
                if(!CommonService.containsObject($scope.resultParams.geoResults.features[item], $scope.resultParams.selectedResultItems)) {
                    newSelection.push($scope.resultParams.geoResults.features[item]);
                }
            }
            $scope.resultParams.selectedResultItems = newSelection;
            $rootScope.$broadcast('results.invert', $scope.resultParams.selectedResultItems);
        };

        /* Clear selection */
        $scope.clearSelection = function() {
            $scope.resultParams.selectedResultItems = [];
            $rootScope.$broadcast('results.select.all', false);
        };

        /* Select all results */
        $scope.selectAll = function() {
            var newSelection = [];
            for(var item in $scope.resultParams.geoResults.features) {
                newSelection.push($scope.resultParams.geoResults.features[item]);
            }
            $scope.resultParams.selectedResultItems = newSelection;
            $rootScope.$broadcast('results.select.all', true);
        };

        $scope.clearAll = function(){
            setResults();
        };

        /* Clear results */
        function cleanResults() {
            if(!$scope.resultParams.geoResults || $scope.resultParams.geoResults.length < 1) {
                $scope.clearSelection();
                $scope.clearAll();
            }
        }
        cleanResults();

        /* Clear results when map is reset */
        $scope.$on('map.cleared', function () {
            $scope.resultParams.geoResults = [];
            cleanResults();
        });

    }]);
});
