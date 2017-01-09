/**
 * @ngdoc function
 * @name ftepApp.controller:NavbarCtrl
 * @description
 * # NavbarCtrl
 * Controller of the navbar
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('NavbarCtrl', function ($scope, $location) {
        $scope.isActive = function (route) {
            return route === $location.path();
        };
    });
});
