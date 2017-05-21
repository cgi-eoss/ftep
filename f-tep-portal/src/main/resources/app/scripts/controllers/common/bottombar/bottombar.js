/**
 * @ngdoc function
 * @name ftepApp.controller:BottombarCtrl
 * @description
 * # BottombarCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {
    ftepmodules.controller('BottombarCtrl', [ '$scope', '$rootScope', '$q', 'CommonService', 'TabService', 'BasketService', 'JobService', 'GeoService', 'FileService',
                                              function($scope, $rootScope, $q, CommonService, TabService, BasketService, JobService, GeoService, FileService) {

        $scope.bottomNavTabs = TabService.getBottomNavTabs();
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
         * Add the items to the selected databasket
         */
        $scope.addToDatabasket = function(items, isGeoResult) {
            if(isGeoResult){
                var itemLinks = [];
                var promises = [];
                for(var index in items){
                    var partialPromise = $q.defer();
                    promises.push(partialPromise.promise);

                    FileService.createGeoResultFile(items[index], $scope.resultParams.resultsMission.name).then(function(extProdFile){
                        itemLinks.push(extProdFile._links.self.href);
                        partialPromise.resolve();
                    },
                    function(error){
                        partialPromise.reject();
                    });
                }
                $q.all(promises).then(function(){
                    addToBasket(itemLinks);
                });
            }
            else{
                addToBasket(items);
            }
        };

        function addToBasket(items){
            BasketService.addItems($scope.dbParams.selectedDatabasket, items).then(function () {
                BasketService.refreshDatabaskets("explorer");
            });
        }

        $scope.bottombarTall = false;
        $scope.toggleBottombarHeight = function() {
            $scope.bottombarTall = !$scope.bottombarTall;
        };

        $scope.getColor = function(status){
            return CommonService.getColor(status);
        };

    }]);
});
