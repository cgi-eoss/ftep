/**
 * @ngdoc service
 * @name ftepApp.BasketService
 * @description
 * # BasketService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('BasketService', [ '$rootScope', '$http', 'ftepProperties', '$q', '$timeout', 'MessageService',
                                           function ($rootScope, $http, ftepProperties, $q, $timeout, MessageService) {

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          var pNumber, pSize;
          var basketListCache;
          var is_polling = false;
          var POLLING_FREQUENCY = 20 * 1000;
          var connectionError = false, retriesLeft = 3;

          /** GET DATABASKETS & POLL **/
          this.getDatabaskets = function(pageNumber, pageSize, noCache){
            if(noCache && noCache === true){
                return retrieveDatabaskets(pageNumber, pageSize);
            }
            else if(is_polling && pNumber === pageNumber){
                return $q.when(basketListCache);
            }
            else {
                pNumber = pageNumber;
                pSize = pageSize;
                return pollDatabaskets(pageNumber, pageSize);
            }
          };

          // fetch the databaskets once
          function retrieveDatabaskets(pageNumber, pageSize){
              var deferred = $q.defer();
              var parameters = {
                  'page[size]': pageSize,
                  'page[number]': pageNumber,
                  include: 'files'
              };

              $http({
                      method: 'GET',
                      url: ftepProperties.URL + '/databaskets',
                      params: parameters,
                  })
                  .then(function (response) {
                      basketListCache = response.data;
                      deferred.resolve(response.data);
                  });
              return deferred.promise;
          }

          var pollDatabaskets = function (pageNumber, pageSize) {
              if (connectionError && retriesLeft === 0) {
                  retriesLeft = 3;
                  connectionError = false;
              } else {
                  if (pNumber === pageNumber && pSize === pageSize) {

                      var deferred = $q.defer();
                      var parameters = {
                          'page[size]': pageSize,
                          'page[number]': pageNumber,
                          include: 'files'
                      };

                      $http({
                              method: 'GET',
                              url: ftepProperties.URL + '/databaskets',
                              params: parameters,
                          })
                          .then(function (response) {
                              if (angular.equals(basketListCache, response.data) == false) {
                                  basketListCache = response.data;
                                  $rootScope.$broadcast('refresh.databaskets', response.data);
                              }
                              deferred.resolve(response.data);
                              retriesLeft = 3;
                              connectionError = false;
                          })
                          .catch(function (e) {
                              connectionError = true;
                              MessageService.addError(
                                  'Could not get databaskets',
                                  'Could not get databaskets. Retries left: ' + retriesLeft
                              );
                              retriesLeft--;
                              deferred.reject();
                          })
                          .finally(function () {
                              is_polling = true;
                              $timeout(function () {
                                  pollDatabaskets(pageNumber, pageSize);
                              }, POLLING_FREQUENCY);
                          });
                      return deferred.promise;
                  }
              }
          };
          /** END OF GET DATABASKETS & POLL **/

          /** GET DATABASKET ITEMS **/
          this.getItems = function(databasket){
              var deferred = $q.defer();
              $http({
                  method: 'GET',
                  url: ftepProperties.URL + '/databaskets/' + databasket.id + '/relationships/files'
              })
              .then(function(response) {
                  deferred.resolve(response.data);
              })
              .catch(function(e){
                  MessageService.addError(
                      'Could not get databasket',
                      'Could not get databasket items'
                  );
                  deferred.reject();
              });
              return deferred.promise;
          };

          /** END OF GET DATABASKET ITEMS **/

          /** POST DATABASKET AND/OR RELATED PRODUCTS **/
          this.createDatabasket = function(name, desc, items) {
              return $q(function(resolve, reject) {
                  var basket = {type: 'databaskets', attributes:{name: name, description: (desc ? desc : ''), databaskettype:'', accesslevel:''}};
                  $http({
                      method: 'POST',
                      url: ftepProperties.URL + '/databaskets',
                      data: '{"data": ' + JSON.stringify(basket) + '}',
                  }).
                  then(function(response) {
                      if(items){
                          addItems(response.data.data, items);
                      }
                      resolve(response.data.data);
                      MessageService.addInfo('Databasket created', 'New databasket '.concat(name).concat(' created'));
                  }).
                  catch(function(e) {
                      if(e.status == 409){
                          MessageService.addError(
                              'Could not create databasket',
                              'Conflicts with an already existing one'
                          );
                      }
                      else {
                          MessageService.addError('Could not create databasket', 'Could not create databasket: '.concat(name));
                      }
                      reject();
                  });
              });
          };

          function addItems(databasket, items){
              var itemsList = [];
              for(var i = 0; i < items.length; i++){
                  if(items[i].identifier){ //TODO currently result items only
                      itemsList.push({"type": "file", "url": items[i].meta, "name": items[i].identifier});
                  }
                  else if(items[i].name){ //or cloned from another basket
                      itemsList.push({"type": "file", "url": items[i].url, "name": items[i].name});
                  }
              }

              $http({
                  method: 'POST',
                  url: ftepProperties.URL + '/databaskets/' + databasket.id + '/relationships/files',
                  data: '{"data": ' + JSON.stringify(itemsList) + '}',
              });
          }

          this.addBasketItems = function(basket, items){
              addItems(basket, items);
          };

          /** END OF POST DATABASKET AND/OR RELATED PRODUCTS **/

          /** DELETE DATABASKET **/
          this.removeBasket = function(basket){
              /* First remove relationships */
              return $q(function(resolve, reject) {
                  $http({
                      method: 'PATCH',
                      url: ftepProperties.URL + '/databaskets/' + basket.id + '/relationships/files',
                      data: '{"data": ' + JSON.stringify([]) + '}',
                  }).
                  then(function(response) {
                      /* If successful, then the basket itself */
                      $http({
                          method: 'DELETE',
                          url: ftepProperties.URL + '/databaskets/' + basket.id
                      }).
                      then(function(response) {
                          resolve(basket);
                          MessageService.addInfo('Databasket deleted', 'Databasket '.concat(basket.attributes.name).concat(' deleted'));
                      }).
                      catch(function(e) {
                          MessageService.addError(
                              'Failed to remove databasket'
                          );
                          console.log(e);
                          reject();
                      });
                  }).
                  catch(function(e) {
                      MessageService.addError(
                          'Failed to remove databasket items'
                      );
                      console.log(e);
                      reject();
                  });
              });
          };
          /** END OF DELETE DATABASKET **/

          /** REMOVE ALL FILES **/
          this.clearBasket = function(basket){
              return $q(function(resolve, reject) {
                  $http({
                      method: 'PATCH',
                      url: ftepProperties.URL + '/databaskets/' + basket.id + '/relationships/files',
                      data: '{"data": ' + JSON.stringify([]) + '}',
                  }).
                  catch(function(e) {
                      MessageService.addError(
                          'Failed to remove databasket items'
                      );
                      console.log(e);
                      reject();
                  });
              });
          };
          /** END OF REMOVE ALL FILES **/

          /** REMOVE A SINGLE FILE **/

          this.removeRelation = function(basket, file){
              var obj = {"type": "files", "name": file.name, "url": file.url, "datasource": null};
              return $q(function(resolve, reject) {
                  $http({
                      method: 'DELETE',
                      url: ftepProperties.URL + '/databaskets/' + basket.id + '/relationships/files',
                      data: '{"data": ' + JSON.stringify(obj) + '}',
                  }).
                  then(function(response) {
                      resolve(file);
                  }).
                  catch(function(e) {
                      MessageService.addError(
                          'Failed to remove databasket item'
                      );
                      console.log(e);
                      reject();
                  });
              });
          };

          /** END OF REMOVE A SINGLE FILE **/

          /** UPDATE DATABASKET **/
          this.updateBasket = function(basket){
              return $q(function(resolve, reject) {

                  delete basket.attributes.id;
                  $http({
                      method: 'PATCH',
                      url: ftepProperties.URL + '/databaskets/' + basket.id,
                      data: '{"data": ' + JSON.stringify(basket) + '}',
                  }).
                  then(function(response) {
                      resolve(basket);
                      MessageService.addInfo('Databasket updated', 'Databasket '.concat(basket.attributes.name).concat(' updated'));
                  }).
                  catch(function(e) {
                      MessageService.addError(
                          'Failed to update databasket'
                      );
                      console.log(e);
                      reject();
                  });
              });
          };
          /** END OF UPDATE DATABASKET **/

          /** PRESERVE USER SELECTIONS **/
          this.params = {
                  selectedDatabasket: undefined,
                  selectedItems: undefined,
                  searchText: '',
                  displayFilters: false
          };

          this.pagingData = {
                  dbCurrentPage: 1,
                  dbPageSize: 10,
                  dbTotal: 0
          };
          /** END OF PRESERVE USER SELECTIONS **/

          // Initiate the databasket list
          this.getDatabaskets(this.pagingData.dbCurrentPage, this.pagingData.dbPageSize);

          return this;
      }]);
});
