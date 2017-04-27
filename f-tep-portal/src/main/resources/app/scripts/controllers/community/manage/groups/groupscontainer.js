/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityGroupsContainerCtrl
 * @description
 * # CommunityGroupsContainerCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityGroupsContainerCtrl', ['GroupService', 'UserService', '$scope', function (GroupService, UserService, $scope) {

        /* Get stored Groups details */
        $scope.groupParams = GroupService.params;
        $scope.userParams = UserService.params.community;

        /* Select a Group */
        $scope.selectGroup = function (group) {
            $scope.groupParams.selectedGroup = group;
            UserService.getUsers(group).then(function (data) {
                $scope.userParams.groupUsers = data;
            });
        };

    }]);
});
