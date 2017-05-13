/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageJobCtrl
 * @description
 * # CommunityManageJobCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageJobCtrl', ['JobService', '$scope', function (JobService, $scope) {

        /* Get stored Jobs details */
        $scope.jobParams = JobService.params.community;
        $scope.item = "Job";

        $scope.refreshJob = function() {
            JobService.refreshSelectedJob('community');
        };

        $scope.splitInputFiles = function(link) {
            return link.split(',');
        };

    }]);
});

