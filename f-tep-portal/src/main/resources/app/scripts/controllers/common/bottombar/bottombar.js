/**
 * @ngdoc function
 * @name ftepApp.controller:BottombarCtrl
 * @description
 * # BottombarCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {
    ftepmodules.controller('BottombarCtrl', [ '$scope', '$rootScope', '$q', 'CommonService', 'TabService', 'BasketService', 'JobService', 'GeoService', 'FileService', 'MessageService', function($scope, $rootScope, $q, CommonService, TabService, BasketService, JobService, GeoService, FileService, MessageService) {

        $scope.bottomNavTabs = TabService.getBottomNavTabs();
        $scope.tabs = TabService.getTabs();

        $scope.dbParams = BasketService.params.explorer;
        $scope.jobParams = JobService.params.explorer;
        $scope.resultParams = GeoService.params;

        /** Opens a 'Create Databasket' dialog
         *  Collects selected items based on active tab, and adds to the new databasket
         */
        $scope.createNewBasket = function($event, items, isGeoResult){
            if(items && items.length > 0){
                CommonService.createBasketWithItems($event, items).then(function (newBasket) {
                    $scope.addToDatabasket(items, isGeoResult);
                    BasketService.refreshDatabaskets("explorer", "Create", newBasket);
                });
            }
            else {
                CommonService.createItemDialog($event, 'BasketService', 'createDatabasket').then(function (newBasket) {
                    BasketService.refreshDatabaskets("explorer", "Create", newBasket);
                });
            }
        };

        /**
         * Add the items to the selected databasket
         */
        $scope.addToDatabasket = function(items, isGeoResult) {
            if(isGeoResult){
                var itemLinks = [];
                var promises = [];
                for(var index in items){
                    promises.push(getBasketItemLink(items[index], itemLinks));
                }

                $q.all(promises).then(function(){
                    addToBasket(itemLinks);
                });
            }
            else{
                addToBasket(items);
            }
        };

        function getBasketItemLink(item, itemLinks){
            var partialPromise = $q.defer();
            FileService.createGeoResultFile(item, $scope.resultParams.resultsMission.name).then(function(extProdFile){
                itemLinks.push(extProdFile._links.self.href);
                partialPromise.resolve();
            },
            function(error){
                MessageService.addError('Could not add file to Databasket', error);
                partialPromise.reject();
            });
            return partialPromise.promise;
        }

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

        $scope.estimateDownloadCost = function($event, file){
            CommonService.estimateDownloadCost($event, file);
        };

    }]);
});
