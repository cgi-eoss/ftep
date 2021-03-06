/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityFilesCtrl
 * @description
 * # CommunityFilesCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityFilesCtrl', ['FileService', 'CommonService', '$scope', '$mdDialog', function (FileService, CommonService, $scope, $mdDialog) {

        /* Get stored Files details */
        $scope.fileParams = FileService.params.community;
        $scope.fileOwnershipFilters = FileService.fileOwnershipFilters;
        $scope.item = "File";
        $scope.filetypes = [
            { name: "Reference Data", value: "REFERENCE_DATA" },
            { name: "Output Products", value: "OUTPUT_PRODUCT" }
        ];

        /* Get files */
        FileService.refreshFtepFiles("community");

        /* Update files when polling */
        $scope.$on('poll.ftepfiles', function (event, data) {
            $scope.fileParams.files = data;
        });

        /* Stop Polling */
        $scope.$on("$destroy", function() {
            FileService.stopPolling();
        });

        /* Paging */
        $scope.getPage = function(url){
            FileService.getFtepFilesPage('community', url);
        };

        $scope.filter = function(){
            FileService.getFtepFilesByFilter('community');
        };

        /* Select a File */
        $scope.selectFile = function (item) {
            $scope.fileParams.selectedFile = item;
            FileService.refreshSelectedFtepFile("community");
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.fileParams.displayFilters = !$scope.fileParams.displayFilters;
        };

        /* Add reference data */
        $scope.addReferenceFileDialog = function ($event) {
            function AddReferenceFileDialog($scope, $mdDialog, FileService) {

                $scope.item = "File";
                $scope.fileParams = FileService.params.community;
                $scope.newReference = {
                    filetype: "",
                    autoDetectDisabled: true,
                    autoDetectGeometry: false,
                };
                $scope.validation = "Valid";

                // Fetch all file types
                FileService.getCatalogueFileTypes().then(function (result) {
                    $scope.types = result;
                });

                // called upon dropbox change
                $scope.populateFileType = function(file) {

                    $scope.resetProgressBar();
                    if ($scope.validation === "Valid") {
                        FileService.getFileTypeByExtension(file.name.substring(file.name.lastIndexOf(".") + 1)).then(function (result) {
                            $scope.newReference.filetype = result;
                            $scope.toggleEnableAutoDetectGeometry();
                        });
                    }
                };

                // called upon option value change
                $scope.toggleEnableAutoDetectGeometry = function() {

                    FileService.getAutoDetectFlag($scope.newReference.filetype).then(function (result) {
                        $scope.newReference.autoDetectDisabled = !result;
                        $scope.newReference.autoDetectGeometry = result;
                    });
                }

                $scope.validateFile = function (file) {
                    if(!file) {
                        $scope.validation = "No file selected";
                    } else if (file.name.indexOf(' ') >= 0) {
                        $scope.validation = "Filename cannot contain white space";
                    } else if (file.size > (1024*1024*1024*10)) {
                        $scope.validation = "Filesize cannot exceed 10GB";
                    } else {
                        $scope.validation = "Valid";
                    }
                };

                $scope.resetProgressBar = function() {
                    $scope.fileParams.progressPercentage = 0;
                    $scope.fileParams.uploadStatus = 'pending';
                    $scope.fileParams.uploadMessage = undefined;
                }

                /* Upload the file */
                $scope.addReferenceFile = function () {
                    if ($scope.newReference.autoDetectGeometry) {
                        $scope.newReference.geometry = "Auto-detected";
                    }
                    FileService.uploadFile("community", $scope.newReference).then(function (response) {
                        /* Get updated list of reference data */
                        FileService.refreshFtepFiles("community");
                    });
                };

                $scope.closeDialog = function () {
                    $scope.resetProgressBar();
                    $mdDialog.hide();
                };

            }
            AddReferenceFileDialog.$inject = ['$scope', '$mdDialog', 'FileService'];
            $mdDialog.show({
                controller: AddReferenceFileDialog,
                templateUrl: 'views/community/templates/addreferencedata.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        /* Remove File */
        $scope.removeItem = function (event, key, item) {
            FileService.getFileReferencers(item).then(function(res) {
                var msg = 'Are you sure you want to delete this file?\n' + 'Removing file from ' + res.databaskets.length + ' databaskets, ' + res.jobs.length + ' jobs and ' + res.jobConfigs.length + ' job configurations.';
                CommonService.confirm(event, msg).then(function (confirmed) {
                    if (confirmed !== false) {
                        FileService.removeFtepFile(item).then(function (data) {
                            /* Update list of files */
                            FileService.refreshFtepFiles("community", "Remove", item);
                        });
                    }
                });
            });

        };

    }]);
});
