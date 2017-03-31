/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityFilesCtrl
 * @description
 * # CommunityFilesCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityFilesCtrl', ['MessageService', 'FileService', 'TabService', 'ReferenceService', '$rootScope', '$scope', '$mdDialog', '$sce', function (MessageService, FileService, TabService, ReferenceService, $rootScope, $scope, $mdDialog, $sce) {

        /* Get stored Files details */
        $scope.fileParams = FileService.params.community;
        $scope.files = [];
        $scope.item = "File";
        $scope.filetypes = [
            { name: "Reference Data", value: "REFERENCE_DATA" },
            { name: "Output Products", value: "OUTPUT_PRODUCT" }
        ];

        /* Get files list */
        FileService.getFtepFiles().then(function (data) {
            $scope.files = data;
        });

        /* Update files when polling */
        $scope.$on('refresh.ftepfiles', function (event, data) {
            $scope.files = data;
        });

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.fileParams.displayFilters = !$scope.fileParams.displayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.fileParams.searchText
        };

        $scope.quickSearch = function (item) {
            if (item.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Add reference data */
        $scope.addReferenceFileDialog = function ($event) {
            function AddReferenceFileDialog($scope, $mdDialog, FileService) {

                $scope.item = "File";
                $scope.fileParams = FileService.params.community;
                $scope.newReference = {};

                /* Upload the file */
                $scope.addReferenceFile = function () {
                    ReferenceService.uploadFile($scope.newReference).then(function (response) {
                        /* Get updated list of reference data */
                        FileService.getFtepFiles().then(function (data) {
                            $scope.files = data;
                        });
                    });
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };

            }
            AddReferenceFileDialog.$inject = ['$scope', '$mdDialog', 'FileService'];
            $mdDialog.show({
                controller: AddReferenceFileDialog,
                templateUrl: 'views/community/manage/files/templates/addreferencedata.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        /* Remove File */
        $scope.removeItem = function (key, item) {
            FileService.removeFtepFile(item).then(function (data) {
                /* Clear selected file and file details */
                if (item.id === $scope.fileParams.selectedFile.id) {
                    $scope.fileParams.selectedFile = undefined;
                    $scope.fileParams.fileDetails = [];
                }
                /* Update list of files */
                FileService.getFtepFiles().then(function (files) {
                    $scope.files = files;
                });
            });
        };

    }]);
});
