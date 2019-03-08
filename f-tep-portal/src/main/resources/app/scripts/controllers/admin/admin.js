/**
 * @ngdoc function
 * @name ftepApp.controller:AdminCtrl
 * @description
 * # AdminCtrl
 * Controller of the admin page
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('AdminCtrl', ['$scope', 'UserService', 'MessageService', 'WalletService', 'TabService', 'ftepProperties', function ($scope, UserService, MessageService, WalletService, TabService, ftepProperties) {

        $scope.rootUri = ftepProperties.URLv2;

        /* Sidenav & Bottombar */
        $scope.navInfo = TabService.navInfo.admin;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.toggleBottomView = function(){
            $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
        };
        /* End Sidenav & Bottombar */

        $scope.userParams = UserService.params.admin;
        $scope.roles = ['USER', 'EXPERT_USER', 'CONTENT_AUTHORITY', 'ADMIN'];
        $scope.months = [ 'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December' ];

        /* Populate report years between 2018-current */
        let initialYear = 2018;
        $scope.years = [];
        while ( initialYear <= new Date().getFullYear() ) {
            $scope.years.push(initialYear++);
        }

        /* Paging */
        $scope.getPage = function(url){
            UserService.getUsersPage('admin', url);
        };

        UserService.getUsersByFilter('admin');

        $scope.filter = function(){
            UserService.getUsersByFilter('admin');
        };

        $scope.getUserData = function(){
            if($scope.userParams.selectedUser){
                UserService.getUserByLink($scope.userParams.selectedUser._links.self.href).then(function(data){
                    $scope.userParams.userDetails = data;
                });

                WalletService.getUserWallet($scope.userParams.selectedUser).then(function(wallet){
                    $scope.userParams.wallet = wallet;
                });
            }
        };

        $scope.addCoins = function() {
            WalletService.makeTransaction($scope.userParams.selectedUser, $scope.userParams.wallet, $scope.userParams.coins).then(function(){
                $scope.userParams.coins = 0;
                $scope.getUserData();
            });
        };

        $scope.updateRole = function(newRole) {
            $scope.userParams.userDetails.role = newRole;
            UserService.updateUser($scope.userParams.userDetails).then(function(data){
                $scope.getUserData();
            });
        };

        $scope.hideContent = true;
        var navbar, sidenav, management;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'management':
                    management = true;
                    break;
            }

            if (navbar && sidenav && management) {
                $scope.hideContent = false;
            }
        };

    }]);
});
