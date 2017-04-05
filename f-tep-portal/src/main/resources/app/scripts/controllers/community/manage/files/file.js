/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityFileCtrl
 * @description
 * # CommunityFileCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityFileCtrl', ['FileService', '$scope', function (FileService, $scope) {

        /* Get stored File details */
        $scope.fileParams = FileService.params.community;
        $scope.item = "File";

        $scope.fileTags = ['File', 'Reference', 'testfile'];

        $scope.tempFile = angular.copy($scope.fileParams.selectedFile);

        /* Patch file and update file list */
        $scope.saveFile = function() {
            FileService.updateFtepFile($scope.tempFile).then(function (data) {
                FileService.refreshFtepFilesV2("Community");
            });
        };

    }]);
});
