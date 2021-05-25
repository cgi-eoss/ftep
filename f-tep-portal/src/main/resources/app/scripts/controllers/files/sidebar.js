/**
 * @ngdoc function
 * @name ftepApp.controller:FilesSidebarCtrl
 * @description
 * # FilesSidebarCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('FilesSidebarCtrl', ['$scope', 'FileService', '$rootScope', '$mdDialog', function ($scope, FileService, $rootScope, $mdDialog) {

        $scope.filesParams = FileService.params.files;

        // Reset the progress bar
        $scope.filesParams.progressPercentage = 0;
        $scope.filesParams.uploadStatus = 'pending';
        $scope.filesParams.uploadMessage = undefined;

        $scope.newReferenceFile = {
            filetype: "",
            autoDetectDisabled: true,
            autoDetectGeometry: false,
        };
        $scope.uploadValid = "Valid";

        $scope.inputChange = function () {
            // Call ui-grid update
            $rootScope.$broadcast('filesParamsUpdated', $scope.filesParams.params);
        };

        // Fetch all file types
        FileService.getCatalogueFileTypes().then(function (result) {
            $scope.types = result;
        });

        // called upon dropbox change
        $scope.populateFileType = function (file) {
            $scope.resetProgressBar();
            if ($scope.uploadValid === "Valid") {
                FileService.getFileTypeByExtension(file.name.substring(file.name.lastIndexOf(".") + 1)).then(function (result) {
                    $scope.newReferenceFile.filetype = result;
                    $scope.toggleEnableAutoDetectGeometry();
                });
            }
        };

        // called upon option value change
        $scope.toggleEnableAutoDetectGeometry = function () {
            FileService.getAutoDetectFlag($scope.newReferenceFile.filetype).then(function (result) {
                $scope.newReferenceFile.autoDetectDisabled = !result;
                $scope.newReferenceFile.autoDetectGeometry = result;
            });
        };

        /* Validate file being uploaded */
        $scope.validateFile = function (file) {
            if (!file) {
                $scope.uploadValid = 'No file selected';
            } else if (file.name.indexOf(' ') >= 0) {
                $scope.uploadValid = 'Filename cannot contain white space';
            } else if (file.size > (1024 * 1024 * 1024 * 10)) {
                $scope.uploadValid = 'Filesize cannot exceed 10GB';
            } else {
                $scope.uploadValid = 'Valid';
            }
        };

        $scope.resetProgressBar = function () {
            $scope.filesParams.progressPercentage = 0;
            $scope.filesParams.uploadStatus = 'pending';
            $scope.filesParams.uploadMessage = undefined;
        };

        /* Upload the file */
        $scope.addReferenceFile = function () {
            if ($scope.newReferenceFile.autoDetectGeometry) {
                $scope.newReferenceFile.geometry = "Auto-detected";
            }

            FileService.uploadFile('files', $scope.newReferenceFile).then(function (response) {
                (function (ev) {
                    $mdDialog.show(
                        $mdDialog.alert()
                            .clickOutsideToClose(true)
                            .title('Upload Successful')
                            .textContent(response.data.filename + ' has been uploaded successfully!')
                            .ariaLabel('File Upload Success Dialog')
                            .ok('OK')
                            .targetEvent(ev)
                    );
                })();
                /* Updated list of files */
                $rootScope.$broadcast('filesParamsUpdated', $scope.filesParams.params);

            });
        };

    }]);

});
