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
    'ftepConfig',
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
    'ngPaging',
    'moment',
    'angularMoment',
    'ngScrollbar',
    'traversonAngular',
    'traversonHal',
    'ngFileUpload',
    'moduleloader'
], function (ftepConfig) {
    'use strict';

    var app = angular.module('ftepApp', ['app.ftepmodules', 'ngRoute', 'ngMaterial', 'ngAnimate', 'ngAria', 'ngSanitize', 'ngResource',
                                         'rzModule', 'dndLists', 'ui.bootstrap', 'openlayers-directive', 'bw.paging', 'angularMoment',
                                         'ngScrollbar', 'traverson', 'ngFileUpload']);

    /* jshint -W117  */
    app.constant('ftepProperties', {
        "URL_PREFIX": ftepConfig.urlPrefix,
        "URL": ftepConfig.apiUrl,
        "URLv2": ftepConfig.apiUrlv2,
        "ZOO_URL": ftepConfig.zooUrl,
        "WMS_URL": ftepConfig.wmsUrl,
        "MAPBOX_URL": "https://api.mapbox.com/styles/v1/mapbox/streets-v8/tiles/{z}/{x}/{y}?access_token=" + ftepConfig.mapboxToken
    });
    /* jshint +W117 */

    app.init = function () {
        angular.bootstrap(document, ['ftepApp']);
    };

    app.config(['$routeProvider', '$locationProvider', '$httpProvider', function ($routeProvider, $locationProvider, $httpProvider) {
        $httpProvider.defaults.withCredentials = true;
        $routeProvider
            .when('/', {
                templateUrl: 'views/explorer/explorer.html',
                controller: 'ExplorerCtrl',
                controllerAs: 'main'
            })
            .when('/developer', {
                templateUrl: 'views/developer/developer.html'
            })
            .when('/community', {
                templateUrl: 'views/community/community.html',
                controller: 'CommunityCtrl'
            })
            .when('/account', {
                templateUrl: 'views/account/account.html'
            })
            .when('/helpdesk', {
                templateUrl: 'views/helpdesk/helpdesk.html',
                controller: 'HelpdeskCtrl',
                controllerAs: 'helpdesk'
            })
            .otherwise({
                redirectTo: '/'
            });
        $locationProvider.html5Mode(false);
      }]);

    /* Custom angular-material color theme */
    app.config(['$mdThemingProvider', function ($mdThemingProvider) {
        $mdThemingProvider.theme('default')
            .primaryPalette('light-green', {
                'default': '800',
                'hue-1': '300',
                'hue-2': '900'
            })
            .accentPalette('pink', {
                'default': 'A200',
                'hue-1': 'A100',
                'hue-2': 'A700'
            })
            .warnPalette('red')
            .backgroundPalette('grey');
    }]);

    /* Set time & date format */
    app.config(['$mdDateLocaleProvider', 'moment', function ($mdDateLocaleProvider, moment) {

        $mdDateLocaleProvider.formatDate = function (date) {
            return date ? moment(date).format('DD-MM-YYYY') : '';
        };

        $mdDateLocaleProvider.parseDate = function (dateString) {
            var m = moment(dateString, 'DD-MM-YYYY', true);
            return m.isValid() ? m.toDate() : new Date(NaN);
        };
    }]);

    app.filter('bytesToGB', function () {
        return function (bytes) {
            if (isNaN(bytes) || bytes < 1) {
                return bytes;
            } else {
                return (bytes / 1073741824).toFixed(2) + ' GB';
            }
        };
    });

    app.filter('asSingular', function () {
        return function (name) {
            if (name.lastIndexOf('s') === (name.length -1)) {
                return name.substr(0, name.length-1);
            } else {
                return name;
            }
        };
    });

    app.directive('hasPermission', function() {
        return {
            link: function(scope, element, attrs, ngModel) {

                if(!attrs.hasPermission || ['READ', 'EDIT', 'ADMIN'].indexOf(attrs.hasPermission.toUpperCase() ) < 0) {
                    throw 'hasPermission must be set';
                }

                if(!attrs.hasOwnProperty('permissionSource')) {
                    throw 'For each hasPermission attribute, the permissionSource must also be set';
                }

                function checkPermission(){
                    var userPermission = 'READ'; //default user permission
                    if(attrs.permissionSource){
                        var permissionSource = JSON.parse(attrs.permissionSource);
                        userPermission = (permissionSource.attributes.permissionLevel ? permissionSource.attributes.permissionLevel : 'READ');
                    }

                    var allowed = false;
                    switch(attrs.hasPermission){
                        case 'READ':
                            allowed = true;
                            break;
                        case 'EDIT':
                            allowed = (['EDIT', 'ADMIN',].indexOf(userPermission.toUpperCase()) > -1);
                            break;
                        case 'ADMIN':
                            allowed = (['ADMIN'].indexOf(userPermission.toUpperCase()) > -1);
                            break;
                    }

                    //Whether an element has been requested to be disabled only. If no disable-on-check setting, hide the whole element.
                    if(attrs.hasOwnProperty('disableOnCheck')){
                        attrs.$set('disabled', !allowed);
                    }
                    else{
                        if(allowed){
                            element.show();
                        }
                        else {
                            element.hide();
                        }
                    }
                }
                checkPermission();

                // watch for element updates
                attrs.$observe('permissionSource', function() {
                    checkPermission();
                });

            }
          };
        });

    return app;
});
