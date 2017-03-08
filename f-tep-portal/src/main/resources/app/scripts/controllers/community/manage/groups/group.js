/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityGroupsCtrl
 * @description
 * # CommunityGroupsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityGroupCtrl', ['GroupService', 'MessageService', '$rootScope', '$scope', '$mdDialog', '$sce', function (GroupService, MessageService, $rootScope, $scope, $mdDialog, $sce) {

        /* Get stored Groups details */
        $scope.groupParams = GroupService.params;

    }]);
});
