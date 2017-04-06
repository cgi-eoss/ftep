/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityFilesCtrl
 * @description
 * # CommunityFilesCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityFilesCtrl', ['FileService', '$scope', '$mdDialog', function (FileService, $scope, $mdDialog) {

        /* Get stored Files details */
        $scope.fileParams = FileService.params.community;
        $scope.fileOwnershipFilters = FileService.fileOwnershipFilters;
        $scope.item = "File";
        $scope.filetypes = [
            { name: "Reference Data", value: "REFERENCE_DATA" },
            { name: "Output Products", value: "OUTPUT_PRODUCT" }
        ];

        /* Get files */
        FileService.refreshFtepFiles("Community");

        /* Update files when polling */
        $scope.$on('poll.ftepfiles', function (event, data) {
            $scope.fileParams.files = data;
        });

        /* Select a File */
        $scope.selectFile = function (item) {
            $scope.fileParams.selectedFile = item;
            FileService.refreshSelectedFtepFile("Community");
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.fileParams.displayFilters = !$scope.fileParams.displayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.fileParams.searchText
        };

        $scope.quickSearch = function (item) {
            if (item.filename.toLowerCase().indexOf(
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
                    FileService.uploadFile($scope.newReference).then(function (response) {
                        /* Get updated list of reference data */
                        FileService.refreshFtepFiles("Community");
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
                FileService.refreshFtepFiles("Community", "Remove", item);
            });
        };

    }]);
});
