/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareJobCtrl
 * @description
 * # CommunityShareJobCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareJobCtrl', ['JobService', '$scope', function (JobService, $scope) {

        /* Get stored Jobs details */
        $scope.jobParams = JobService.params.community;
        $scope.item = "Job";

    }]);
});

