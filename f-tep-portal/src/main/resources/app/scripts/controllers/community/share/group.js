/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareGroupCtrl
 * @description
 * # CommunityShareGroupCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareGroupCtrl', ['GroupService', 'UserService', '$scope', '$mdDialog', function (GroupService, UserService, $scope, $mdDialog) {

        /* Get stored Groups details */
        $scope.groupParams = GroupService.params.community;
        $scope.userParams = UserService.params.community;

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
            function AddUserController($scope, $mdDialog, GroupService) {

                $scope.groupParams = GroupService.params.community;
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
                    /* Ensure user list is up to date */
                    $scope.userParams.groupUsers = UserService.params.community.groupUsers;
                    /* Set success message to hidden */
                    $scope.addUserSuccess = false;
                    /* Check user doesn't belong to group already */
                    if ($scope.validateUser(user)) {
                        UserService.addUser(group, $scope.userParams.groupUsers, user).then(function (data) {
                            /* Display success message and clear form */
                            $scope.addUserSuccess = true;
                            $scope.searchText = null;
                            /* Update groups & active group */
                            GroupService.refreshGroupsV2("Community");
                        });
                    }
                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            AddUserController.$inject = ['$scope', '$mdDialog', 'GroupService'];
            $mdDialog.show({
                controller: AddUserController,
                templateUrl: 'views/community/manage/groups/templates/adduser.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
           });
        };

        /* Remove User */
        $scope.removeUser = function (group, users, user) {
             UserService.removeUser(group, users, user).then(function (data) {
                GroupService.refreshGroupsV2("Community");
            });
        };

    }]);
});
