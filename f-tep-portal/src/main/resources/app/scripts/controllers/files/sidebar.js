/**
 * @ngdoc function
 * @name ftepApp.controller:FilesSidebarCtrl
 * @description
 * # FilesSidebarCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('FilesSidebarCtrl', ["$scope",  "FileService", "$rootScope", function($scope, FileService, $rootScope) {
        $scope.filesParams = FileService.params.files;
        $scope.inputs = {};

        $scope.inputChange = function() {   
            var fileType = $scope.inputs.fileType;
            var keyword = $scope.inputs.keyword;
            var ownership = $scope.inputs.ownership;
            var fileSize = $scope.inputs.fileSize;

            var mappedInputs = {
                fileType: fileType,
                keyword: keyword,
                ownership: ownership,
                fileSize: fileSize
            };
            
            // Set params in service for future calls
            $scope.filesParams.params = mappedInputs;

            // Call ui-grid update
            $rootScope.$broadcast('filesParamsUpdated', mappedInputs);
        }
    }]);

});
