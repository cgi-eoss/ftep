/**
 * @ngdoc function
 * @name ftepApp.controller:AccountCtrl
 * @description
 * # AccountCtrl
 * Controller of the account page
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('AccountCtrl', ['ftepProperties', '$scope', 'UserService', 'WalletService', function (ftepProperties, $scope, UserService, WalletService) {

        $scope.ftepURL = ftepProperties.FTEP_URL;
        $scope.ssoURL = ftepProperties.SSO_URL;
        $scope.walletParams = WalletService.params.account;
        $scope.user = undefined;

        UserService.getCurrentUser().then(function(user){
            $scope.user = user;
            WalletService.refreshUserTransactions('account', user);
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
