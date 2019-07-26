/**
 * @ngdoc service
 * @name ftepApp.JobService
 * @description
 * # JobService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('SystematicService', [ 'ftepProperties', '$q', '$timeout', '$rootScope', 'MessageService', 'CommonService', 'UserService', 'CommunityService', 'traverson', function (ftepProperties, $q, $timeout, $rootScope, MessageService, CommonService, UserService, CommunityService, traverson) {

        var rootUri = ftepProperties.URLv2;

        var self = this;

        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();

        this.ownershipFilters = {
            ALL_PROCESSINGS: { id: 0, name: 'All', searchUrl: ''},
            MY_PROCESSINGS: { id: 1, name: 'Mine', searchUrl: 'search/findByOwner'},
            SHARED_PROCESSINGS: { id: 2, name: 'Shared', searchUrl: 'search/findByNotOwner'}
        };

        this.params = {
            community: {
                systematicProcessings: undefined,
                pollingUrl: rootUri + '/systematicProcessings/?sort=id,DESC',
                pollingRequestOptions: {},
                pagingData: {},
                selectedsystematicProcessing: undefined,
                searchText: '',
                displayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.ownershipFilters.MY_PROCESSINGS
            }
        }

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollSystematicProcessings = function (page) {
            pollingTimer = $timeout(function () {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.systematicprocs', document._embedded.systematicProcessings);
                        pollSystematicProcessings(page);
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll systematic processings', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollGroups(page);
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

        function getSystematicProcessings(page) {
            var deferred = $q.defer();
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                 .then(
                function (document) {
                    if(startPolling) {
                        pollSystematicProcessings(page);
                        startPolling = false;
                    }
                    self.params[page].pagingData._links = document._links;
                    self.params[page].pagingData.page = document.page;

                    deferred.resolve(document._embedded.systematicProcessings);
                }, function (error) {
                    MessageService.addError('Could not get systematic processings', error);
                    deferred.reject();
                });

            return deferred.promise;
        }

        this.refreshSystematicProcessings = function(page, action, job) {
            return self.getSystematicProcessingsByFilter(page);
        }

        this.getSystematicProcessingsByFilter = function(page) {
            if (self.params[page]) {
                var url = rootUri + '/systematicProcessings/' + self.params[page].selectedOwnershipFilter.searchUrl +
                    '?sort=name&filter=' + (self.params[page].searchText ? self.params[page].searchText : '');

                if (self.params[page].selectedOwnershipFilter !== self.ownershipFilters.ALL_PROCESSINGS) {
                    url += '&owner=' + UserService.params.activeUser._links.self.href;
                }
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getSystematicProcessings(page).then(function(data) {
                    self.params[page].systematicProcessings = data;
                });
            }
        };

        this.terminateSystematicProcessing = function(systematicProcessing) {
            var deferred = $q.defer();

            halAPI.from( rootUri + '/systematicProcessings/' + systematicProcessing.id + '/terminate')
            .newRequest()
            .post()
            .result
            .then(
                function (response) {
                    deferred.resolve();
                },
                function(error) {
                    MessageService.addError('Could not terminate systematic processing', error);
                    deferred.reject();
                }
            );

            return deferred.promise;
        }

        var getSystematicProcessing = function(systematicProcessing) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/systematicProcessings/' + systematicProcessing.id)
                .newRequest()
                .getResource()
                .result
                .then(
                    function(document) {
                        deferred.resolve(document);
                    }, function(error) {
                        MessageService.addError('Could not get systematic processing: ' + systematicProcessing.id, error);
                        deferred.reject();
                    });
            return deferred.promise;
        }
        
        this.getSystematicProcessingsPage = function(page, url) {
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                getSystematicProcessings(page).then(function(data) {
                    self.params[page].systematicProcessings = data;
                });
            }
        };

        this.refreshSelectedSystematicProcessing = function(page) {
            if (self.params[page]) {
                if (self.params[page].selectedSystematicProcessing) {
                    getSystematicProcessing(self.params[page].selectedSystematicProcessing).then(function(systematicProcessing) {
                        self.params[page].selectedSystematicProcessing = Object.assign({}, self.params[page].selectedSystematicProcessing, systematicProcessing);
                        if (page === 'community') {
                            CommunityService.getObjectGroups(self.params[page].selectedSystematicProcessing, 'systematicProcessing').then(function(data) {
                                self.params.community.sharedGroups = data;
                            });
                        }

                    });
                }
            }
        };

        this.estimateMonthlyCost =  function(service, systematicParam, inputParams, searchParams){

            var params = [];
            for (var key in searchParams) {
                params.push(key + '=' + searchParams[key]);
            }

            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/estimateCost/systematic?' + params.join('&'))
                .newRequest()
                .post({
                    service: {
                        id: service.id,
                        name: service.name,
                        owner: service.owner,
                        type: service.type,
                        _links: service._links,
                        access: service.access,
                        description: service.description,
                        dockerTag: service.dockerTag,
                        status: service.status,
                        licence: service.licence
                    },
                    inputs: inputParams,
                    systematicParameter: systematicParam
                })
                .result
                .then(function (document) {
                    resolve(JSON.parse(document.body));
                 }, function (error) {
                    if (error.httpStatus === 402) {
                        MessageService.addError('Balance exceeded', error);
                    } else {
                        MessageService.addError('Could not get cost estimation', error);
                    }
                    reject(JSON.parse(error.body));
                 });
            });
        };

        this.launchSystematicProcessing = function(service, systematicParam, inputParams, searchParams, label){

            var params = [];
            for (var key in searchParams) {
                params.push(key + '=' + searchParams[key]);
            }

            return $q(function(resolve, reject) {
                    halAPI.from(rootUri + '/jobConfigs/launchSystematic?' + params.join('&'))
                    .newRequest()
                    .post({
                        service: {
                            id: service.id,
                            name: service.name,
                            owner: service.owner,
                            type: service.type,
                            _links: service._links,
                            access: service.access,
                            description: service.description,
                            dockerTag: service.dockerTag,
                            status: service.status,
                            licence: service.licence
                        },
                        inputs: inputParams,
                        systematicParameter: systematicParam,
                        label: label
                    })
                    .result
                    .then(
                 function (document) {
                     //resolve(JSON.parse(document.body));
                 }, function (error) {
                     MessageService.addError('Could not create systematic job', error);
                     reject();
                 });
            });
        };

        return this;
    }]);
});
