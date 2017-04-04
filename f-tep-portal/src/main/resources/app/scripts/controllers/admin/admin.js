/**
 * @ngdoc function
 * @name ftepApp.controller:AdminCtrl
 * @description
 * # AdminCtrl
 * Controller of the admin page
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('AdminCtrl', ['$scope', 'UserService', function ($scope, UserService) {

        $scope.userParams = UserService.params.admin;

        $scope.selectedUser = { accountData: { coinBalance: 0 }}; //TODO
        $scope.coins = 0;

        var allUsers;
        UserService.getAllUsers().then(function (data) {
            allUsers = data;
        });

        $scope.searchUsers = function() {
            var filteredItems = [];
            var queryLower = angular.copy($scope.userParams.searchText).toLowerCase();

            filteredItems = allUsers.filter(function(user) {
                return (user.name.toLowerCase()).indexOf(queryLower) > -1;
            });

            //reset account data
            $scope.selectedUser = { accountData: { coinBalance: 0 }}; //TODO
            $scope.coins = 0;
            return filteredItems;
        };

        $scope.addCoins = function() {
            $scope.selectedUser.accountData.coinBalance += $scope.coins;
            //TODO
        };

    }]);
});
