/**
 * @ngdoc function
 * @name ftepApp.controller:FilesSidebarCtrl
 * @description
 * # FilesSidebarCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('FilesSidebarCtrl', ['$scope',  'FileService', '$rootScope', '$mdDialog', function($scope, FileService, $rootScope, $mdDialog) {

        $scope.filesParams = FileService.params.files;
        $scope.newReferenceFile = {};
        $scope.uploadValid = 'Valid';


        $scope.inputChange = function() {   
            // Call ui-grid update
            $rootScope.$broadcast('filesParamsUpdated', $scope.filesParams.params);
        };

        /* Validate file being uploaded */
        $scope.validateFile = function (file) {
            if(!file) {
                $scope.uploadValid = 'No file selected';
            } else if (file.name.indexOf(' ') >= 0) {
                $scope.uploadValid = 'Filename cannot contain white space';
            } else if (file.size >= (1024*1024*1024*10)) {
                $scope.uploadValid = 'Filesize cannot exceed 10GB';
            } else {
                $scope.uploadValid = 'Valid';
            }
        };

        /* Upload the file */
        $scope.addReferenceFile = function () {
            FileService.uploadFile('files', $scope.newReferenceFile).then(function (response) {
                (function(ev) {
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
