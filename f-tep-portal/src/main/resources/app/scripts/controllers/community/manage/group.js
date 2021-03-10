/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageGroupCtrl
 * @description
 * # CommunityManageGroupCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageGroupCtrl', ['CommunityService', 'GroupService', 'UserService', '$scope', '$mdDialog', function (CommunityService, GroupService, UserService, $scope, $mdDialog) {

        /* Get stored Groups details */
        $scope.groupParams = GroupService.params.community;
        $scope.userParams = UserService.params.community;
        $scope.permissions = CommunityService.permissionTypes;

         /* Filters */
        $scope.toggleUserFilters = function () {
            $scope.userParams.displayUserFilters = !$scope.userParams.displayUserFilters;
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

        $scope.toggleShareFilters = function () {
            $scope.groupParams.sharedGroupsDisplayFilters = !$scope.groupParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.groupParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshGroup = function() {
            GroupService.refreshSelectedGroup('community');
        };

         /* Add user to group */
        $scope.addUsersDialog = function($event) {
            function AddUserController($scope, $mdDialog, GroupService) {

                $scope.groupParams = GroupService.params.community;
                $scope.userParams = UserService.params.community;

                $scope.userParams.searchText = "";
                $scope.errorMessage = "";

                UserService.getUsers($scope.groupParams.selectedGroup).then(function (groupUsers) {
                    $scope.userParams.groupUsers = groupUsers;
                });

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

                $scope.addUser = function() {
                    // Ensure user list is up to date
                    $scope.userParams.groupUsers = UserService.params.community.groupUsers;
                    // Set success and error message to hidden
                    $scope.addUserSuccess = false;
                    $scope.addUserFailure = false;
                    // Try to retrieve the user
                    UserService.getAllUsers('community',  'search/byFilterExact?sort=name&filter=' + $scope.userParams.searchText).then(function(users){
                        switch (users.length) {
                            case 1:
                                $scope.userParams.selectedUser = users[0];

                                // Ensure the selected user is not already in the group
                                if (!$scope.validateUser($scope.userParams.selectedUser)) {
                                    $scope.displayFailure("The user " + $scope.userParams.selectedUser.name + " is already in " + $scope.groupParams.selectedGroup.name + ".");
                                }
                                else {
                                    UserService.addUser($scope.groupParams.selectedGroup, $scope.userParams.groupUsers, $scope.userParams.selectedUser).then(function (data) {
                                        // Display success message and clear form
                                        $scope.addUserSuccess = true;
                                        $scope.userParams.selectedUser = null;
                                        // Update groups & active group
                                        GroupService.refreshGroups("community");
                                    });
                                }
                                break;
                            default:
                                $scope.displayFailure("No user found matching the given identifier, please try again.");
                                break;
                        }
                    });
                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                }

                $scope.displayFailure = function(message) {
                    $scope.addUserFailure = true;
                    $scope.errorMessage = message;
                };
            }
            AddUserController.$inject = ['$scope', '$mdDialog', 'GroupService'];
            $mdDialog.show({
                controller: AddUserController,
                templateUrl: 'views/community/templates/adduser.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
           });
        };

        /* Remove User */
        $scope.removeUser = function (group, users, user) {
             UserService.removeUser(group, users, user).then(function (data) {
                GroupService.refreshGroups("community");
            });
        };

    }]);
});
