/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityGroupsCtrl
 * @description
 * # CommunityGroupsCtrl
 * Controller of the ftepApp
 */

define(['../../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('CommunityGroupsCtrl', ['GroupService', 'CommonService', 'MessageService', '$rootScope', '$scope', '$mdDialog', '$sce',
                                                   function (GroupService, CommonService, MessageService, $rootScope, $scope, $mdDialog, $sce) {

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
            CommonService.createItemDialog($event, 'GroupService', 'createGroup').then(function (newGroup) {
                /* Update the group list */
                GroupService.getGroups().then(function (groups) {
                    $scope.groupParams.groups = groups;
                    /* Set created group to the active group */
                    $scope.groupParams.selectedGroup = groups[groups.length-1];
                });
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
        $scope.editGroupDialog = function($event, selectedGroup) {
            CommonService.editItemDialog($event, selectedGroup, 'GroupService', 'updateGroup').then(function(updatedGroup) {
                /* If the modified group is currently selected then update it */
                if ($scope.groupParams.selectedGroup && $scope.groupParams.selectedGroup.id === updatedGroup.id) {
                    $scope.groupParams.selectedGroup = updatedGroup;
                }
                GroupService.getGroups().then(function(data) {
                    $scope.groupParams.groups = data;
                });
            });
        };

    }]);
});
