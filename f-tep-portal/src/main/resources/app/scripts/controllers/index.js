/**
* @ngdoc function
* @name ftepApp.controller:IndexCtrl
* @description
* # IndexCtrl
* Controller of the ftepApp
*/
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('IndexCtrl', ['$scope', '$location', function ($scope, $location) {

        $scope.goTo = function ( path ) {
            $location.path( path );
        };

        $scope.version = document.getElementById("version").content;

    }]);
});
