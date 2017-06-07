/**
 * @ngdoc function
 * @name ftepApp.controller:AccountCtrl
 * @description
 * # AccountCtrl
 * Controller of the account page
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('AccountCtrl', ['ftepProperties', '$scope', 'UserService', 'WalletService', 'TabService', 'MessageService', function (ftepProperties, $scope, UserService, WalletService, TabService, MessageService) {

        /* Sidenav & Bottombar */
        $scope.navInfo = TabService.navInfo.account;
        $scope.bottombarNavInfo = TabService.navInfo.bottombar;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.toggleBottomView = function(){
            $scope.bottombarNavInfo.bottomViewVisible = !$scope.bottombarNavInfo.bottomViewVisible;
        };
        /* End Sidenav & Bottombar */

        $scope.ftepURL = ftepProperties.FTEP_URL;
        $scope.ssoURL = ftepProperties.SSO_URL;
        $scope.walletParams = WalletService.params.account;
        $scope.user = undefined;

        UserService.getCurrentUser().then(function(user){
            $scope.user = user;
            WalletService.refreshUserTransactions('account', user);
        });

        $scope.hideContent = true;
        var navbar, userdetails, sidenav;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'userdetails':
                    userdetails = true;
                    break;
            }

            if (navbar && sidenav && userdetails) {
                $scope.hideContent = false;
            }
        };

    }]);
});
