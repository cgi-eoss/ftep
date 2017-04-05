/**
 * Created by dashi on 03-08-2016.
 */

  'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('GroupService', [ 'ftepProperties', '$q', 'UserService', 'MessageService', 'TabService', 'traverson', '$timeout', '$rootScope', function (ftepProperties, $q, UserService, MessageService, TabService, traverson, $timeout, $rootScope) {

        var that = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var groupsAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            community: {
                groups: [],
                selectedGroup: undefined,
                searchText: '',
                displayGroupFilters: false
            }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;

        var pollGroupsV2 = function () {
            $timeout(function () {
                groupsAPI.from(rootUri + '/groups/')
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        $rootScope.$broadcast('poll.groups', document._embedded.groups);
                        if(TabService.startPolling().groups) {
                            pollGroupsV2();
                        }
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Groups', error);
                        pollCount -= 1;
                        if (pollCount >= 0 && TabService.startPolling().groups) {
                            pollGroupsV2();
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.getGroups = function () {
            var deferred = $q.defer();
            groupsAPI.from(rootUri + '/groups/')
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                if(TabService.startPolling().baskets) {
                    pollGroupsV2();
                }
                deferred.resolve(document._embedded.groups);
            }, function (error) {
                MessageService.addError ('Could not get Groups', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.createGroup = function(name, desc) {
            return $q(function(resolve, reject) {
                var group = {name: name, description: (desc ? desc : '')};
                groupsAPI.from(rootUri + '/groups/')
                         .newRequest()
                         .post(group)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Group created', 'New group '.concat(name).concat(' created.'));
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError ('Failed to Create Group', error);
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
                        MessageService.addInfo('Group deleted', 'Group '.concat(group.name).concat(' deleted.'));
                        resolve(group);
                    } else {
                        MessageService.addError ('Failed to Remove Group', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to Remove Group', error);
                    reject();
                });
            });
        };

        this.updateGroup = function (group) {
            var newgroup = {name: group.name, description: group.description};
            return $q(function(resolve, reject) {
                groupsAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .patch(newgroup)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Group successfully updated', 'Group ' + group.name + ' successfully updated.' );
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Failed to update Group', error);
                    reject();
                });
            });
        };

        var getGroup = function (group) {
            var deferred = $q.defer();
            groupsAPI.from(rootUri + '/groups/' + group.id)
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError ('Could not get Group', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.refreshGroupsV2 = function (service, action, group) {

            if (service === "Community") {

                /* Get group list */
                this.getGroups().then(function (data) {

                    that.params.community.groups = data;

                    /* Select last group if created */
                    if (action === "Create") {
                        that.params.community.selectedGroup = that.params.community.groups[that.params.community.groups.length-1];
                    }

                    /* Clear group if deleted */
                    if (action === "Remove") {
                        if (group && group.id === that.params.community.selectedGroup.id) {
                            that.params.community.selectedGroup = undefined;
                            that.params.community.groupUsers = [];
                        }
                    }

                    /* Update the selected group */
                    that.refreshSelectedGroupV2("Community");

                });

            }

        };

        this.refreshSelectedGroupV2 = function (service) {

            if (service === "Community") {
                /* Get group contents if selected */
                if (that.params.community.selectedGroup) {
                    getGroup(that.params.community.selectedGroup).then(function (group) {
                        that.params.community.selectedGroup = group;
                        UserService.getUsers(group).then(function (users) {
                            UserService.params.community.groupUsers = users;
                        });
                    });
                }
            }

        };

    return this;
  }]);
});
