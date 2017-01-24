/**
 * @ngdoc service
 * @name ftepApp.ProductService
 * @description
 * # ProductService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('ProductService', [ '$http', 'ftepProperties', '$q', 'MessageService', function ($http, ftepProperties, $q, MessageService) {

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          var servicesCache;

          this.getServices = function(){
              var deferred = $q.defer();
              $http.get( ftepProperties.URL + '/services').then(function(response) {
                  deferred.resolve(response.data.data);
                  servicesCache = response.data.data;
              })
              .catch(function(e){
                  MessageService.addMessage(
                      'Error',
                      'Could not get services',
                      'Could not get services'
                  );
                  deferred.reject();
              });
              return deferred.promise;
          };

          this.getServiceById = function(id){
              var service = undefined;
              if(servicesCache){
                  for(var i = 0; i < servicesCache.length; i++){
                      if(servicesCache[i].id === id){
                          service = servicesCache[i];
                          break;
                      }
                  }
              }
              return service;
          }

          return this;
      }]);
});
