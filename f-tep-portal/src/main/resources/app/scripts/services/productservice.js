/**
 * @ngdoc service
 * @name ftepApp.ProductService
 * @description
 * # ProductService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {


    ftepmodules.service('ProductService', ['$rootScope', 'CommunityService', 'UserService', 'MessageService', '$http', 'ftepProperties', '$q', 'traverson', '$timeout', function ( $rootScope, CommunityService, UserService, MessageService, $http, ftepProperties, $q, traverson, $timeout) {

        var self = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.serviceOwnershipFilters = {
            MY_SERVICES: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner' },
            SHARED_SERVICES: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner' },
            ALL_SERVICES: { id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'}
        };

        this.serviceTypeFilters = {
            ALL_SERVICES: { id: 0, name: 'All Service Types' },
            APPLICATION: { id: 1, name: 'Application Services', value: 'APPLICATION' },
            PROCESSOR: { id: 2, name: 'Processor Services', value: 'PROCESSOR' },
            BULK_PROCESSOR: { id: 3, name: 'Bulk Processor Services', value: 'BULK_PROCESSOR' }
        };

        this.servicePublicationFilters = {
            ALL_SERVICES: { id: 0, name: 'All Publication Statuses' },
            PUBLIC_SERVICES: { id: 1, name: 'Public', value: 'PUBLIC_SERVICES'},
            PENDING_SERVICES: { id: 2, name: 'Pending', value: 'PENDING_SERVICES'},
            PRIVATE_SERVICES: { id: 3, name: 'Private', value: 'PRIVATE_SERVICES'}
        };

        this.params = {
            explorer: {
                services: undefined,
                pollingUrl: rootUri + '/services?sort=type,name',
                pagingData: {},
                selectedService: undefined,
                selectedOwnershipFilter: self.serviceOwnershipFilters.ALL_SERVICES,
                selectedTypeFilter: self.serviceTypeFilters.ALL_SERVICES,
                searchText: '',
                inputValues: {},
                label: undefined,
                dropLists: {}
            },
            community: {
                services: undefined,
                pollingUrl: rootUri + '/services?sort=type,name',
                pagingData: {},
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
                selectedTypeFilter: self.serviceTypeFilters.ALL_SERVICES,
                selectedPublicationFilter: self.servicePublicationFilters.ALL_SERVICES,
                showServices: true
            },
            development: {
                services: undefined,
                pollingUrl: rootUri + '/services?sort=type,name',
                pagingData: {},
                activeForm: undefined,
                displayFilters: false,
                displayRight: false,
                selectedService: undefined,
                selectedOwnershipFilter: self.serviceOwnershipFilters.ALL_SERVICES,
                selectedTypeFilter: self.serviceTypeFilters.ALL_SERVICES,
                selectedServiceFileTab: 1,
                fileTree: undefined,
                openedFile: undefined,
                activeMode: undefined
            }
        };

        this.refreshServices = function (page, action, service) {

            if(self.params[page]){
                /* Get service list */
                getUserServices(page).then(function (data) {

                    self.params[page].services = data;

                    /* Select last service if created */
                    if (action === "Create") {
                        self.params[page].selectedService = service;
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
                                var promises = [];
                                for(var i = 0; i < data.length; i++){
                                    var partialPromise = getFileDetails(page, data[i]);
                                    promises.push(partialPromise);
                                }
                                $q.all(promises).then(function() {
                                    self.params[page].selectedService.files.sort(sortFiles);
                                    self.params[page].openedFile = self.params[page].selectedService.files[0];
                                    self.getFileList(page);
                                    self.setFileType();
                                });
                            }

                        }
                    });

                    if(page === 'community') {
                        CommunityService.getObjectGroups(service, 'service').then(function (data) {
                            self.params.community.sharedGroups = data;
                        });
                    }
                });
            }
        };

        this.getFileList =  function(page)  {

            var files = self.params[page].selectedService.files;
            var filename;
            var list = [];
            for (var file in files) {
                var indent = 0;
                filename = files[file].filename;
                while(filename.indexOf('/') !== -1) {
                    var folderexists = false;
                    for(var i=0; i < list.length; i++) {
                        if(list[i].name.indexOf(filename.slice(0, filename.indexOf("/")))  !== -1) {
                             folderexists = true;
                        }
                    }
                    if(!folderexists) {
                       list.push({name: filename.slice(0, filename.indexOf("/")), type: 'folder', indent: indent});
                    }
                    filename = filename.substring(filename.indexOf("/") + 1);
                    indent++;
                }
                list.push({name: filename, type: 'file', indent: indent, contents: files[file]});
            }

            var previousIndent = 0;
            var nextIndent;
            for(var item = 0; item < list.length; item++) {
                var currentIndent = list[item].indent;

                if(list.length > item + 1) {
                    nextIndent = list[item + 1].indent;
                } else {
                    nextIndent = 'end';
                }

                if(nextIndent === 'end' && currentIndent === 0) {
                    list[item].tree = "└─";
                } else if(currentIndent === 0) {
                    list[item].tree="├";
                } else {
                    list[item].tree="│";
                    for(var j = 0; j < currentIndent; j++) {
                        if (j < currentIndent -1) {
                            list[item].tree = list[item].tree + "...";
                            if(currentIndent > 0) {
                               list[item].tree = list[item].tree + "│";  //Needs forward logic to check if │ or ...
                            }
                        } else {
                            list[item].tree = list[item].tree + "...";
                            if(nextIndent === 'end') {
                                list[item].tree = list[item].tree + "└─";
                            } else if(currentIndent === nextIndent) {
                                list[item].tree = list[item].tree + "├─";
                            } else if(currentIndent < nextIndent) {
                                list[item].tree = list[item].tree + "├─"; //Needs forward logic to check if ├─ or └─
                            } else if(currentIndent > nextIndent) {
                                list[item].tree = list[item].tree + "└─";
                            }
                        }
                    }
                }
                previousIndent = currentIndent;
            }

             self.params[page].fileTree = list;
        };

        this.setFileType = function () {
            var filename = self.params.development.openedFile.filename;
            var extension = filename.slice((filename.lastIndexOf(".") - 1 >>> 0) + 2).toLowerCase();
            var modes = ['Text', 'Dockerfile', 'Javascript', 'Perl', 'PHP', 'Python', 'Properties', 'Shell', 'XML', 'YAML' ];

            if (filename === "Dockerfile") {
                self.params.development.activeMode = modes[1];
            } else {
                switch(extension) {
                    case "js":
                        self.params.development.activeMode = modes[2];
                        break;
                    case "pl":
                        self.params.development.activeMode = modes[3];
                        break;
                    case "php":
                        self.params.development.activeMode = modes[4];
                        break;
                     case "py":
                        self.params.development.activeMode = modes[5];
                        break;
                    case "properties":
                        self.params.development.activeMode = modes[6];
                        break;
                    case "sh":
                        self.params.development.activeMode = modes[7];
                        break;
                    case "xml":
                        self.params.development.activeMode = modes[8];
                        break;
                    case "yml":
                        self.params.development.activeMode = modes[9];
                        break;
                    default:
                        self.params.development.activeMode = modes[0];
                }
            }
        };

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollServices = function (page) {
            pollingTimer = $timeout(function () {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.services', document._embedded.services);
                        pollServices(page);
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Services', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollServices(page);
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.stopPolling = function(){
            if(pollingTimer){
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        function getUserServices(page){
            var deferred = $q.defer();
            halAPI.from(self.params[page].pollingUrl)
                       .newRequest()
                       .getResource()
                       .result
                       .then(
            function (document) {
                if (startPolling) {
                    pollServices(page);
                    startPolling = false;
                }
                self.params[page].pagingData._links = document._links;
                self.params[page].pagingData.page = document.page;

                deferred.resolve(document._embedded.services);
            }, function (error) {
                MessageService.addError('Could not get Services', error);
                deferred.reject();
            });
            return deferred.promise;
        }

        /* Get list of default services */
        this.getDefaultServices = function(){
            var deferred = $q.defer();
            halAPI.from(rootUri + '/services/defaults')
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
        }

        /* Fetch a new page */
        this.getServicesPage = function(page, url){
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get services list */
                getUserServices(page).then(function (data) {
                    self.params[page].services = data;
                });
            }
        };

        this.getServicesByFilter = function (page) {
            if (self.params[page]) {
                var url = rootUri + '/services/' +
                                    self.params[page].selectedOwnershipFilter.searchUrl +
                                    '?sort=type,name&filter='+
                                    (self.params[page].searchText ? self.params[page].searchText : '');

                if(self.params[page].selectedOwnershipFilter !== self.serviceOwnershipFilters.ALL_SERVICES){
                    url += '&owner=' + UserService.params.activeUser._links.self.href;
                }

                if(self.params[page].selectedTypeFilter !== self.serviceTypeFilters.ALL_SERVICES){
                    url += '&serviceType=' + self.params[page].selectedTypeFilter.value;
                }
                self.params[page].pollingUrl = url;

                /* Get services list */
                getUserServices(page).then(function (data) {
                    self.params[page].services = data;
                });
            }
        };

        this.getService = function(service){
            var deferred = $q.defer();
            halAPI.from(rootUri + '/services/' + service.id + '?projection=detailedFtepService')
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

        this.createService = function(name, description, title){
            return $q(function(resolve, reject) {
                  var service = {
                          name: name,
                          description: description,
                          dockerTag: 'ftep/' + name.toLowerCase(),
                          serviceDescriptor: {
                              description: description,
                              id: name,
                              title: title,
                              serviceProvider: name,
                              version: '0.1',
                              serviceType: 'Java'
                          }
                  };
                  halAPI.from(rootUri + '/services/')
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
                      content: btoa(DEFAULT_WORKFLOW),
                      service: service._links.self.href,
                      executable: true
              };
              self.addFile(file1);
              self.addFile(file2);
          }

          this.addFile = function(file){
              var deferred = $q.defer();
              halAPI.from(rootUri + '/serviceFiles/')
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

        this.saveService = function(selectedService) {

            // Some descriptor fields are a copy from service itself
            if(!selectedService.description) {
                selectedService.description = '';
            }
            if(!selectedService.serviceDescriptor) {
               selectedService.serviceDescriptor = {};
            }
            selectedService.serviceDescriptor.description = selectedService.description;
            selectedService.serviceDescriptor.id = selectedService.name;
            selectedService.serviceDescriptor.serviceProvider = selectedService.name;

            var editService = {
                name: selectedService.name,
                description: selectedService.description,
                dockerTag: selectedService.dockerTag,
                serviceDescriptor: selectedService.serviceDescriptor,
                type: selectedService.type
            };

            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/services/' + selectedService.id)
                           .newRequest()
                           .patch(editService)
                           .result
                           .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            if(selectedService.files) {
                                saveFiles(selectedService);
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

        function saveFiles(selectedService) {
            if(selectedService.files) {
                var promises = [];
                for(var i = 0; i < selectedService.files.length; i++){
                    var partialPromise = updateFile(selectedService.files[i]);
                    promises.push(partialPromise);
                }
                $q.all(promises).then(function(){
                    MessageService.addInfo('Service updated', 'Service ' + selectedService.name + ' successfully updated');
                });
            }
        }

        function updateFile(file) {
            var deferred = $q.defer();
            var editedFile = angular.copy(file);
            editedFile.content =  btoa(file.content);
            halAPI.from(file._links.self.href)
                       .newRequest()
                       .patch(editedFile)
                       .result
                       .then(
                function (result) {
                    deferred.resolve();
                }, function (error) {
                    MessageService.addError('Could not update Service File ' + file.name, error);
                    deferred.reject();
                }
            );
            return deferred.promise;
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
            var request = halAPI.from(rootUri + '/serviceFiles/search/')
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

        function getFileDetails(page, file) {
            var deferred = $q.defer();
            halAPI.from(file._links.self.href)
                       .newRequest()
                       .getResource()
                       .result
                       .then(
                function (document) {
                    self.params[page].selectedService.files.push(document);
                    deferred.resolve();
                }, function (error) {
                    MessageService.addError('Could not get Service File details', error);
                    deferred.reject();
                }
            );
            return deferred.promise;
        }

        function sortFiles(a, b){
            if (a.filename < b.filename) {
                return -1;
            }
            if (a.filename > b.filename) {
                return 1;
            }
            return 0;
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

        var DEFAULT_WORKFLOW;
        function loadWorkflowTemplate(){
            $http.get('scripts/templates/workflow.sh')
            .success(function(data) {
                DEFAULT_WORKFLOW = data;
            })
            .error(function(error) {
                MessageService.addError('Could not get workflow.sh Template', error);
            });
        }
        loadWorkflowTemplate();

        function getMessage(document) {
            var message = '';
            if (document.data && document.data.indexOf('message') > 0) {
                message = ': ' + JSON.parse(document.data).message;
            }
            return message;
        }

        this.restoreServices = function () {
            halAPI.from(rootUri + '/contentAuthority/services/restoreDefaults/')
            .newRequest()
            .post()
            .result
            .then(
                function (document) {
                    MessageService.addInfo('Restored Services', 'Successfully restored default Services.');
                },function (error) {
                    MessageService.addError('Failed to restore default Services', error);
                }
            );
        };

        this.synchronizeServices = function () {
            halAPI.from(rootUri + '/contentAuthority/services/wps/syncAllPublic')
            .newRequest()
            .post()
            .result
            .then(
                function (document) {
                    MessageService.addInfo('Synchronized Services', 'Successfully synchronized Services.');
                },function (error) {
                    MessageService.addError('Failed to synchronized Services', error);
                }
            );
        };

        return this;

    }]);
});
