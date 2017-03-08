/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityGroupsCtrl
 * @description
 * # CommunityGroupsCtrl
 * Controller of the ftepApp
 */

define(['../../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('CommunityGroupsCtrl', ['GroupService', 'MessageService', '$rootScope', '$scope', '$mdDialog', '$sce', function (GroupService, MessageService, $rootScope, $scope, $mdDialog, $sce) {

        /* Get stored Groups details */
        $scope.groupParams = GroupService.params;

        /* Get Groups */
         GroupService.getGroups().then(function (data) {
            $scope.groupParams.groups = data;
        });

        /* Filters */
        $scope.toggleGroupFilters = function () {
            $scope.groupParams.displayGroupFilters = !$scope.groupParams.displayGroupFilters;
            $scope.$broadcast('rebuild:scrollbar');
        };

        $scope.groupSearch = {
            searchText: $scope.groupParams.searchText
        };

        $scope.groupQuickSearch = function (group) {
            if (group.name.toLowerCase().indexOf(
                $scope.groupSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Group Description Popup */
        var popover = {};
        $scope.getGroupDescription = function (group) {
            if (!group.description) {
                group.description = "No description.";
            }
            var html =
                '<div class="metadata">' +
                    '<div class="row">' +
                        '<div class="col-sm-12">' + group.description + '</div>' +
                    '</div>' +
                '</div>';
            return popover[html] || (popover[html] = $sce.trustAsHtml(html));
        };

        /* Create Group */
        $scope.createGroupDialog = function ($event) {
            function DialogController($scope, $mdDialog, GroupService) {

                /* Get Groups and User details */
                $scope.groupParams = GroupService.params;

                $scope.addGroup = function () {
                    GroupService.createGroup($scope.newGroup.name, $scope.newGroup.description).then(function (createdGroup) {
                        /* Update the group list */
                         GroupService.getGroups().then(function (groups) {
                            $scope.groupParams.groups = groups;
                            /* Set created group to the active group */
                            $scope.groupParams.selectedGroup = groups[groups.length-1];
                        });
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };

            }
            $mdDialog.show({
                controller: DialogController,
                templateUrl: 'views/community/manage/groups/templates/creategroup.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        /* Remove Group */
        $scope.removeGroup = function (key, group) {
            GroupService.removeGroup(group).then(function (data) {
                $scope.groupParams.groups.splice(key, 1);
                $scope.userParams.groupUsers = [];
                /* If group removed is active clear it */
                if (group === $scope.groupParams.selectedGroup) {
                    $scope.groupParams.selectedGroup = undefined;
                }
            });
        };

        /* Edit Group */
        $scope.editGroupDialog = function ($event, selectedGroup) {
            function DialogController($scope, $mdDialog, GroupService) {

                /* Get Groups and User details */
                $scope.groupParams = GroupService.params;

                /* Save temporary changes */
                $scope.tempGroup = angular.copy(selectedGroup);

                /* Patch group and update group list */
                $scope.updateGroup = function () {
                    GroupService.updateGroup($scope.tempGroup).then(function (data) {
                        GroupService.getGroups().then(function (data) {
                            $scope.groupParams.groups = data;
                            /* If the modified group is currently selected then update it */
                            if ($scope.groupParams.selectedGroup.id === $scope.tempGroup.id) {
                                $scope.groupParams.selectedGroup = $scope.tempGroup;
                            }
                        });
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };

            }
            $mdDialog.show({
                controller: DialogController,
                templateUrl: 'views/community/manage/groups/templates/editgroup.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

    }]);
});
