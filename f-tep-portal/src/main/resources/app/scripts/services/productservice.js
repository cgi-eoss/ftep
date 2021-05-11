/**
 * @ngdoc service
 * @name ftepApp.ProductService
 * @description
 * # ProductService
 * Service in the ftepApp.
 */
'use strict';
define(['../ftepmodules', 'traversonHal', '../vendor/handlebars/handlebars'], function (ftepmodules, TraversonJsonHalAdapter, Handlebars) {


    ftepmodules.service('ProductService', ['$rootScope', 'CommunityService', 'UserService', 'MessageService', '$http', 'ftepProperties', '$q', '$window', 'traverson', '$timeout', 'EditorService', function ($rootScope, CommunityService, UserService, MessageService, $http, ftepProperties, $q, $window, traverson, $timeout, EditorService) {

        var self = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI = traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();
        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        this.serviceOwnershipFilters = {
            MY_SERVICES: {id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner'},
            SHARED_SERVICES: {id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner'},
            ALL_SERVICES: {id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'}
        };

        this.serviceTypeFilters = {
            ALL_SERVICES: {id: 0, name: 'All Service Types'},
            APPLICATION: {id: 1, name: 'Application Services', value: 'APPLICATION'},
            PROCESSOR: {id: 2, name: 'Processor Services', value: 'PROCESSOR'},
            BULK_PROCESSOR: {id: 3, name: 'Bulk Processor Services', value: 'BULK_PROCESSOR'}
        };

        this.servicePublicationFilters = {
            ALL_SERVICES: {id: 0, name: 'All Publication Statuses'},
            PUBLIC_SERVICES: {id: 1, name: 'Public', value: 'PUBLIC_SERVICES'},
            PENDING_SERVICES: {id: 2, name: 'Pending', value: 'PENDING_SERVICES'},
            PRIVATE_SERVICES: {id: 3, name: 'Private', value: 'PRIVATE_SERVICES'}
        };

        this.serviceRunModes = {
            STANDARD: {id: 0, name: 'Standard'},
            SYSTEMATIC: {id: 1, name: 'Systematic'}
        }

        this.params = {
            explorer: {
                savedService: undefined,
                services: undefined,
                pollingUrl: rootUri + '/services?sort=type,name',
                pagingData: {},
                selectedOwnershipFilter: self.serviceOwnershipFilters.ALL_SERVICES,
                selectedTypeFilter: self.serviceTypeFilters.ALL_SERVICES,
                searchText: '',
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
            developer: {
                selectedService: undefined,
                savedServiceConfig: {},
                services: undefined,
                pollingUrl: rootUri + '/services?sort=type,name',
                pagingData: {},
                activeForm: undefined,
                displayFilters: false,
                displayRight: false,
                selectedOwnershipFilter: self.serviceOwnershipFilters.ALL_SERVICES,
                selectedTypeFilter: self.serviceTypeFilters.ALL_SERVICES,
                selectedServiceFileTab: 1,
                fileTree: undefined,
                openedFile: undefined,
                activeMode: undefined,
                constants: {
                    tabs: {
                        files: {id: 'files', title: 'Files'},
                        dataInputs: {id: 'dataInputs', title: 'Input Definitions'},
                        dataOutputs: {id: 'dataOutputs', title: 'Output Definitions'},
                        easyMode: {id: 'easyMode', title: 'Simple Input Definitions'}
                    },
                    fieldTypes: [{type: 'LITERAL'}, {type: 'COMPLEX'}], //{type: 'BOUNDING_BOX'}],
                    literalTypes: [{dataType: 'string'}, {dataType: 'integer'}, {dataType: 'double'}]
                }
            }
        };

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

        this.stopPolling = function () {
            if (pollingTimer) {
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        function getUserServices(page) {
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

        function getServiceFiles(service) {
            var deferred = $q.defer();
            var request = halAPI.from(rootUri + '/serviceFiles/search/')
                .newRequest()
                .getResource();

            request.result.then(
                function (document) {
                    request.continue().then(function (request) {
                        request.follow('findByService')
                            .withRequestOptions({
                                qs: {service: service._links.self.href}
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

        this.refreshServices = function (page, action, service) {
            if (self.params[page]) {
                /* Get service list */
                getUserServices(page).then(function (data) {
                    self.params[page].services = data;
                    /* Select last service if created */
                    if (action === 'Create') {
                        self.params[page].selectedService = service;
                    }
                    /* Clear service if deleted */
                    if (action === 'Remove') {
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
                    self.params[page].fileTree = [];
                    getServiceFiles(service)
                        .then(function (data) {
                            self.params[page].contents = data;

                            if (page === 'developer') {
                                self.params[page].selectedService.files = [];
                                if (data) {
                                    var promises = [];
                                    for (var i = 0; i < data.length; i++) {
                                        var partialPromise = EditorService.getFileDetails(page, data[i], self.params[page].selectedService);
                                        promises.push(partialPromise);
                                    }
                                    $q.all(promises).then(function () {
                                        if (self.params[page].selectedService.files) {
                                            self.params[page].selectedService.files.sort(EditorService.sortFiles);
                                            self.params[page].openedFile = self.params[page].selectedService.files[0];
                                        }
                                        self.params[page].fileTree = EditorService.getFileList(self.params[page].selectedService.files);
                                        if (self.params.developer.openedFile) {
                                            self.params.developer.activeMode = EditorService.setFileType(self.params.developer.openedFile.filename);
                                        }
                                    });
                                }
                            }
                        })
                        .catch(error => {
                            self.params[page].contents = [];
                            self.params[page].openedFile = null;
                        });

                    if (page === 'developer') {
                        self.updateBuildStatus(self.params[page].selectedService).then(function (response) {
                        })
                    } else if (page === 'community') {
                        CommunityService.getObjectGroups(service, 'service').then(function (data) {
                            self.params.community.sharedGroups = data;
                        });
                    }
                });
            }
        };

        /* Get list of default services */
        this.getDefaultServices = function () {
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
        };

        /* Fetch a new page */
        this.getServicesPage = function (page, url) {
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
                    '?sort=type,name&filter=' +
                    (self.params[page].searchText ? self.params[page].searchText : '');

                if (self.params[page].selectedOwnershipFilter !== self.serviceOwnershipFilters.ALL_SERVICES) {
                    url += '&owner=' + UserService.params.activeUser._links.self.href;
                }

                if (self.params[page].selectedTypeFilter !== self.serviceTypeFilters.ALL_SERVICES) {
                    url += '&serviceType=' + self.params[page].selectedTypeFilter.value;
                }
                self.params[page].pollingUrl = url;

                /* Get services list */
                getUserServices(page).then(function (data) {
                    self.params[page].services = data;
                });
            }
        };

        this.getService = function (service) {
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

        this.createService = function (name, description, title) {
            return $q(function (resolve, reject) {
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
                                EditorService.addDefaultFiles(JSON.parse(document.data));
                                self.params.developer.activeArea = self.params.developer.constants.tabs.files;
                            } else if (document.status == 409) {
                                MessageService.addError('Could not create Service due to name conflict ' + name, document);
                                reject();
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

        this.saveService = function (selectedService) {

            // Some descriptor fields are a copy from service itself
            if (!selectedService.description) {
                selectedService.description = '';
            }
            if (!selectedService.serviceDescriptor) {
                selectedService.serviceDescriptor = {};
            }

            selectedService.serviceDescriptor.description = selectedService.description;
            selectedService.serviceDescriptor.id = selectedService.name;
            selectedService.serviceDescriptor.serviceProvider = selectedService.name;

            if (selectedService.easyModeServiceDescriptor) {
                selectedService.easyModeServiceDescriptor.id = selectedService.name;
            }

            var editService = {
                name: selectedService.name,
                description: selectedService.description,
                dockerTag: selectedService.dockerTag,
                applicationPort: selectedService.applicationPort,
                serviceDescriptor: selectedService.serviceDescriptor,
                easyModeServiceDescriptor: selectedService.easyModeServiceDescriptor,
                easyModeParameterTemplate: selectedService.easyModeParameterTemplate,
                type: selectedService.type
            };

            return $q(function (resolve, reject) {
                halAPI.from(rootUri + '/services/' + selectedService.id)
                    .newRequest()
                    .patch(editService)
                    .result
                    .then(
                        function (document) {
                            if (200 <= document.status && document.status < 300) {
                                if (selectedService.files) {
                                    EditorService.saveFiles(selectedService, 'Service');
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

        this.generateEasyJobConfig = function (inputValues, template, serviceName, serviceLabel, parallelParameters, returnString) {
            try {
                var formContents = {inputs: this.formatInputs(inputValues)};
                var templateScript = Handlebars.compile(template);

                var generatedConfig = JSON.parse(templateScript(formContents));
                var templateConfig = {
                    service: serviceName,
                    label: serviceLabel,
                    parallelParameters: this.formatParallelInputs(parallelParameters),
                    systematicParameter: null
                };
                var query = Object.assign(generatedConfig, templateConfig);

                if (returnString) {
                    return JSON.stringify(query, null, 2);
                } else {
                    return query;
                }

            } catch (e) {
                return {error: e};
            }
        };

        /** Remove service with its related files **/
        this.removeService = function (service) {
            return $q(function (resolve, reject) {
                deleteAPI.from(rootUri + '/services/' + service.id)
                    .newRequest()
                    .delete()
                    .result
                    .then(
                        function (document) {
                            if (200 <= document.status && document.status < 300) {
                                MessageService.addInfo('Service removed', 'Service ' + service.name + ' deleted.');
                                if (self.params.developer.selectedService && service.id === self.params.developer.selectedService.id) {
                                    self.params.developer.selectedService = undefined;
                                    self.params.developer.displayRight = false;
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

        this.removeServiceFile = function (file) {
            return $q(function (resolve, reject) {
                deleteAPI.from(file.contents._links.self.href)
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

        // Ensure all service input values are wrapped in an array
        this.formatInputs = function (inputList) {
            var formattedInputs = {};
            for (var key in inputList) {
                if (!inputList[key]) {
                    inputList[key] = [''];
                }
                Array.isArray(inputList[key]) ? formattedInputs[key] = inputList[key] : formattedInputs[key] = [inputList[key]];
            }
            return formattedInputs;
        };

        // Put all service input parallel parameters into the correct format ([{key: true}] => [key])
        this.formatParallelInputs = function (inputList) {
            var formattedInputs = [];
            for (var key in inputList) {
                if (inputList[key] === true) {
                    formattedInputs.push(key);
                }
            }
            return formattedInputs;
        };

        this.restoreServices = function () {
            halAPI.from(rootUri + '/contentAuthority/services/restoreDefaults/')
                .newRequest()
                .post()
                .result
                .then(
                    function (document) {
                        MessageService.addInfo('Restored Services', 'Successfully restored default Services.');
                    }, function (error) {
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
                    }, function (error) {
                        MessageService.addError('Failed to synchronized Services', error);
                    }
                );
        };

        this.updateBuildStatus = function (service) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/services/' + service.id + '/buildStatus')
                .newRequest()
                .getResource()
                .result
                .then(
                    function (data) {
                        service.buildStatus = data;
                        deferred.resolve(data);
                    }, function (error) {
                        MessageService.addError('Could not get build details for Service ' + service.name, error);
                        deferred.reject();
                    });
            return deferred.promise;
        };

        this.rebuildServiceContainer = function (service) {

            service.buildStatus = {
                needsBuild: false,
                status: 'REQUESTED',
                serviceFingerprint: null
            };

            var deferred = $q.defer();
            halAPI.from(rootUri + '/services/' + service.id + '/build')
                .newRequest()
                .post()
                .result
                .then(
                    function () {
                        self.updateBuildStatus(service);
                        deferred.resolve();
                    }, function (error) {
                        self.updateBuildStatus(service);
                        MessageService.addError('Could not start service container build' + service.name, error);
                        deferred.reject();
                    });
            return deferred.promise;
        };

        this.openBuildLogs = function (service) {
            var url = rootUri + '/services/' + service.id + '/buildLogs?fingerprint=' + service.buildStatus.serviceFingerprint;
            $window.open(url, '_blank');
        };

        return this;

    }]);
});
