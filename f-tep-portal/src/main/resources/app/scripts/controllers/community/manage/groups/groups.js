/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityGroupsCtrl
 * @description
 * # CommunityGroupsCtrl
 * Controller of the ftepApp
 */

define(['../../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('CommunityGroupsCtrl', ['GroupService', 'CommonService', '$scope', '$sce', function (GroupService, CommonService, $scope, $sce) {

        /* Get stored Groups details */
        $scope.groupParams = GroupService.params.community;
        $scope.item = "Group";

        /* Get Groups */
        GroupService.getGroups().then(function (data) {
            $scope.groupParams.groups = data;
        });

        /* Update Groups when polling */
        $scope.$on('poll.groups', function (event, data) {
            $scope.groupParams.groups = data;
        });

        /* Select a Group */
        $scope.selectGroup = function (item) {
            $scope.groupParams.selectedGroup = item;
            GroupService.refreshSelectedGroupV2("Community");
        };

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
                GroupService.refreshGroupsV2("Community", "Create");
            });
        };

        /* Remove Group */
        $scope.removeGroup = function (key, group) {
            GroupService.removeGroup(group).then(function (data) {
                GroupService.refreshGroupsV2("Community", "Remove");
            });
        };

        /* Edit Group */
        $scope.editGroupDialog = function($event, selectedGroup) {
            CommonService.editItemDialog($event, selectedGroup, 'GroupService', 'updateGroup').then(function(updatedGroup) {
                GroupService.refreshGroupsV2("Community");
            });
        };

    }]);
});
