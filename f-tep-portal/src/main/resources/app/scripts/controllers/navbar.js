/**
 * @ngdoc function
 * @name ftepApp.controller:NavbarCtrl
 * @description
 * # NavbarCtrl
 * Controller of the navbar
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('NavbarCtrl', ['$scope', '$location', 'UserService', function ($scope, $location, UserService) {
        $scope.isActive = function (route) {
            return route === $location.path();
        };

        $scope.user = undefined;
        UserService.getCurrentUser().then(function(data){
            $scope.user = data;
        });

    }]);
});
