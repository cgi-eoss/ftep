/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityFileCtrl
 * @description
 * # CommunityFileCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityFileCtrl', ['MessageService', 'FileService', 'TabService', 'ReferenceService', '$rootScope', '$scope', '$mdDialog', '$sce', function (MessageService, FileService, TabService, ReferenceService, $rootScope, $scope, $mdDialog, $sce) {

        /* Get stored File details */
        $scope.fileParams = FileService.params.community;
        $scope.item = "File";

        $scope.fileTags = ['File', 'Reference', 'testfile'];

        $scope.tempFile = angular.copy($scope.fileParams.selectedFile);

        /* Patch file and update file list */
        $scope.saveFile = function() {
            FileService.updateFtepFile($scope.tempFile).then(function (data) {
                FileService.getFtepFiles().then(function (data) {
                    $scope.fileParams.files = data;
                    /* If the modified item is currently selected then update it */
                    if ($scope.fileParams.selectedFile.id === $scope.tempFile.id) {
                        $scope.fileParams.selectedFile = $scope.tempFile;
                    }
                });
            });
        };

    }]);
});
