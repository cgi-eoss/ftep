/**
 * @ngdoc overview
 * @name ftepApp
 * @description
 * # ftepApp
 *
 * Main module of the application.
 */

require(['bootstrap']);

define([
    'ftepConfig',
    'angular',
    'ngRoute',
    'ngMaterial',
    'ngAnimate',
    'ngCookies',
    'ngSanitize',
    'ngMessages',
    'ngTouch',
    'ngResource',
    'ngAria',
    'ol',
    'dndLists',
    'ngOpenlayers',
    'ngBootstrap',
    'ngPaging',
    'moment',
    'angularMoment',
    'traversonAngular',
    'traversonHal',
    'ngFileUpload',
    'uiCodeMirror',
    'moduleloader'
], function (ftepConfig) {
    'use strict';

    var app = angular.module('ftepApp', ['app.ftepmodules', 'ngRoute', 'ngMaterial', 'ngAnimate', 'ngAria', 'ngSanitize', 'ngMessages',
                                         'ngResource', 'dndLists', 'ui.bootstrap', 'openlayers-directive', 'bw.paging',
                                         'angularMoment', 'traverson', 'ngFileUpload', 'ui.codemirror']);

    /* jshint -W117  */
    app.constant('ftepProperties', {
        'FTEP_URL': ftepConfig.ftepUrl,
        'URL': ftepConfig.apiUrl,
        'URLv2': ftepConfig.apiUrlv2,
        'SSO_URL': ftepConfig.ssoUrl,
        'MAPBOX_URL': "https://api.mapbox.com/styles/v1/mapbox/streets-v8/tiles/{z}/{x}/{y}?access_token=" + ftepConfig.mapboxToken
    });
    /* jshint +W117 */

    app.init = function () {
        angular.bootstrap(document, ['ftepApp']);
        require([
            "scripts/vendor/codemirror/lib/codemirror",
            "scripts/vendor/codemirror/mode/dockerfile/dockerfile",
            "scripts/vendor/codemirror/mode/javascript/javascript",
            "scripts/vendor/codemirror/mode/perl/perl",
            "scripts/vendor/codemirror/mode/php/php",
            "scripts/vendor/codemirror/mode/properties/properties",
            "scripts/vendor/codemirror/mode/python/python",
            "scripts/vendor/codemirror/mode/shell/shell",
            "scripts/vendor/codemirror/mode/xml/xml",
            "scripts/vendor/codemirror/mode/yaml/yaml"
        ], function(CodeMirror) {
            window.CodeMirror = CodeMirror;
            }
        );
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
                templateUrl: 'views/developer/developer.html',
                resolve:{
                    "check": ['$location', 'UserService', function($location, UserService) {
                        UserService.getCurrentUser().then(function(user){
                            if(user.role !== 'ADMIN' && user.role !== 'EXPERT_USER' && user.role !== 'CONTENT_AUTHORITY'){
                                $location.path('/');  //redirect to homepage
                            }
                        });
                    }]
                }
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
            .when('/admin', {
                templateUrl: 'views/admin/admin.html',
                controller: 'AdminCtrl',
                controllerAs: 'admin',
                resolve:{
                    "check": ['$location', 'UserService', function($location, UserService) {
                        UserService.getCurrentUser().then(function(user){
                            if(user.role !== 'ADMIN'){
                                $location.path('/');  //redirect to homepage
                            }
                        });
                    }]
                }
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
            var m = moment(dateString.trim(), 'DD-MM-YYYY', true);
            return m.isValid() ? m.toDate() : new Date(NaN);
        };
    }]);

    app.filter('bytesToGB', function () {
        return function (bytes) {
            if (bytes && isNaN(bytes) || bytes < 1) {
                return bytes;
            }
            else if(bytes){
                return (bytes / 1073741824).toFixed(2) + ' GB';
            }
            else{
                return '';
            }
        };
    });

    app.filter('formatDateTime', function(){
        return function (dateTime) {
            if(dateTime){
                if(typeof dateTime === 'string'){
                    return dateTime;
                }
                else {
                    return new Date(dateTime.year + "-" +
                        getTwoDigitNumber(dateTime.monthValue) + "-" +
                        getTwoDigitNumber(dateTime.dayOfMonth) + "T" +
                        getTwoDigitNumber(dateTime.hour) + ":" +
                        getTwoDigitNumber(dateTime.minute) + ":" +
                        getTwoDigitNumber(dateTime.second) + "." +
                        getThreeDigitNumber(dateTime.nano/1000000) + "Z").toISOString();
                }
            }
            else{
                return '';
            }
        };
    });

    function getTwoDigitNumber(num){
        return (num > 9 ? num : '0'+num);
    }

    function getThreeDigitNumber(num){
        return (num > 99 ? num : (num > 9 ? '0'+num : '00' + num));
    }

    app.filter('asSingular', function () {
        return function (name) {
            if (name.lastIndexOf('s') === (name.length -1)) {
                return name.substr(0, name.length-1);
            } else {
                return name;
            }
        };
    });

    app.filter("nl2br", function () {
        return function (data) {
            if (!data) {
                return data;
            }
            return data.replace(/\r/g, '<br />');
        };
    });

    app.directive('hasPermission', function() {
        return {
            link: function(scope, element, attrs, ngModel) {

                if(!attrs.hasPermission || ['READ', 'WRITE', 'ADMIN', 'SUPERUSER'].indexOf(attrs.hasPermission.toUpperCase() ) < 0) {
                    throw 'hasPermission must be set';
                }

                if(!attrs.hasOwnProperty('permissionSource')) {
                    throw 'For each hasPermission attribute, the permissionSource must also be set';
                }

                function checkPermission(){
                    var userPermission = 'READ'; //default user permission
                    if(attrs.permissionSource){
                        var permissionSource = JSON.parse(attrs.permissionSource);
                        if(permissionSource.access) {
                            userPermission = (permissionSource.access.currentLevel ? permissionSource.access.currentLevel : 'READ');
                        }
                    }

                    var allowed = false;
                    switch(attrs.hasPermission){
                        case 'READ':
                            allowed = true;
                            break;
                        case 'WRITE':
                            allowed = (['WRITE', 'ADMIN', 'SUPERUSER'].indexOf(userPermission.toUpperCase()) > -1);
                            break;
                        case 'ADMIN':
                            allowed = (['ADMIN', 'SUPERUSER'].indexOf(userPermission.toUpperCase()) > -1);
                            break;
                        case 'SUPERUSER':
                            allowed = (['SUPERUSER'].indexOf(userPermission.toUpperCase()) > -1);
                            break;
                    }

                    // Whether an element has been requested to be disabled only. If no disable-on-check setting, hide the whole element.
                    if(attrs.hasOwnProperty('disableOnCheck')){
                        attrs.$set('disabled', !allowed);
                    } else {
                        if(allowed){
                            element.show();
                        } else {
                            element.hide();
                        }
                    }
                }
                checkPermission();

                // watch for element updates
                attrs.$observe('permissionSource', function() {
                    checkPermission();
                });

                element.show();
            }
        };
    });

    /** Directive for adding specific style-class based on screen size **/
    app.directive("mediaClass", ["$mdMedia", "$window", "$timeout", function($mdMedia, $window, $timeout) {
        return {
            restrict: "A",
            link: function(scope, element, attrs) {

                function checkMediaClass(){
                    var styles = attrs.mediaClass.split(',');
                    for(var i = 0; i < styles.length; i++){
                        var keyVal = styles[i].split(':');

                        //add specified class based on the screen size, the attribute values should be ordered from smallest to largest
                        if($mdMedia(keyVal[0])){
                            element.addClass(keyVal[1]);
                        }
                        // remove from elements that are in the gray area between bootstrap and material values
                        if(keyVal[0] === 'gt-md' && $mdMedia('min-width: 1199px') && $mdMedia('max-width: 1280px')){
                          element.removeClass(keyVal[1]);
                        }
                    }
                }
                checkMediaClass();

                angular.element($window).on('resize', function(){
                    $timeout(function () {
                        checkMediaClass();
                    }, 100);
                });
            }
        };
     }]);

    /** Directive for showing/hiding elements based on screen size **/
    app.directive('mediaShow', ["$mdMedia", "$window", "$timeout", function($mdMedia, $window, $timeout) {
        return {
            link: function(scope, element, attrs) {

                function checkVisibility(){
                    element.hide();

                    //if the screen size is defined in the media-show list, set the element visible at all times
                    var showList = attrs.mediaShow.split(',');
                    for(var i = 0; i < showList.length; i++){
                        if($mdMedia(showList[i])){
                            element.show();
                        }
                    }

                    // otherwise check if a special case exists for the element
                    if(element.is(':visible') === false){
                        if(attrs.mediaShowOn && attrs.mediaShowOn === "true"){
                            element.show();
                        }
                    }
                }
                checkVisibility();

                // watch for element updates
                attrs.$observe('mediaShowOn', function() {
                    checkVisibility();
                });

                angular.element($window).on('resize', function(){
                    $timeout(function () {
                        checkVisibility();
                    }, 100);
                });
            }
        };
    }]);

    app.directive('ftepPaging', function () {
        return {
            restrict: 'EA',
            templateUrl: 'views/common/templates/paging.tmpl.html',
            link: function ($scope, element, attrs) {

                $scope.pagingData = JSON.parse(attrs.ftepPaging);

                $scope.getPageText = function(){
                    var txt = 'Page 1 of 1';
                    if($scope.pagingData.page){
                        var currentPage = $scope.pagingData.page.number + 1;
                        txt = 'Page ' + currentPage + ' of ' + $scope.pagingData.page.totalPages;
                    }
                    return txt;
                };

                attrs.$observe('ftepPaging', function() {
                    $scope.pagingData = JSON.parse(attrs.ftepPaging);
                });

            }
        };
    });

    app.directive('serviceInput', function(){
        return {
            restrict: 'E',
            templateUrl: 'views/common/templates/serviceinput.tmpl.html',
            link: function ($scope, element, attrs) {

                function checkType(){
                    var data = JSON.parse(attrs.defaultAttrs);
                    var inputField = element.find('input');

                    $scope.fieldData = {isSelection: (data.allowedValues && data.allowedValues !== '' ? true : false),
                                        allowedValues: (data.allowedValues && data.allowedValues !== '' ? data.allowedValues.split(',') : []),
                                        description: attrs.description};

                    if($scope.fieldData.allowedValues.length > 0 && attrs.ngRequired === 'false'){
                        $scope.fieldData.allowedValues.splice(0, 0, ''); //add empty value at the beginning
                    }

                    switch(data.dataType){
                        case 'integer':
                            inputField.attr('type', 'number');
                            inputField.attr('step', '1');
                            inputField.attr('required', attrs.ngRequired);
                            break;
                        case 'double':
                            inputField.attr('type', 'number');
                            inputField.attr('step', '0.1');
                            inputField.attr('required', attrs.ngRequired);
                            break;
                        default:
                            inputField.attr('type', 'text');
                            inputField.attr('required', attrs.ngRequired);
                    }
                }

                attrs.$observe('defaultAttrs', function() {
                    checkType();
                });
            },
            scope: {
                value: '=ngModel'
            }
        };
    });

    return app;
});
