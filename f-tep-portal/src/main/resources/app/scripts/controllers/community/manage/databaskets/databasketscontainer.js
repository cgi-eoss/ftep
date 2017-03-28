/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityDatabasketsContainerCtrl
 * @description
 * # CommunityDatabasketsContainerCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityDatabasketsContainerCtrl', ['MessageService', 'BasketService', '$rootScope', '$scope', '$mdDialog', '$sce', function (MessageService, BasketService, $rootScope, $scope, $mdDialog, $sce) {

        /* Get stored Databasket details */
        $scope.basketParams = BasketService.params.community;

        /* Select a Databasket */
        $scope.selectItem = function (item) {
            $scope.basketParams.selectedDatabasket = item;
            BasketService.getItems(item).then(function (data) {
                $scope.basketParams.items = data;
            });
        };

        /* Databaskets right display */
        $scope.displayRight = function() {
            if ($scope.basketParams.selectedDatabasket === undefined) {
                return false;
            }
            return true;
        };

    }]);
});
