/**
* @ngdoc function
* @name ftepApp.controller:IndexCtrl
* @description
* # IndexCtrl
* Controller of the ftepApp
*/
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('IndexCtrl', ['ftepProperties', '$scope', '$location', '$window', 'UserService', function (ftepProperties, $scope, $location, $window, UserService) {

        $scope.ftepUrl = ftepProperties.FTEP_URL;
        $scope.sessionEnded = false;
        $scope.timeoutDismissed = false;

        $scope.$on('no.user', function() {
            $scope.sessionEnded = true;
        });

        $scope.hideTimeout = function() {
            $scope.sessionEnded = false;
            $scope.timeoutDismissed = true;
        };

        $scope.reloadRoute = function() {
            $window.location.reload();
        };

        $scope.goTo = function ( path ) {
            $location.path( path );
        };

        $scope.version = document.getElementById("version").content;

        // Trigger a user check to ensure controllers load correctly
        UserService.checkLoginStatus();
    }]);
});
