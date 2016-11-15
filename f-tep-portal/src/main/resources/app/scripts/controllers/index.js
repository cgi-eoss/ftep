/**
* @ngdoc function
* @name ftepApp.controller:IndexCtrl
* @description
* # IndexCtrl
* Controller of the ftepApp
*/
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('IndexCtrl', function ($scope, $location) {
        this.awesomeThings = [
          'HTML5 Boilerplate',
          'AngularJS',
          'Karma'
        ];

        $scope.goTo = function ( path ) {
            $location.path( path );
        };
      });

});