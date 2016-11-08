/**
 * @ngdoc overview
 * @name ftepApp
 * @description
 * # ftepApp
 *
 * Main module of the application.
 */

require(['bootstrap', 'notify']);

define([
    'angular',
    'ngRoute',
    'ngMaterial',
    'ngAnimate',
    'ngCookies',
    'ngSanitize',
    'ngTouch',
    'ngResource',
    'ngAria',
    'ol',
    'rzModule',
    'dndLists',
    'ngOpenlayers',
    'ngBootstrap',
    'scripts/moduleloader',
    'ngPaging'
], function () {
    'use strict';

    var app = angular.module('ftepApp', ['app.ftepmodules', 'ngRoute', 'ngMaterial', 'ngAnimate', 'ngAria', 'ngSanitize', 'rzModule', 'dndLists', 'ui.bootstrap', 'openlayers-directive', 'bw.paging']);


    //TODO mapbox access token!!!
    app.constant( "ftepProperties", {
         "URL": "/secure/api/v1.0",
         "URL_PREFIX": "https://192.171.139.83",
         "MAPBOX_TOKEN": "https://api.mapbox.com/styles/v1/mapbox/streets-v8/tiles/{z}/{x}/{y}?access_token=pk.eyJ1IjoibWxvYiIsImEiOiJjaXFqNjNwdGgwMGMxaHBtY2lrOWNzM3hqIn0.vL2vPjsDl3aEh_jZa63OrQ"
    });

    app.init = function() {
        angular.bootstrap(document, ['ftepApp']);
    }

      app.config(['$routeProvider', function ($routeProvider) {
        $routeProvider
          .when('/', {
            templateUrl: 'views/explorer.html',
            controller: 'ExplorerCtrl',
            controllerAs: 'main'
          })
          .when('/about', {
            templateUrl: 'views/about.html',
            controller: 'AboutCtrl',
            controllerAs: 'about'
          })
          .when('/developer', {
              templateUrl: 'views/developer.html'
          })
          .when('/community', {
              templateUrl: 'views/community.html',
              controller: 'CommunityCtrl'
          })
          .when('/account', {
              templateUrl: 'views/account.html'
          })
          .when('/helpdesk', {
              templateUrl: 'views/helpdesk.html',
              controller: 'HelpdeskCtrl',
              controllerAs: 'helpdesk'
            })
          .otherwise({
            redirectTo: '/'
          });
      }]);

      /* Custom angular-material color theme */
      app.config(['$mdThemingProvider', function($mdThemingProvider) {
          $mdThemingProvider.definePalette('ftepColorSchema', {
            '50' : 'abf6b7',
            '100' : 'abf6b7',
            '200' : '345a3d',
            '300' : '345a3d',
            '400' : 'ef5350',
            '500' : '345a3d',
            '600' : 'e53935',
            '700' : 'd32f2f',
            '800' : 'c62828',
            '900' : 'b71c1c',
            'A100' : '4da762',
            'A200' : '208e3a',
            'A400' : '1a742f',
            'A700' : 'd50000',
            'contrastDefaultColor' : 'light',
            'contrastDarkColors' : [ '50', '100', //hues which contrast should be 'dark' by default
            '200', '300', '400', 'A100' ],
            'contrastLightColors' : undefined
        });
        $mdThemingProvider.theme('default').primaryPalette('ftepColorSchema');
    }]);

     app.filter('bytesToGB', function(){
          return function(bytes){
              if(isNaN(bytes) || bytes < 1){
                  return bytes;
              }
              else {
                  return (bytes/1073741824).toFixed(2)+' GB';
              }
          }
       });

    return app;
});
