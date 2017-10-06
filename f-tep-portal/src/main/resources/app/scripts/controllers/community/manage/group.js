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

                UserService.getUsers($scope.groupParams.selectedGroup).then(function (groupUsers) {
                    $scope.userParams.groupUsers = groupUsers;
                });

                $scope.searchUsers = function() {
                    return UserService.getAllUsers('community',  'search/byFilter?sort=name&filter=' + $scope.userParams.searchText).then(function(users){
                        return users;
                    });
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
                            $scope.userParams.selectedUser = null;
                            /* Update groups & active group */
                            GroupService.refreshGroups("community");
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
