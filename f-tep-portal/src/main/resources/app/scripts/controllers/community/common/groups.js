/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityGroupsCtrl
 * @description
 * # CommunityGroupsCtrl
 * Controller of the ftepApp
 */

define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('CommunityGroupsCtrl', ['GroupService', 'CommonService', '$scope', function (GroupService, CommonService, $scope) {

        /* Get stored Groups details */
        $scope.groupParams = GroupService.params.community;
        $scope.groupOwnershipFilters = GroupService.groupOwnershipFilters;
        $scope.item = "Group";

        /* Get Groups */
        GroupService.refreshGroups('community');

        /* Update Groups when polling */
        $scope.$on('poll.groups', function (event, data) {
            $scope.groupParams.groups = data;
        });

        $scope.getPage = function(url){
            GroupService.getGroupsPage('community', url);
        };

        $scope.$on("$destroy", function() {
            GroupService.stopPolling();
        });

        /* Select a Group */
        $scope.selectGroup = function (item) {
            $scope.groupPermission = item;
            $scope.groupParams.selectedGroup = item;
            GroupService.refreshSelectedGroup("community");
        };

        /* Filters */
        $scope.toggleGroupFilters = function () {
            $scope.groupParams.displayGroupFilters = !$scope.groupParams.displayGroupFilters;
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

        /* Create Group */
        $scope.createGroupDialog = function ($event) {
            CommonService.createItemDialog($event, 'GroupService', 'createGroup').then(function (newGroup) {
                GroupService.refreshGroups("community", "Create");
            });
        };

        /* Remove Group */
        $scope.removeGroup = function (key, group) {
            GroupService.removeGroup(group).then(function (data) {
                GroupService.refreshGroups("community", "Remove");
            });
        };

        /* Edit Group */
        $scope.editGroupDialog = function($event, selectedGroup) {
            CommonService.editItemDialog($event, selectedGroup, 'GroupService', 'updateGroup').then(function(updatedGroup) {
                GroupService.refreshGroups("community");
            });
        };

    }]);
});
