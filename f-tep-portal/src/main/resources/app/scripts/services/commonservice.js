/**
 * @ngdoc service
 * @name ftepApp.CommonService
 * @description
 * # CommonService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('CommonService', [ '$rootScope', 'ftepProperties', '$mdDialog', '$q', function($rootScope, ftepProperties, $mdDialog, $q) {

                this.getColor = function(status){
                    if("Succeeded" === status || "approved" === status){
                        return "background: #dff0d8; border: 2px solid #d0e9c6; color: #3c763d";
                    } else if("Failed" === status || "Error" === status){
                        return "background: #f2dede; border: 2px solid #ebcccc; color: #a94442";
                    } else if("Running" === status || "Info" === status){
                        return "background: #d9edf7; border: 2px solid #bcdff1; color: #31708f";
                    } else if("Warning" === status){
                        return "background: #fcf8e3; border: 2px solid #faf2cc; color: #8a6d3b";
                    }
                };

                this.getOutputLink = function(link){
                    return  ftepProperties.URL_PREFIX + link;
                };

                this.confirm = function(event, message) {
                    var deferred = $q.defer();
                    var confirm = $mdDialog.confirm()
                          .title('Confirmation needed')
                          .textContent(message)
                          .targetEvent(event)
                          .ok('Confirm')
                          .cancel('Cancel');

                    $mdDialog.show(confirm).then(function() {
                        deferred.resolve(true);
                    }, function() {
                        deferred.resolve(false);
                    });
                    return deferred.promise;
                  };

                return this;
      }]);
});
