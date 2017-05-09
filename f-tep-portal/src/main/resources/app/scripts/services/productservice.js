/**
 * @ngdoc service
 * @name ftepApp.ProductService
 * @description
 * # ProductService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {


    ftepmodules.service('ProductService', [ 'CommunityService', 'UserService', 'MessageService', '$http', 'ftepProperties', '$q', 'traverson', function ( CommunityService, UserService, MessageService, $http, ftepProperties, $q, traverson) {

        var self = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var productsAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.serviceOwnershipFilters = {
                ALL_SERVICES: { id: 0, name: 'All', criteria: ''},
                MY_SERVICES: { id: 1, name: 'Mine', criteria: undefined },
                SHARED_SERVICES: { id: 2, name: 'Shared', criteria: undefined }
        };

        UserService.getCurrentUser().then(function(currentUser){
            self.serviceOwnershipFilters.MY_SERVICES.criteria = { owner: {name: currentUser.name } };
            self.serviceOwnershipFilters.SHARED_SERVICES.criteria = { owner: {name: "!".concat(currentUser.name) } };
        });

        this.params = {
            explorer: {
                services: undefined,
                selectedService: undefined,
                searchText: '',
                inputValues: {},
                dropLists: {}
            },
            community: {
                services: undefined,
                contents: undefined,
                selectedService: undefined,
                searchText: '',
                displayFilters: false,
                contentsSearchText: '',
                contentsDisplayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.serviceOwnershipFilters.ALL_SERVICES,
                showServices: true
            },
            development: {
                services: undefined,
                activeForm: undefined,
                displayFilters: false,
                displayRight: false,
                selectedService: undefined,
                selectedServiceFileTab: 1
            }
        };

        this.refreshServices = function (page, action, service) {

            if(self.params[page]){
                /* Get service list */
                this.getUserServices().then(function (data) {

                    self.params[page].services = data;

                    /* Select last service if created */
                    if (action === "Create") {
                        self.params[page].selectedService = self.params[page].services[self.params[page].services.length-1];
                    }

                    /* Clear service if deleted */
                    if (action === "Remove") {
                        if (service && self.params[page].selectedService && service.id === self.params[page].selectedService.id) {
                            self.params[page].selectedService = undefined;
                            self.params[page].contents = [];
                        }
                    }

                    /* Update the selected group */
                    self.refreshSelectedService(page);
                });
            }
        };

        this.refreshSelectedService = function (page) {

            /* Get service contents if selected */
            if (self.params[page].selectedService) {
                self.getService(self.params[page].selectedService).then(function (service) {
                    self.params[page].selectedService = service;
                    getServiceFiles(service).then(function (data) {
                        self.params[page].contents = data;

                        if(page === 'development'){
                            self.params[page].selectedService.files = [];
                            if(data){
                                for(var i = 0; i < data.length; i++){
                                    getFileDetails(page, data[i]);
                                }
                                self.params[page].selectedService.files.sort(sortFiles);
                            }
                        }
                    });

                    if(page === 'community'){
                        CommunityService.getObjectGroups(service, 'service').then(function (data) {
                            self.params.community.sharedGroups = data;
                        });
                    }
                });
            }

        };

        this.getUserServices = function(){
            var deferred = $q.defer();
            productsAPI.from(rootUri + '/services/')
                       .newRequest()
                       .getResource()
                       .result
                       .then(
            function (document) {
                deferred.resolve(document._embedded.services);
            }, function (error) {
                MessageService.addError('Could not get Services', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.getService = function(service){
            var deferred = $q.defer();
            productsAPI.from(rootUri + '/services/' + service.id + '?projection=detailedFtepService')
                       .newRequest()
                       .getResource()
                       .result
                       .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError('Could not get details for Service ' + service.name, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.createService = function(name, description){
            return $q(function(resolve, reject) {
                  var service = {
                          name: name,
                          description: description,
                          dockerTag: 'ftep/' + name.replace(/[^a-zA-Z0-9-_.]/g,'_').toLowerCase(),
                  };
                  productsAPI.from(rootUri + '/services/')
                           .newRequest()
                           .post(service)
                           .result
                           .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            MessageService.addInfo('Service added', 'New Service ' + name + ' added.');
                            resolve(JSON.parse(document.data));
                            addDefaultFiles(JSON.parse(document.data));
                        } else {
                            MessageService.addError('Could not create Service ' + name, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not add Service ' + name, error);
                        reject();
                    }
                );
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
                      executable: true
              };
              self.addFile(file1);
              self.addFile(file2);
          }

          this.addFile = function(file){
              var deferred = $q.defer();
              productsAPI.from(rootUri + '/serviceFiles/')
                       .newRequest()
                       .post(file)
                       .result
                       .then(
                function (result) {
                    MessageService.addInfo('Service File added', file.filename + ' added');
                    deferred.resolve(JSON.parse(result.data));
                }, function (error) {
                    MessageService.addError('Could not add Service File ' + file.filename, error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        this.updateService = function(selectedService) {
            //Some descriptor fields are a copy from service itself
            selectedService.serviceDescriptor.description = selectedService.description;
            selectedService.serviceDescriptor.id = selectedService.name;
            selectedService.serviceDescriptor.serviceProvider = selectedService.name;

            var editService = {
                name: selectedService.name,
                description: selectedService.description,
                dockerTag: selectedService.dockerTag,
                serviceDescriptor: selectedService.serviceDescriptor
            };
            return $q(function(resolve, reject) {
                productsAPI.from(rootUri + '/services/' + selectedService.id)
                           .newRequest()
                           .patch(editService)
                           .result
                           .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            MessageService.addInfo('Service updated', 'Service ' + selectedService.name + ' successfully updated');
                            if(selectedService.files) {
                                for(var i = 0; i < selectedService.files.length; i++){
                                    updateFile(selectedService.files[i]);
                                }
                            }
                            resolve(document);
                        } else {
                            MessageService.addError('Could not update Service ' + selectedService.name, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not update Service ' + selectedService.name, error);
                        reject();
                    }
                );
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
                    MessageService.addInfo('Service File updated', file.filename + ' updated');
                }, function (error) {
                    MessageService.addError('Could not update Service File ' + file.name, error);
                }
            );
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
                            MessageService.addInfo('Service removed', 'Service ' + service.name + ' deleted.');
                            if(self.params.development.selectedService && service.id === self.params.development.selectedService.id){
                                self.params.development.selectedService = undefined;
                                self.params.development.displayRight = false;
                            }
                            resolve(service);
                        } else {
                            MessageService.addError('Could not remove Service ' + service.name, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not remove Service ' + service.name, error);
                        reject();
                    }
                );
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
                            MessageService.addInfo('Service File removed', 'File ' + file.filename + ' deleted.');
                            resolve();
                        } else {
                            MessageService.addError('Could not remove Service File ' + file.filename, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not remove Service File ' + file.filename, error);
                        reject();
                    }
                );
            });
        };

        function getServiceFiles(service){
            var deferred = $q.defer();
            var request = productsAPI.from(rootUri + '/serviceFiles/search/')
                                     .newRequest()
                                     .getResource();

            request.result.then(
                function (document) {
                    request.continue().then(function(request) {
                        request.follow('findByService')
                               .withRequestOptions({
                                    qs: { service: service._links.self.href }
                                })
                               .getResource()
                               .result
                               .then(
                            function (document) {
                                deferred.resolve(document._embedded.serviceFiles);
                            }, function (error) {
                                MessageService.addError('Could not get Service Files', error);
                                deferred.reject();
                            }
                        );
                    });
                }, function (error) {
                    MessageService.addError('Could not get Service Files', error);
                    deferred.reject();
                }
            );
            return deferred.promise;
        }

        function getFileDetails(page, file){
            productsAPI.from(file._links.self.href)
                       .newRequest()
                       .getResource()
                       .result
                       .then(
                function (document) {
                    self.params[page].selectedService.files.push(document);
                }, function (error) {
                    MessageService.addError('Could not get Service File details', error);
                }
            );
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
                MessageService.addError('Could not get Docker Template', error);
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

        return this;

    }]);
});
