/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityGroupsCtrl
 * @description
 * # CommunityGroupsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityUsersCtrl', ['GroupService', 'UserService', 'MessageService', '$rootScope', '$scope', '$mdDialog', '$sce', function (GroupService, UserService, MessageService, $rootScope, $scope, $mdDialog, $sce) {

        /* Get stored Groups and User details */
        $scope.groupParams = GroupService.params;
        $scope.userParams = UserService.params;

        /* Filters */
        $scope.toggleUserFilters = function () {
            $scope.userParams.displayUserFilters = !$scope.userParams.displayUserFilters;
            $scope.$broadcast('rebuild:scrollbar');
        };

        $scope.userSearch = {
            searchText: $scope.userParams.searchText
        };

        $scope.userQuickSearch = function (user) {
            if (user.name.toLowerCase().indexOf(
                $scope.userSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Add user to group */
        $scope.addUsersDialog = function($event) {
            function EditController($scope, $mdDialog, GroupService) {

                $scope.groupParams = GroupService.params;
                $scope.userParams = UserService.params;
                $scope.searchText = "";

                UserService.getAllUsers().then(function (allUsers) {
                    $scope.userParams.allUsers = allUsers;
                });

                UserService.getUsers($scope.groupParams.selectedGroup).then(function (groupUsers) {
                    $scope.userParams.groupUsers = groupUsers;
                });

                $scope.searchUsers = function(query) {
                    var filteredItems = [];
                    var queryLower = query.toLowerCase();
                    filteredItems = $scope.userParams.allUsers.filter(function(searchText) {
                        return (searchText.name.toLowerCase()).indexOf(queryLower) > -1;
                    });
                    return filteredItems;
                };

                $scope.validateUser = function (user) {
                    if (user) {
                        for (var groupUser in $scope.userParams.groupUsers) {
                            if (user.id === $scope.userParams.groupUsers[groupUser].id) {
                                return false;
                            }
                        }
                        return true;
                    }
                    return false;
                };

                $scope.addUser = function(group, user) {
                    /* Set success message to hidden */
                    $scope.addUserSuccess = false;
                    /* Check user doesn't belong to group already */
                    if ($scope.validateUser(user)) {
                        UserService.addUser(group, user).then(function (data) {
                            /* Display success message and clear form */
                            $scope.addUserSuccess = true;
                            $scope.searchText = null;
                            /* Update groups list */
                            $scope.groupParams.selectedGroup.size += 1;
                            /* Update user list */
                            $scope.userParams.groupUsers.push(user);
                        });
                    }
                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            $mdDialog.show({
                controller: EditController,
                templateUrl: 'views/community/manage/groups/templates/adduser.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
           });
        };

        /* Remove User */
        $scope.removeUser = function (key, user) {
             UserService.removeUser($scope.groupParams.selectedGroup, $scope.userParams.groupUsers, user).then(function (data) {
                /* Update groups list */
                $scope.groupParams.selectedGroup.size -= 1;
                /* Update user list */
                $scope.userParams.groupUsers.splice(key, 1);
            });
        };

    }]);
});
