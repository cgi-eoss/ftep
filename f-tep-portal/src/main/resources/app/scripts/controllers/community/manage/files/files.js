/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityFilesCtrl
 * @description
 * # CommunityFilesCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityFilesCtrl', ['FileService', 'ReferenceService', '$scope', '$mdDialog', function (FileService, ReferenceService, $scope, $mdDialog) {

        /* Get stored Files details */
        $scope.fileParams = FileService.params.community;
        $scope.item = "File";
        $scope.filetypes = [
            { name: "Reference Data", value: "REFERENCE_DATA" },
            { name: "Output Products", value: "OUTPUT_PRODUCT" }
        ];

        /* Get files */
        FileService.refreshFtepFilesV2("Community");

        /* Update files when polling */
        $scope.$on('poll.ftepfiles', function (event, data) {
            $scope.fileParams.files = data;
        });

        /* Select a File */
        $scope.selectFile = function (item) {
            $scope.fileParams.selectedFile = item;
            FileService.refreshSelectedFtepFileV2("Community");
        };

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
                        FileService.refreshFtepFilesV2("Community");
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
                /* Update list of files */
                FileService.refreshFtepFilesV2("Community", "Remove", item);
            });
        };

    }]);
});
