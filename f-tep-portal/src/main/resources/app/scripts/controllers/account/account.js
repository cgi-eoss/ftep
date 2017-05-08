/**
 * @ngdoc function
 * @name ftepApp.controller:AccountCtrl
 * @description
 * # AccountCtrl
 * Controller of the account page
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('AccountCtrl', ['$scope', 'UserService', 'WalletService', function ($scope, UserService, WalletService) {

        $scope.user = undefined;
        $scope.wallet = undefined;

        UserService.getCurrentUser().then(function(user){
            $scope.user = user;
            WalletService.getUserWalletByUserId(user.id).then(function(wallet){
                $scope.wallet = wallet;
            });
        });

        $scope.hideContent = true;
        var navbar;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
            }

            if (navbar) {
                $scope.hideContent = false;
            }
        };

    }]);
});
