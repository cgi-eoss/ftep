/**
 * @ngdoc function
 * @name ftepApp.controller:BottombarCtrl
 * @description
 * # BottombarCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {
    ftepmodules.controller('BottombarCtrl', [ '$scope', '$rootScope', '$q', 'CommonService', 'TabService', 'BasketService', 'JobService', 'SearchService', 'FileService', 'MessageService', '$mdDialog', function($scope, $rootScope, $q, CommonService, TabService, BasketService, JobService, SearchService, FileService, MessageService, $mdDialog) {

        $scope.bottomNavTabs = TabService.getBottomNavTabs();
        $scope.tabs = TabService.getTabs();

        $scope.dbParams = BasketService.params.explorer;
        $scope.jobParams = JobService.params.explorer;
        $scope.resultsParams = SearchService.params;

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

        /* Create a databasket with items or without */
        $scope.createNewBasket = function($event, files){
            if(files && files.length > 0){
                $scope.createBasketWithItems($event, files).then(function (newBasket) {
                    BasketService.refreshDatabaskets("explorer", "Create", newBasket);
                });
            } else {
                CommonService.createItemDialog($event, 'BasketService', 'createDatabasket').then(function (newBasket) {
                    BasketService.refreshDatabaskets("explorer", "Create", newBasket);
                });
            }
        };

        /* Create a databasket with items */
        $scope.createBasketWithItems = function($event, files){
            var deferred = $q.defer();
            function BasketController($scope, $mdDialog, BasketService) {
                $scope.files = files;

                $scope.createBasket= function() {
                    BasketService.createDatabasket($scope.newBasket.name, $scope.newBasket.description).then(function(newBasket) {
                        BasketService.addToDatabasket(newBasket, files);
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    deferred.reject();
                    $mdDialog.hide();
                };
            }

            BasketController.$inject = ['$scope', '$mdDialog', 'BasketService'];
            $mdDialog.show({
                controller: BasketController,
                templateUrl: 'views/explorer/templates/createdatabasket.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });

            return deferred.promise;
        };

        /* Add the items to the selected databasket */
        $scope.addToDatabasket = function(items) {
            BasketService.addToDatabasket($scope.dbParams.selectedDatabasket, items);
        };

    }]);
});
