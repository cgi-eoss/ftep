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
                ALL_BASKETS: { id: 0, name: 'All', criteria: ''},
                MY_BASKETS: { id: 1, name: 'Mine', criteria: undefined },
                SHARED_BASKETS: { id: 2, name: 'Shared', criteria: undefined }
        };

        UserService.getCurrentUser().then(function(currentUser){
            self.dbOwnershipFilters.MY_BASKETS.criteria = { owner: {name: currentUser.name } };
            self.dbOwnershipFilters.SHARED_BASKETS.criteria = { owner: {name: "!".concat(currentUser.name) } };
        });

        this.params = {
            explorer: {
                databaskets: undefined,
                items: undefined,
                selectedDatabasket: undefined,
                selectedItems: undefined,
                searchText: '',
                displayFilters: false,
                showBaskets: true,
                databasketOnMap: {id: undefined},
                selectedOwnerhipFilter: self.dbOwnershipFilters.ALL_BASKETS
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
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.dbOwnershipFilters.ALL_BASKETS,
                showBaskets: true
            }
        };

        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;

        var pollDatabaskets = function () {
            $timeout(function () {
                halAPI.from(rootUri + '/databaskets/?size=100') //TODO implement paging
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        $rootScope.$broadcast('poll.baskets', document._embedded.databaskets);
                        pollDatabaskets();
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Databaskets', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollDatabaskets();
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        var getDatabaskets = function () {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/databaskets/?size=100') //TODO implement paging
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    if (startPolling) {
                        pollDatabaskets();
                        startPolling = false;
                    }
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
                    MessageService.addInfo('Databasket created', 'New databasket '.concat(name).concat(' created.'));
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError ('Failed to create Databasket', error);
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
                        MessageService.addInfo('Databasket deleted', 'Databasket '.concat(databasket.name).concat(' deleted.'));
                        resolve(databasket);
                    } else {
                        MessageService.addError ('Failed to remove Databasket', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to remove Databasket', error);
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
                    MessageService.addInfo('Databasket successfully updated', 'Databasket ' + databasket.name + ' successfully updated.');
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Failed to update Databasket', error);
                    reject();
                });
            });
        };

        var getDatabasket = function(databasket) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/databaskets/' + databasket.id)
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError ('Could not get Databasket', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.getDatabasketContents = function(databasket) {
            var deferred = $q.defer();
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

        this.refreshDatabaskets = function (page, action, basket) {

            if (self.params[page]) {
                /* Get databasket list */
                getDatabaskets().then(function (data) {

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
                if(databasket._embedded){
                    for (var item in databasket._embedded.files) {
                        itemsArray.push(databasket._embedded.files[item]._links.self.href);
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
                        MessageService.addInfo('Files successfully added');
                        resolve(document);
                    } else {
                        MessageService.addError ('Failed to add file/s to Databasket', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Failed to add file/s to Databasket', error);
                    reject();
                });

            });
        };

        this.removeItem = function(databasket, files, file) {
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
                        MessageService.addInfo('File removed from Databasket', 'File '.concat(file.filename).concat('removed from ').concat(databasket.name));
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
                        MessageService.addInfo('Databasket successfully cleared');
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

        return this;
    }]);
});
