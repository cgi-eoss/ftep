/**
 * @ngdoc service
 * @name ftepApp.ProductService
 * @description
 * # ProductService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('ProductService', [ '$http', 'ftepProperties', '$q', function ($http, ftepProperties, $q) {

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          this.getServices = function(){
              var deferred = $q.defer();
              $http.get( ftepProperties.URL + '/services').then(function(response) {
                  deferred.resolve(response.data.data);
              })
              .catch(function(e){
                  alert('Could not get services');
                  deferred.reject();
              });
              return deferred.promise;
          }

          return this;
      }]);
});
