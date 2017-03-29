/**
 * @ngdoc service
 * @name ftepApp.BasketService
 * @description
 * # BasketService
 * Service in the ftepApp.
 */
'use strict';
define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('BasketService', [ '$rootScope', '$http', 'ftepProperties', '$q', '$timeout', 'MessageService', 'UserService', 'traverson', function ($rootScope, $http, ftepProperties, $q, $timeout, MessageService, UserService, traverson) {

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        var that = this;

        this.dbOwnershipFilters = {
            ALL_BASKETS: { id: 0, name: 'All', criteria: ''},
            MY_BASKETS: { id: 1, name: 'Mine', criteria: undefined },
            SHARED_BASKETS: { id: 2, name: 'Shared', criteria: undefined }
        };

        UserService.getCurrentUser().then(function(currentUser){
            that.dbOwnershipFilters.MY_BASKETS.criteria = { owner: {name: currentUser.name } };
            that.dbOwnershipFilters.SHARED_BASKETS.criteria = { owner: {name: "!".concat(currentUser.name) } };
        });

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            explorer: {
                selectedDatabasket: undefined,
                selectedItems: undefined,
                searchText: '',
                displayFilters: false,
                showBaskets: true,
                selectedOwnerhipFilter: that.dbOwnershipFilters.ALL_BASKETS
            },
            community: {
                databaskets: undefined,
                items: undefined,
                selectedDatabasket: undefined,
                selectedItems: undefined,
                searchText: '',
                displayFilters: false,
                itemSearchText: '',
                itemDisplayFilters: false,
                showBaskets: true
            }
        };

        this.pagingData = {
            dbCurrentPage: 1,
            dbPageSize: 10,
            dbTotal: 0
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var startPolling = true;
        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;

        var pollDatabasketsV2 = function () {
            $timeout(function () {
                halAPI.from(rootUri + '/databaskets/')
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        $rootScope.$broadcast('refresh.databasketsV2', document._embedded.databaskets);
                        pollDatabasketsV2();
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Databaskets', error);
                        pollCount -= 1;
                        if (pollCount >= 0) {
                            pollDatabasketsV2();
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.getDatabasketsV2 = function () {
            var deferred = $q.defer();
                halAPI.from(rootUri + '/databaskets/')
                         .newRequest()
                         .getResource()
                         .result
                         .then(
                function (document) {
                    /* Start polling if not already */
                    if (startPolling) {
                        startPolling = false;
                        pollDatabasketsV2();
                    }
                    deferred.resolve(document._embedded.databaskets);
                }, function (error) {
                    MessageService.addError ('Could not get Databaskets', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.createDatabasketV2 = function (name, desc) {
            return $q(function(resolve, reject) {
                var databasket = {name: name, description: (desc ? desc : '')};
                halAPI.from(rootUri + '/databaskets/')
                         .newRequest()
                         .post(databasket)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Databasket created', 'New databasket '.concat(name).concat(' created.'));
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError ('Failed to Create Databasket', error);
                    reject();
                });
            });
        };

        this.removeDatabasketV2 = function(databasket) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/databaskets/' + databasket.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Databasket deleted', 'Databasket '.concat(databasket.name).concat(' deleted.'));
                        resolve(databasket);
                    } else {
                        MessageService.addError ('Failed to Remove Databasket', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to Remove Databasket', error);
                    reject();
                });
            });
        };

        this.updateDatabasketV2 = function (databasket) {
            var newdatabasket = {name: databasket.name, description: databasket.description};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/databaskets/' + databasket.id)
                         .newRequest()
                         .patch(newdatabasket)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Databasket successfully updated', 'Databasket ' + databasket.name + ' successfully updated.');
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Failed to update Databasket', error);
                    reject();
                });
            });
        };

        this.getItemsV2 = function() {
            var deferred = $q.defer();
            var databasket = that.params.community.selectedDatabasket;
            halAPI.from(rootUri + '/databaskets/' + databasket.id)
                     .newRequest()
                     .follow('files')
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document._embedded.ftepFiles);
            }, function (error) {
                MessageService.addError ('Could not get Databasket contents', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.addItemsV2 = function (databasket, files) {
            return $q(function(resolve, reject) {

                /* Create array of item links */
                var itemsArray = [];
                for (var item in databasket.items) {
                    itemsArray.push(databasket.items[item]._links.self.href);
                }

                /* Append item to new items array */
                for (var file in files) {
                    itemsArray.push(files[file]._links.self.href);
                }

                /* Set new files object */
                var updatedItems = {"files": itemsArray};

                /* Patch members with updated member list */
                halAPI.from(rootUri + '/databaskets/' + databasket.id)
                         .newRequest()
                         .patch(updatedItems)
                         .result
                         .then(
                function (document) {
                   if (200 <= document.status && document.status < 300) {
                        MessageService.addError ('Failed to add file/s to Databasket', document);
                        reject();
                    } else {
                        MessageService.addInfo('Files successfully added');
                        resolve(document);
                    }
                }, function (error) {
                    MessageService.addError('Failed to add file/s to Databasket', error);
                    reject();
                });

            });
        };

        this.removeItemV2 = function(databasket, files, file) {
            return $q(function(resolve, reject) {

                /* Create array of user links */
                var itemsArray = [];
                for (var item in files) {
                    /* Don't add user to updated list */
                    if (files[item].id !== file.id) {
                        itemsArray.push(files[item]._links.self.href);
                    }
                }

                /* Set list of files to empty array */
                var updatedItems = {"files": itemsArray};

                /* Patch databasket with empty item list */
                halAPI.from(rootUri + '/databaskets/' + databasket.id)
                         .newRequest()
                         .patch(updatedItems)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('File removed from Databasket', 'File '.concat(file.name).concat('removed from ').concat(databasket.name));
                        resolve(databasket);
                    } else {
                        MessageService.addError ('Failed to remove item from Databasket', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to remove item from Databasket', error);
                    reject();
                });
            });
        };

        this.clearDatabasketV2 = function(databasket) {
            return $q(function(resolve, reject) {

                /* Set list of files to empty array */
                var updatedItems = {"files": []};

                /* Patch databasket with empty item list */
                halAPI.from(rootUri + '/databaskets/' + databasket.id)
                         .newRequest()
                         .patch(updatedItems)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Databasket successfully cleared.');
                        resolve(databasket);
                    } else {
                        MessageService.addError ('Failed to clear Databasket', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to clear Databasket', error);
                    reject();
                });
            });
        };




          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          var pNumber, pSize;
          var basketListCache = { data:[] };
          var is_polling = false;
          //var POLLING_FREQUENCY = 20 * 1000;
          var connectionError = false, retriesLeft = 3;

          var USE_TEST_DATA = false;

          this.getBasketCache = function(){
              return basketListCache;
          };

          /** GET DATABASKETS & POLL **/
          this.getDatabaskets = function(pageNumber, pageSize, noCache){
              if(USE_TEST_DATA){
                  var deferred = $q.defer();
                  $timeout(function () {
                      getTestData(deferred);
                  }, 100);
                  return deferred.promise;
              }
              else{
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
              }
          };

          //TODO only for prototyping
          function getTestData(deferred){
              console.log('USING TEST DATA for databaskets');
              $.getJSON("temp_data/test_databaskets.json", function(json) {
                  basketListCache = json;
                  deferred.resolve(json);
                  $rootScope.$broadcast('refresh.databaskets', json);
              })
              .fail(function(e) {
                console.log( "error", e );
              });
          }

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

          // Initiate the databasket list
          this.getDatabaskets(this.pagingData.dbCurrentPage, this.pagingData.dbPageSize);

          return this;
      }]);
});
