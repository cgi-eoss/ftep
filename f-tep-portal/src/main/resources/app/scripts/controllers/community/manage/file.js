/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageFileCtrl
 * @description
 * # CommunityManageFileCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageFileCtrl', ['CommunityService', 'FileService', 'CommonService', '$scope', function (CommunityService, FileService, CommonService, $scope) {

        /* Get stored File details */
        $scope.fileParams = FileService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "File";

         /* Filters */
        $scope.toggleSharingFilters = function () {
            $scope.fileParams.sharedGroupsDisplayFilters = !$scope.fileParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.fileParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.fileTags = ['File', 'Reference', 'testfile'];

        $scope.refreshFile = function() {
            FileService.refreshSelectedFtepFile('community');
        };

        /* Patch file and update file list */
        $scope.saveFile = function() {
            FileService.updateFtepFile($scope.fileParams.selectedFile).then(function (data) {
                FileService.refreshFtepFiles("community");
            });
        };

        $scope.getGeometryStr = function(geoJson){
            return JSON.stringify(geoJson);
        };

        /* Estimate Download cost */
        $scope.estimateDownloadCost = function($event, file) {
            CommonService.estimateDownloadCost($event, file);
        };

    }]);
});
