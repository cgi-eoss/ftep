/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageFileCtrl
 * @description
 * # CommunityManageFileCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageFileCtrl', ['FileService', '$scope', function (FileService, $scope) {

        /* Get stored File details */
        $scope.fileParams = FileService.params.community;
        $scope.item = "File";

        $scope.fileTags = ['File', 'Reference', 'testfile'];

        $scope.refreshFile = function() {
            FileService.refreshSelectedFtepFile('community')
        };

        /* Patch file and update file list */
        $scope.saveFile = function() {
            FileService.updateFtepFile($scope.fileParams.selectedFile).then(function (data) {
                FileService.refreshFtepFiles("community");
            });
        };

    }]);
});
