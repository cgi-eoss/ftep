/**
 * @ngdoc service
 * @name ftepApp.ProductService
 * @description
 * # ProductService
 * Service in the ftepApp.
 */
define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {
    'use strict';

    ftepmodules.service('ProductService', [ '$http', 'ftepProperties', '$q', 'MessageService', 'traverson',
                                            function ($http, ftepProperties, $q, MessageService, traverson) {

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
                  MessageService.addError(
                      'Could not get services'
                  );
                  deferred.reject();
              });
              return deferred.promise;
          };

          this.getServiceById = function(id){
              var service;
              if(servicesCache){
                  for(var i = 0; i < servicesCache.length; i++){
                      if(servicesCache[i].id === id){
                          service = servicesCache[i];
                          break;
                      }
                  }
              }
              return service;
          };

          /** ------------------------------- API V2.0 ------------------------------------------- **/

          traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
          var rootUri = ftepProperties.URLv2;
          var productsAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
          var deleteAPI = traverson.from(rootUri).useAngularHttp();

          var userServicesCache = [];
          this.getUserServicesCache = function(){
              return userServicesCache;
          };

          var that = this;

          this.getUserServices = function(){
              var deferred = $q.defer();
              productsAPI.from(rootUri + '/services/')
                       .newRequest()
                       .getResource()
                       .result
                       .then(
              function (document) {
                  userServicesCache = document._embedded.service;
                  deferred.resolve(document._embedded.services);
              }, function (error) {
                  MessageService.addError ('Could not Get Services', error);
                  deferred.reject();
              });
              return deferred.promise;
          };

          this.createService = function(name, description){
              return $q(function(resolve, reject) {
                  var service = {
                          name: name,
                          description: description,
                          dockerTag: 'dockerTag' //default tag
                  };
                  productsAPI.from(rootUri + '/services/')
                           .newRequest()
                           .post(service)
                           .result
                           .then(
                      function (document) {
                          if (200 <= document.status && document.status < 300) {
                              MessageService.addInfo('Service Added', 'New service '.concat(service.name).concat(' added.'));
                              resolve(JSON.parse(document.data));
                              addDefaultFiles(JSON.parse(document.data));
                          }
                          else {
                              MessageService.addError ('Could not Add Service', 'Failed to create service ' + name + getMessage(document));
                              reject();
                          }
                      }, function (error) {
                          MessageService.addError ('Could not Add Service', error);
                          reject();
                      });
              });
          };

          function addDefaultFiles(service){
              var file1 = {
                      filename: 'Dockerfile',
                      content: btoa(DEFAULT_DOCKERFILE),
                      service: service._links.self.href
              };
              var file2 = {
                      filename: 'workflow.sh',
                      content: btoa('# ' + service.name + ' service'),
                      service: service._links.self.href,
                      executable: false
              };
              that.addFile(file1);
              that.addFile(file2);
          }

          this.addFile = function(file){
              var deferred = $q.defer();
              productsAPI.from(rootUri + '/serviceFiles/')
                       .newRequest()
                       .post(file)
                       .result
                       .then(
                  function (result) {
                      MessageService.addInfo('Service File Added', file.filename + ' added');
                      deferred.resolve(JSON.parse(result.data));
                  }, function (error) {
                      MessageService.addError ('Could not Add Service File', error);
                      deferred.reject();
                  });
              return deferred.promise;
          };

          this.updateService = function(){
              var selectedService = this.params.development.selectedService;
              var editService = {name: selectedService.name, description: selectedService.description, dockerTag: selectedService.dockerTag};
              return $q(function(resolve, reject) {
                  productsAPI.from(rootUri + '/services/' + selectedService.id)
                           .newRequest()
                           .patch(editService)
                           .result
                           .then(
                  function (document) {
                      if (200 <= document.status && document.status < 300) {
                          MessageService.addInfo('Service Updated', 'Service ' + selectedService.name + ' successfully updated');
                          for(var i = 0; i < selectedService.files.length; i++){
                              updateFile(selectedService.files[i]);
                          }
                          resolve(document);
                      }
                      else {
                          MessageService.addError('Could not Update Service',
                                  'Failed to update service ' + selectedService.name + getMessage(document));
                          reject();
                      }
                  }, function (error) {
                      MessageService.addError('Could not Update Service '.concat(selectedService.name),
                              (error.doc && error.doc.message ? error.doc.message : undefined));
                      reject();
                  });
              });
          };

          function updateFile(file){
              var editedFile = angular.copy(file);
              editedFile.content =  btoa(file.content);
              productsAPI.from(file._links.self.href)
                       .newRequest()
                       .patch(editedFile)
                       .result
                       .then(
                  function (result) {
                      MessageService.addInfo('Service File Updated', file.filename + ' updated');
                  }, function (error) {
                      MessageService.addError ('Could not Update Service File', error);
                  });
          }

          /** Remove service with its related files **/
          this.removeService = function(service){
              return $q(function(resolve, reject) {
                  deleteAPI.from(rootUri + '/services/' + service.id)
                  .newRequest()
                  .delete()
                  .result
                  .then(
                  function (document) {
                     if (200 <= document.status && document.status < 300) {
                         MessageService.addInfo('Service Removed', 'Service '.concat(service.name).concat(' deleted.'));
                         if(that.params.development.selectedService && service.id === that.params.development.selectedService.id){
                             that.params.development.selectedService = undefined;
                             that.params.development.displayRight = false;
                         }
                         resolve(service);
                     } else {
                         MessageService.addError ('Could not Remove Service', 'Failed to remove service '+
                                 service.name + getMessage(document));
                         reject();
                     }
                  }, function (error) {
                     MessageService.addError ('Could not Remove Service', error);
                     reject();
                  });
              });
          };

          this.removeServiceFile = function(file){
              return $q(function(resolve, reject) {
                  deleteAPI.from(file._links.self.href)
                           .newRequest()
                           .delete()
                           .result
                           .then(
                  function (document) {
                      if (200 <= document.status && document.status < 300) {
                          MessageService.addInfo('Service File Removed', 'File '.concat(file.filename).concat(' deleted.'));
                          resolve();
                      } else {
                          MessageService.addError ('Could not Remove Service File', 'Failed to remove service file ' +
                                 file.filename + getMessage(document));
                          reject();
                      }
                  }, function (error) {
                      MessageService.addError ('Could not Remove Service File', error);
                      reject();
                  });
              });
          };

          /** Select a service, and find the associated files **/
          this.selectService = function(service){
              this.params.development.selectedService = angular.copy(service);
              this.params.development.selectedService.files = [];
              this.params.development.displayRight = true;

              getServiceFiles(service).then(function(serviceFiles){
                  if(serviceFiles){
                      for(var i = 0; i < serviceFiles.length; i++){
                          getFileDetails(serviceFiles[i]);
                      }
                  }
              });
          };

          function getServiceFiles(service){
              var deferred = $q.defer();
              var request = productsAPI.from(rootUri + '/serviceFiles/search/')
              .newRequest()
              .getResource();

              request.result.then(function (document) {
                  request.continue().then(function(request) {
                      request
                      .follow('findByService')
                      .withRequestOptions({
                          qs: { service: service._links.self.href }
                      })
                      .getResource()
                      .result
                      .then(function (document) {
                          deferred.resolve(document._embedded.serviceFiles);
                      }, function (error) {
                          MessageService.addError ('Could not Get Service Files', error);
                          deferred.reject();
                      });
                  });
              }, function (error) {
                  MessageService.addError ('Could not Get Service Files', error);
                  deferred.reject();
              });

              return deferred.promise;
          }

          function getFileDetails(file){
              productsAPI.from(file._links.self.href)
                      .newRequest()
                      .getResource()
                      .result
                      .then(
             function (document) {
                 that.params.development.selectedService.files.push(document);
                 that.params.development.selectedService.files.sort(sortFiles);
             }, function (error) {
                 MessageService.addError ('Could not Get Service File', error);
             });
          }

          function sortFiles(a, b){
              var aId = parseInt(a._links.self.href.substring(a._links.self.href.lastIndexOf('/')+1));
              var bId = parseInt(b._links.self.href.substring(b._links.self.href.lastIndexOf('/')+1));

              return aId - bId;
          }

          var DEFAULT_DOCKERFILE;
          function loadDockerTemplate(){
              $http.get('scripts/templates/Dockerfile')
              .success(function(data) {
                  DEFAULT_DOCKERFILE = data;
              })
              .error(function(error) {
                  MessageService.addError ('Could not Get Load Docker Template', error);
              });
          }

          loadDockerTemplate();

          function getMessage(document){
              var message = '';
              if(document.data && document.data.indexOf('message') > 0){
                  message = ': ' + JSON.parse(document.data).message;
              }
              return message;
          }

          this.params = {
              explorer: {
                  selectedService: undefined,
                  searchText: ''
              },
              community: {
              },
              development: {
                  displayFilters: false,
                  displayRight: false,
                  selectedService: undefined,
                  selectedServiceFileTab: 1
              }
          };

          return this;
      }]);
});
