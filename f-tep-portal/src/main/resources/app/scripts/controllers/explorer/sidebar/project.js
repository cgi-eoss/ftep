/**
 * @ngdoc function
 * @name ftepApp.controller:ProjectCtrl
 * @description
 * # ProjectCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ProjectCtrl', ['$scope', '$rootScope', '$mdDialog', 'ProjectService', 'CommonService',
                                               function ($scope, $rootScope, $mdDialog, ProjectService, CommonService) {

        $scope.projectParams = ProjectService.params.explorer;
        $scope.projectOwnershipFilters = ProjectService.projectOwnershipFilters;

        $scope.selectProject = function (project) {
            $scope.projectParams.selectedProject = project;
        };

        ProjectService.refreshProjects('explorer');

        $scope.removeProject = function (project) {
            ProjectService.removeProject(project).then(function () {
                ProjectService.refreshProjects('explorer', 'Remove');
            });
        };

        $scope.getPage = function(url){
            ProjectService.getProjectsPage('explorer', url);
        };


        /** CREATE PROJECT MODAL **/
        $scope.createProjectDialog = function($event) {
            CommonService.createItemDialog($event, 'ProjectService', 'createProject').then(function (newProject) {
                ProjectService.refreshProjects('explorer', 'Create');
            });
        };
        /** END OF CREATE PROJECT MODAL **/

        $scope.updateProject = function ($event, project) {
            CommonService.editItemDialog($event, project, 'ProjectService', 'updateProject').then(function(updatedProject) {
                ProjectService.refreshProjects('explorer');
            });
        };

        $scope.projectShow = false;
        $scope.projectShowIcon = "expand_more";

        $scope.toggleProjectShow = function () {

            $scope.projectShow = !$scope.projectShow;

            if ($scope.projectShow === true) {
                $scope.projectShowIcon = "expand_less";
            } else {
                $scope.projectShowIcon = "expand_more";
            }
        };

    }]);

});
