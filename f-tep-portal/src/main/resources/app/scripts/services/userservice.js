/**
 * @ngdoc service
 * @name ftepApp.UserService
 * @description
 * # UserService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('UserService', [ 'ftepProperties', '$rootScope', '$q', 'MessageService', 'traverson', '$mdDialog', '$window', function (ftepProperties, $rootScope, $q, MessageService, traverson, $mdDialog, $window) {

        var _this = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();
        var timeout = false;

        this.params = {
            community: {
                pagingData: {},
                pollingUrl: rootUri + '/users/',
                allUsers: [],
                groupUsers: [],
                searchText: '',
                displayUserFilters: false
            },
            admin: {
                pagingData: {},
                pollingUrl: rootUri + '/users/',
                allUsers: [],
                selectedUser: undefined,
                userDetails: undefined,
                newRole: undefined,
                wallet: undefined,
                coins: 0,
                searchText: ''
            }
        };

        this.getCurrentUser = function(withDetails){
            var deferred = $q.defer();
            halAPI.from(ftepProperties.URLv2 + '/users/current')
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    $rootScope.$broadcast('no.user');
                    if (!timeout) {
                        $mdDialog.show({
                            controller: function ($scope, $window) {
                                $scope.reloadRoute = function() {
                                   $window.location.reload();
                                };
                                $scope.closeDialog = function() {
                                    $mdDialog.hide();
                                };
                            },
                            templateUrl: 'views/common/templates/timeout.tmpl.html',
                            parent: angular.element(document.body),
                            clickOutsideToClose: true
                        });

                        MessageService.addError('No User Detected', error);
                        timeout = true;
                    }
                    deferred.reject();
                });
            return deferred.promise;
        };

        this.getAllUsers = function (page, url) {
            var deferred = $q.defer();
            url = url ? url = _this.params[page].pollingUrl + url : url = _this.params[page].pollingUrl;
            halAPI.from(url)
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                if( _this.params[page] &&  _this.params[page].pagingData) {
                    _this.params[page].pagingData._links = document._links;
                    _this.params[page].pagingData.page = document.page;
                }
                deferred.resolve(document._embedded.users);
            }, function (error) {
                MessageService.addError('Could not get Users', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.getUsersByFilter = function(page){
            _this.params[page].pollingUrl = rootUri + '/users/' + 'search/byFilter?sort=name&filter=' + (_this.params[page].searchText ? _this.params[page].searchText : '');
            _this.getAllUsers(page).then(function (users) {
                    _this.params[page].allUsers = users;
            });
        };

        this.getUsers = function (group) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/groups/' + group.id)
                     .newRequest()
                     .follow('members')
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document._embedded.users);
            }, function (error) {
                MessageService.addError('Could not get Users', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        /* Fetch a new page */
        this.getUsersPage = function(page, url){
            if (_this.params[page]) {
                _this.params[page].pollingUrl = url;
                /* Get user list */
                _this.getAllUsers(page).then(function (users) {
                    _this.params[page].allUsers = users;
                });
            }
        };

        this.getUserByLink = function(userUrl){
            var deferred = $q.defer();
            halAPI.from(userUrl)
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError('Could not get User', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.addUser = function (group, groupUsers, user) {
            return $q(function(resolve, reject) {

                /* Create array of user links */
                var membersArray = [];

                for (var item in groupUsers) {
                    membersArray.push(groupUsers[item]._links.self.href);
                }

                /* Append user to new members array */
                membersArray.push(user._links.self.href);

                /* Set new members object */
                var updatedMembers = {"members": membersArray};

                /* Patch members with updated member list */
                halAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .patch(updatedMembers)
                         .result
                         .then(
                function (document) {
                   if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('User successfully added', 'User added to ' + group.name);
                        resolve(document);
                    } else {
                        MessageService.addError('Could not add User to ' + group.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not add User to ' + group.name, error);
                    reject();
                });

            });
        };

        this.updateUser = function(user){
            var newUser = {name: user.name, id: user.id, email: user.email, role: user.role};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/users/' + user.id)
                         .newRequest()
                         .patch(newUser)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('User successfully updated', 'User ' + user.name + ' successfully updated.');
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Could not update User ' + user.name, error);
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
                halAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .patch(updatedMembers)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('User deleted', 'User ' + user.name + ' deleted.');
                        resolve(group);
                    } else {
                        MessageService.addError('Could not remove User ' + user.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not remove User ' + user.name, error);
                    reject();
                });

            });
        };

        return this;
    }]);
});
