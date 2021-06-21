/**
 * @ngdoc function
 * @name ftepApp.controller:FileEditorCtrl
 * @description
 * # FileEditorCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('FileEditorCtrl', ['$scope', 'ProductService', 'EditorService', 'CommonService', '$mdDialog', function ($scope, ProductService, EditorService, CommonService, $mdDialog) {

        $scope.serviceParams = ProductService.params.developer;

        // The modes
        $scope.modes = ['Text', 'Dockerfile', 'Javascript', 'Perl', 'PHP', 'Python', 'Properties', 'Shell', 'XML', 'YAML' ];
        $scope.mode = {};

        $scope.openFile = function(file, event) {
            $scope.serviceParams.openedFile = file;
        };

        $scope.updateMode = function(filename) {
            $scope.serviceParams.activeMode = EditorService.setFileType(filename);
        };

        $scope.refreshMirror = function() {
            // Set mode to default if not yet assigned
            if (!$scope.serviceParams.activeMode) {
                $scope.serviceParams.activeMode = $scope.modes[0];
            }
            if($scope.editor) {
                $scope.editor.setOption("mode", $scope.serviceParams.activeMode.toLowerCase());
            }
        };

        $scope.editorOptions = {
            lineWrapping: true,
            lineNumbers: true,
            autofocus: true
        };

        $scope.codemirrorLoaded = function (editor) {

            // Set mode to default if not yet assigned
            if (!$scope.serviceParams.activeMode) {
                $scope.serviceParams.activeMode = $scope.modes[0];
            }

            // Editor part
            var doc = editor.getDoc();
            editor.focus();

            // Apply mode to editor
            $scope.editor = editor;
            editor.setOption("mode", $scope.serviceParams.activeMode.toLowerCase());

            // Options
            editor.setOption('firstLineNumber', 1);
            doc.markClean();

            editor.on("scrollCursorIntoView", function(){
                $scope.editor = editor;
                editor.setOption("mode", $scope.serviceParams.activeMode.toLowerCase());
            });
            editor.on("focus", function(){
                $scope.editor = editor;
                editor.setOption("mode", $scope.serviceParams.activeMode.toLowerCase());
            });
        };

        $scope.createFileDialog = function($event){
            function CreateFileController($scope, $mdDialog, ProductService) {

                $scope.addFile = function () {
                    var newFile = {
                        filename: $scope.file.filename,
                        content: btoa(unescape(encodeURIComponent('# ' + $scope.file.filename))),
                        service: ProductService.params.developer.selectedService._links.self.href,
                        executable: $scope.file.executable
                    };
                    EditorService.addFile(newFile, '/serviceFiles/', 'Service').then(function(data){
                        ProductService.refreshSelectedService('developer');
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }

            CreateFileController.$inject = ['$scope', '$mdDialog', 'ProductService'];
            $mdDialog.show({
                controller: CreateFileController,
                templateUrl: 'views/developer/templates/createfile.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        $scope.deleteFileDialog = function(file, event){
            CommonService.confirm(event, 'Are you sure you want to delete the file ' + file.name + "?").then(function(confirmed) {
                if (confirmed === false) {
                    return;
                }
                ProductService.removeServiceFile(file).then(function(){
                    ProductService.refreshSelectedService('developer');
                });
            });
        };

    }]);

});
