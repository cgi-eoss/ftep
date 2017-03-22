/**
 * @ngdoc service
 * @name ftepApp.UserService
 * @description
 * # UserService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('UserService', [ 'ftepProperties', '$q', 'MessageService', 'traverson', function (ftepProperties, $q, MessageService, traverson) {

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var usersAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.params = {
            allUsers: [],
            groupUsers: [],
            searchText: '',
            displayUserFilters: false
        };

        this.getCurrentUser = function(){
            var deferred = $q.defer();
            usersAPI.from(ftepProperties.URLv2 + '/currentUser')
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError ('Could not get user data',
                            'Failed to get user data' + ((error.doc && error.doc.message) ? ': ' + error.doc.message : '' ));
                    deferred.reject();
                });
            return deferred.promise;
        };

        this.getAllUsers = function () {
            var deferred = $q.defer();
            usersAPI.from(rootUri + '/users/')
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document._embedded.users);
            }, function (error) {
                MessageService.addError ('Could not get Users', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.getUsers = function (group) {
            var deferred = $q.defer();
            usersAPI.from(rootUri + '/groups/' + group.id)
                     .newRequest()
                     .follow('members')
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document._embedded.users);
            }, function (error) {
                MessageService.addError ('Could not get Users', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.addUser = function (group, user) {
            return $q(function(resolve, reject) {

                /* Create array of user links */
                var membersArray = [];
                for (var item in group.users) {
                    membersArray.push(group.users[item]._links.self.href);
                }

                /* Append user to new members array */
                membersArray.push(user._links.self.href);

                /* Set new members object */
                var updatedMembers = {"members": membersArray};

                /* Patch members with updated member list */
                usersAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .patch(updatedMembers)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('User successfully added');
                        resolve(document);
                    } else {
                        MessageService.addError ('Failed to add User', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Failed to add user to Group', error);
                    reject();
                });

            });
        };

        this.removeUser = function(group, users, user) {
            return $q(function(resolve, reject) {

                /* Create array of user links */
                var membersArray = [];
                for (var item in users) {
                    /* Don't add user to updated list */
                    if (users[item].id !== user.id) {
                        membersArray.push(users[item]._links.self.href);
                    }
                }

                /* Set new members object */
                var updatedMembers = {"members": membersArray};

                /* Patch members with updated member list */
                usersAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .patch(updatedMembers)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('User deleted', 'User '.concat(user.name).concat(' deleted.'));
                        resolve(group);
                    } else {
                        MessageService.addError ('Failed to Remove User', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to Remove User', error);
                    reject();
                });

            });
        };

        return this;
    }]);
});
