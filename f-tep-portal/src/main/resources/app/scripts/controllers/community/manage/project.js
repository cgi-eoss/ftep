/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageProjectCtrl
 * @description
 * # CommunityManageProjectCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageProjectCtrl', ['ProjectService', '$scope', '$mdDialog', function (ProjectService, $scope, $mdDialog) {

        /* Get stored Projects & Contents details */
        $scope.projectParams = ProjectService.params.community;
        $scope.item = "Project Item";

        /* Filters */
        $scope.toggleContentsFilters = function () {
            $scope.projectParams.displayContentsFilters = !$scope.projectParams.displayContentsFilters;
        };

        $scope.contentsSearch = {
            searchText: $scope.projectParams.contentsSearchText
        };

        $scope.contentsQuickSearch = function (item) {
            if (item.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshProject = function() {
            ProjectService.refreshSelectedProject('community');
        };

        /* Add content to a project */
        $scope.addProjectContentDialog = function($event) {
            function AddProjectContentController($scope, $mdDialog, ProjectService) {

                $scope.projectParams = ProjectService.params.community;

                /* TODO: Add content to a project*/

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            AddProjectContentController.$inject = ['$scope', '$mdDialog', 'ProjectService'];
            $mdDialog.show({
                controller: AddProjectContentController,
                templateUrl: 'views/community/manage/projects/templates/addcontents.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
           });
        };

        /* Remove file from project */
        $scope.removeItem = function(files, file) {
            ProjectService.removeItem($scope.projectParams.selectedProject, files, file).then(function (data) {
                ProjectService.refreshProjects("community");
                /* TODO: Implement removeItem in ProjectService */
            });
        };

    }]);
});
