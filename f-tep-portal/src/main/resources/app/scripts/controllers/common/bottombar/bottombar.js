/**
 * @ngdoc function
 * @name ftepApp.controller:BottombarCtrl
 * @description
 * # BottombarCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {
    ftepmodules.controller('BottombarCtrl', [ '$scope', '$rootScope', 'CommonService', 'TabService', 'BasketService', 'JobService', 'GeoService',
                                              function($scope, $rootScope, CommonService, TabService, BasketService, JobService, GeoService) {

        $scope.bottomNavTabs = TabService.getBottomNavTabs();
        $scope.resultTab = TabService.resultTab;
        $scope.navInfo = TabService.navInfo;
        $scope.tabs = TabService.getTabs();
        $scope.dbParams = BasketService.params.explorer;
        $scope.jobParams = JobService.params.explorer;
        $scope.resultParams = GeoService.params;

        /** Opens a 'Create Databasket' dialog
         *  Collects selected items based on active tab, and adds to the new databasket
         */
        $scope.createNewBasket = function($event){
            CommonService.createItemDialog($event, 'BasketService', 'createDatabasket').then(function (newBasket) {
                BasketService.refreshDatabaskets("explorer", "Create");
            });
        };

        /**
         * Collects selected items based on active tab, and adds to the selected databasket
         */
        $scope.addToDatabasket = function() {
//            var items = [];
//            switch($scope.navInfo.activeBottomNav){
//                case $scope.bottomNavTabs.RESULTS:
//                    items = $scope.resultParams.selectedResultItems;
//                    break;
//                case $scope.bottomNavTabs.JOBS:
//                    items = $scope.jobParams.jobSelectedOutputs;
//                    break;
//            }
//
//            for (var i = 0; i < items.length; i++) {
//                var found = false;
//                for(var k = 0; k < $scope.dbParams.selectedDatabasket.items.length; k++){
//                    if(angular.equals(items[i], $scope.dbParams.selectedDatabasket.items[k]) ||
//                    ($scope.dbParams.selectedDatabasket.items[k].name &&
//                    $scope.dbParams.selectedDatabasket.items[k].name === items[i].identifier)){
//                        found = true;
//                        break;
//                    }
//                }
//                if(!found){
//                    $scope.dbParams.selectedDatabasket.items.push(items[i]);
//                }
//            }
//
//            BasketService.addBasketItems($scope.dbParams.selectedDatabasket, $scope.dbParams.selectedDatabasket.items);
//            $scope.$broadcast('rebuild:scrollbar');
        };

        $scope.bottombarTall = false;
        $scope.toggleBottombarHeight = function() {
            $scope.bottombarTall = !$scope.bottombarTall;
        };

        $scope.getColor = function(status){
            return CommonService.getColor(status);
        };

    }]);
});
