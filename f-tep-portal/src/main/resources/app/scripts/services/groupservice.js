/**
 * @ngdoc service
 * @name ftepApp.GroupService
 * @description
 * # GroupService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('GroupService', [ 'ftepProperties', '$q', 'UserService', 'MessageService', 'TabService',  'CommunityService', 'traverson', '$timeout', '$rootScope', function (ftepProperties, $q, UserService, MessageService, TabService, CommunityService, traverson, $timeout, $rootScope) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.groupOwnershipFilters = {
            ALL_GROUPS: {id: 0, name: 'All',  searchUrl: 'search/findByFilterOnly'},
            MY_GROUPS: {id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner' },
            SHARED_GROUPS: {id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner' }
        };

        var userUrl;
        UserService.getCurrentUser().then(function(currentUser){
            userUrl = currentUser._links.self.href;
        });

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            community: {
                groups: [],
                pollingUrl: rootUri + '/groups/?sort=name',
                pagingData: {},
                selectedGroup: undefined,
                searchText: '',
                displayGroupFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.groupOwnershipFilters.ALL_GROUPS,
            }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollGroups = function (page) {
            pollingTimer = $timeout(function () {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.groups', document._embedded.groups);
                        pollGroups(page);
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Groups', error);
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

        function getGroups(page) {
            var deferred = $q.defer();
            halAPI.from(self.params[page].pollingUrl)
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                if(startPolling) {
                    pollGroups(page);
                    startPolling = false;
                }
                self.params[page].pagingData._links = document._links;
                self.params[page].pagingData.page = document.page;

                deferred.resolve(document._embedded.groups);
            }, function (error) {
                MessageService.addError ('Could not get Groups', error);
                deferred.reject();
            });
            return deferred.promise;
        }

        /* Get Groups for share functionality to fill the combobox */
        this.getGroups = function(){
            var deferred = $q.defer();
            halAPI.from(rootUri + '/groups/?sort=name&size=100')
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document._embedded.groups);
            }, function (error) {
                MessageService.addError('Could not get Groups', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        /* Fetch a new page */
        this.getGroupsPage = function(page, url){
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get groups list */
                getGroups(page).then(function (data) {
                    self.params[page].groups = data;
                });
            }
        };

        this.getGroupsByFilter = function (page) {
            if (self.params[page]) {
                var url = rootUri + '/groups/' + self.params[page].selectedOwnershipFilter.searchUrl +
                    '?sort=name&filter=' + (self.params[page].searchText ? self.params[page].searchText : '');

                if(self.params[page].selectedOwnershipFilter !== self.groupOwnershipFilters.ALL_GROUPS){
                    url += '&owner=' + userUrl;
                }
                self.params[page].pollingUrl = url;

                /* Get groups list */
                getGroups(page).then(function (data) {
                    self.params[page].groups = data;
                });
            }
        };

        this.createGroup = function(name, desc) {
            return $q(function(resolve, reject) {
                var group = {name: name, description: (desc ? desc : '')};
                halAPI.from(rootUri + '/groups/')
                         .newRequest()
                         .post(group)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Group created', 'New group ' + name + ' created.');
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Could not create Group ' + name, error);
                    reject();
                });
            });
        };

        this.removeGroup = function(group) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Group deleted', 'Group ' + group.name + ' deleted.');
                        resolve(group);
                    } else {
                        MessageService.addError('Could not remove Group ' + group.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not remove Group ' + group.name, error);
                    reject();
                });
            });
        };

        this.updateGroup = function (group) {
            var newgroup = {name: group.name, description: group.description};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .patch(newgroup)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Group successfully updated', 'Group ' + group.name + ' successfully updated.');
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Could not update Group ' + group.name, error);
                    reject();
                });
            });
        };

        var getGroup = function (group) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/groups/' + group.id + "?projection=detailedGroup")
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError('Could not get Group ' + group.name, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.refreshGroups = function (page, action, group) {

            if (self.params[page]) {

                /* Get group list */
                getGroups(page).then(function (data) {

                    self.params[page].groups = data;

                    /* Select last group if created */
                    if (action === "Create") {
                        self.params[page].selectedGroup = self.params[page].groups[self.params[page].groups.length-1];
                    }

                    /* Clear group if deleted */
                    if (action === "Remove") {
                        if (group && group.id === self.params[page].selectedGroup.id) {
                            self.params[page].selectedGroup = undefined;
                            self.params[page].groupUsers = [];
                        }
                    }

                    /* Update the selected group */
                    self.refreshSelectedGroup(page);
                });
            }

        };

        this.refreshSelectedGroup = function (page) {

            if (self.params[page]) {
                /* Get group contents if selected */
                if (self.params[page].selectedGroup) {
                    getGroup(self.params[page].selectedGroup).then(function (group) {
                        self.params[page].selectedGroup = group;
                        UserService.getUsers(group).then(function (users) {
                            UserService.params[page].groupUsers = users;
                        });
                        if(group.access.currentLevel === 'ADMIN') {
                            CommunityService.getObjectGroups(group, 'group').then(function (data) {
                                self.params[page].sharedGroups = data;
                            });
                        }
                    });
                }
            }

        };

    return this;
  }]);
});
