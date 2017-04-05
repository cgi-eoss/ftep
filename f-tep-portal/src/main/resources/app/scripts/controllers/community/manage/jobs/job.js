/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityJobCtrl
 * @description
 * # CommunityJobCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityJobCtrl', ['JobService', '$scope', function (JobService, $scope) {

        /* Get stored Jobs details */
        $scope.jobParams = JobService.params.community;
        $scope.item = "Job";

    }]);
});

