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

                this.warn = function(message){
                    $rootScope.$broadcast('show.warning', message);
                };
                this.getColor = function(status){
                    if("Succeeded" === status || "approved" === status){
                        return "background: rgba(5, 137, 23, 0.8)";
                    }
                    else if("Failed" === status){
                        return "background: rgba(198, 11, 11, 0.8)";
                    }
                    else if("Running" === status){
                        return "background: rgba(0, 0, 226, 0.8)";
                    }
                };

                this.getLink = function(item, results){
                    var link;
                    if(item.link){
                        link = item.link;
                    }
                    else if(item.attributes && item.attributes.link){
                        link = item.attributes.link;
                    }
                    else if(item.identifier && results){
                        for(var i = 0; i < results.length; i++){
                            link = Object.keys(results[i].results.entities).find(function (key) {
                                return results[i].results.entities[key] === item;
                            });
                            if(link){
                                break;
                            }
                        }
                    }
                    return link;
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
