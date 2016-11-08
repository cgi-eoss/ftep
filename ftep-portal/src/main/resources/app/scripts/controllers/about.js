/**
 * @ngdoc function
 * @name ftepApp.controller:AboutCtrl
 * @description
 * # AboutCtrl
 * Controller of the ftepApp
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('AboutCtrl', function () {
        this.awesomeThings = [
          'HTML5 Boilerplate',
          'AngularJS',
          'Karma'
        ];
      });
});