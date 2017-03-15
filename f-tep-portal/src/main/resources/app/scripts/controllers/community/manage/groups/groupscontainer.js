/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityGroupsContainerCtrl
 * @description
 * # CommunityGroupsContainerCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityGroupsContainerCtrl', ['GroupService', 'UserService', 'MessageService', '$rootScope', '$scope', '$sce', function (GroupService, UserService, MessageService, $rootScope, $scope, $sce) {

        /* Get stored Groups details */
        $scope.groupParams = GroupService.params;
        $scope.userParams = UserService.params;

        /* Select a Group */
        $scope.selectGroup = function (group) {
            $scope.groupParams.selectedGroup = group;
            UserService.getUsers(group).then(function (data) {
                $scope.userParams.groupUsers = data;
            });
        };

        /* Groups right display */
        $scope.displayRight = function() {
            if ($scope.groupParams.selectedGroup === undefined) {
                return false;
            }
            return true;
        };

    }]);
});
