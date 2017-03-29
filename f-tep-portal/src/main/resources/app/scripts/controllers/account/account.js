/**
 * @ngdoc function
 * @name ftepApp.controller:AccountCtrl
 * @description
 * # AccountCtrl
 * Controller of the account page
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('AccountCtrl', ['$scope', 'UserService', function ($scope, UserService) {

        $scope.user = undefined;
        $scope.accountData = { coinBalance: 0 }; //TODO

        UserService.getCurrentUser().then(function(data){
            $scope.user = data;
        });

    }]);
});
