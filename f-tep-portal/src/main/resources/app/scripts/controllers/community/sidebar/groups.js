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

        /* Stop polling */
        $scope.$on("$destroy", function() {
            GroupService.stopPolling();
        });

        $scope.filter = function(){
            GroupService.getGroupsByFilter('community');
        };

        $scope.getPage = function(url){
            GroupService.getGroupsPage('community', url);
        };

        /* Select a Group */
        $scope.selectGroup = function (item) {
            $scope.groupParams.selectedGroup = item;
            GroupService.refreshSelectedGroup("community");
        };

        /* Filters */
        $scope.toggleGroupFilters = function () {
            $scope.groupParams.displayGroupFilters = !$scope.groupParams.displayGroupFilters;
        };

        /* Create Group */
        $scope.createGroupDialog = function ($event) {
            CommonService.createItemDialog($event, 'GroupService', 'createGroup').then(function (newGroup) {
                GroupService.refreshGroups("community", "Create", newGroup);
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
