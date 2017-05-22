/**
 * @ngdoc service
 * @name ftepApp.BasketService
 * @description
 * # BasketService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('BasketService', [ '$rootScope', '$http', 'ftepProperties', '$q', '$timeout', 'MessageService', 'UserService', 'TabService', 'CommunityService', 'traverson', function ($rootScope, $http, ftepProperties, $q, $timeout, MessageService, UserService, TabService, CommunityService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        /** PRESERVE USER SELECTIONS **/
        this.dbOwnershipFilters = {
                ALL_BASKETS: { id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'},
                MY_BASKETS: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner' },
                SHARED_BASKETS: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner' }
        };

        var userUrl;
        UserService.getCurrentUser().then(function(currentUser){
            userUrl = currentUser._links.self.href;
        });

        this.params = {
            explorer: {
                pollingUrl: rootUri + '/databaskets/?sort=name',
                pagingData: {},
                databaskets: undefined,
                items: undefined,
                selectBasketList: undefined,
                selectedDatabasket: undefined,
                selectedItems: undefined,
                searchText: '',
                displayFilters: false,
                databasketOnMap: {id: undefined},
                selectedOwnershipFilter: self.dbOwnershipFilters.ALL_BASKETS
            },
            community: {
                pollingUrl: rootUri + '/databaskets/?sort=name',
                pagingData: {},
                databaskets: undefined,
                items: undefined,
                selectedDatabasket: undefined,
                selectedItems: undefined,
                searchText: '',
                displayFilters: false,
                itemSearchText: '',
                itemDisplayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.dbOwnershipFilters.ALL_BASKETS
            }
        };

        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollDatabaskets = function (page) {
            pollingTimer = $timeout(function () {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.baskets', document._embedded.databaskets);
                        pollDatabaskets(page);
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Databaskets', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollDatabaskets(page);
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

        var getDatabaskets = function (page) {
            var deferred = $q.defer();
            halAPI.from(self.params[page].pollingUrl)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    if (startPolling) {
                        pollDatabaskets(page);
                        startPolling = false;
                    }
                    self.params[page].pagingData._links = document._links;
                    self.params[page].pagingData.page = document.page;

                    deferred.resolve(document._embedded.databaskets);
                }, function (error) {
                    MessageService.addError('Could not get Databaskets', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.createDatabasket = function (name, desc) {
            return $q(function(resolve, reject) {
                var databasket = {name: name, description: (desc ? desc : '')};
                halAPI.from(rootUri + '/databaskets/')
                         .newRequest()
                         .post(databasket)
                         .result
                         .then(
                function (document) {
                     MessageService.addInfo('Databasket created', 'New databasket ' + name + ' created.');
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Could not create Databasket ' + name, error);
                    reject();
                });
            });
        };

        this.removeDatabasket = function(databasket) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/databaskets/' + databasket.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Databasket deleted', 'Databasket ' + databasket.name + ' deleted.');
                        resolve(databasket);
                    } else {
                         MessageService.addError('Could not remove Databasket ' + databasket.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not remove Databasket ' + databasket.name, error);
                    reject();
                });
            });
        };

        this.updateDatabasket = function (databasket) {
            var newdatabasket = {name: databasket.name, description: databasket.description};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/databaskets/' + databasket.id)
                         .newRequest()
                         .patch(newdatabasket)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Databasket successfully updated', 'Databasket ' + databasket.name + ' updated.');
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Could not update Databasket ' + databasket.name, error);
                    reject();
                });
            });
        };

        var getDatabasket = function(databasket) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/databaskets/' + databasket.id + '?projection=detailedDatabasket')
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError('Could not get Databasket' + databasket.name, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.getDatabasketContents = function(databasket) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/databaskets/' + databasket.id)
                     .newRequest()
                     .follow('files')
                     .withRequestOptions({
                          qs: { projection: 'detailedFtepFile' }
                      })
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document._embedded.ftepFiles);
            }, function (error) {
                MessageService.addError('Could not get contents of Databasket ' + databasket.name, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.refreshDatabaskets = function (page, action, basket) {

            if (self.params[page]) {
                /* Get databasket list */
                getDatabaskets(page).then(function (data) {

                    self.params[page].databaskets = data;

                    /* Select last databasket if created */
                    if (action === "Create") {
                        self.params[page].selectedDatabasket = self.params[page].databaskets[self.params[page].databaskets.length-1];
                    }

                    /* Clear basket if deleted */
                    if (action === "Remove") {
                        if (basket && self.params[page].selectedDatabasket && basket.id === self.params[page].selectedDatabasket.id) {
                            self.params[page].selectedDatabasket = undefined;
                            self.params[page].items = [];
                        }
                    }

                    /* Update the selected basket */
                    self.refreshSelectedBasket(page);
                });
            }
        };

        /* Fetch a new page */
        this.getDatabasketsPage = function(page, url){
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getDatabaskets(page).then(function (data) {
                    self.params[page].databaskets = data;
                });
            }
        };

        this.getDatabasketsByFilter = function (page) {
            if (self.params[page]) {
                var url = rootUri + '/databaskets/' + self.params[page].selectedOwnershipFilter.searchUrl
                        + '?sort=name&filter=' + (self.params[page].searchText ? self.params[page].searchText : '');

                if(self.params[page].selectedOwnershipFilter !== self.dbOwnershipFilters.ALL_BASKETS){
                    url += '&owner=' + userUrl;
                }
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getDatabaskets(page).then(function (data) {
                    self.params[page].databaskets = data;
                });
            }
        };

        // search function for explorer page bottombar menu-bars
        this.searchBaskets = function (searchText) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/databaskets/' + 'search/findByFilterOnly?sort=name&filter=' + searchText)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document._embedded.databaskets);
                }, function (error) {
                    MessageService.addError('Could not get Databaskets', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.refreshSelectedBasket = function (page) {

            if (self.params[page]) {
                /* Get basket contents if selected */
                if (self.params[page].selectedDatabasket) {

                    getDatabasket(self.params[page].selectedDatabasket).then(function (basket) {
                        self.params[page].selectedDatabasket = basket;
                        self.getDatabasketContents(basket).then(function (data) {
                            self.params[page].items = data;
                        });

                        if(page === 'community'){
                            CommunityService.getObjectGroups(basket, 'databasket').then(function (data) {
                                self.params.community.sharedGroups = data;
                            });
                        }
                    });
                }
            }
        };

        this.addItems = function (databasket, fileLinks) {
            return $q(function(resolve, reject) {

                var itemsArray = [];

                /* Collect links from current items */
                if(databasket.files){
                    for (var item in databasket.files) {
                        itemsArray.push(databasket.files[item]._links.self.href);
                    }
                }

                /* Append links of new items */
                for (var file in fileLinks) {
                    if(itemsArray.indexOf(fileLinks[file]) < 0){
                        itemsArray.push(fileLinks[file]);
                    }
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
                        MessageService.addInfo('Files successfully added', 'Files added to ' + databasket.name);
                        resolve(document);
                    } else {
                        MessageService.addError('Could not add file/s to Databasket ' + databasket.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not add file/s to Databasket ' + databasket.name, error);
                    reject();
                });

            });
        };

        this.removeDatabasketItem = function(databasket, files, file) {
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
                        MessageService.addInfo('File removed from Databasket', 'File ' + file.filename + ' removed from ' + databasket.name);
                        resolve(databasket);
                    } else {
                        MessageService.addError('Could not remove item from Databasket ' + databasket.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not remove item from Databasket ' + databasket.name, error);
                    reject();
                });
            });
        };

        this.clearDatabasket = function(databasket) {
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
                        MessageService.addInfo('Databasket successfully cleared', 'Databasket ' + databasket.name + ' has been cleared');
                        resolve(databasket);
                    } else {
                        MessageService.addError('Could not clear Databasket ' + databasket.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not clear Databasket ' + databasket.name, error);
                    reject();
                });
            });
        };

        return this;
    }]);
});
