/**
 * @ngdoc function
 * @name ftepApp.controller:NavbarCtrl
 * @description
 * # NavbarCtrl
 * Controller of the navbar
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('NavbarCtrl', ['ftepProperties', '$scope', '$location', 'UserService', '$window', function (ftepProperties, $scope, $location, UserService, $window) {

        $scope.user = undefined;
        $scope.ssoUrl = ftepProperties.SSO_URL;
        $scope.ftepUrl = ftepProperties.FTEP_URL;

        $scope.isActive = function (route) {
            return route === $location.path();
        };

        UserService.getCurrentUser().then(function(data){
            $scope.user = data;
        });

        $scope.reloadRoute = function() {
           $window.location.reload();
        };

        $scope.$on('no.user', function() {
            $scope.user = "Guest";
        });

    }]);
});
