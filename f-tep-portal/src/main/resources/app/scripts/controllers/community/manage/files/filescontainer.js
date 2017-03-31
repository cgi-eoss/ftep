/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityFilesContainerCtrl
 * @description
 * # CommunityFilesContainerCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityFilesContainerCtrl', ['MessageService', 'FileService', '$rootScope', '$scope', '$mdDialog', '$sce', function (MessageService, FileService, $rootScope, $scope, $mdDialog, $sce) {

        /* Get stored Files details */
        $scope.fileParams = FileService.params.community;

        /* Select a File */
        $scope.selectItem = function (item) {
            $scope.fileParams.selectedFile = item;
            FileService.getFileV2(item).then(function (data) {
                $scope.fileParams.fileDetails = data;
            });
        };

    }]);
});
