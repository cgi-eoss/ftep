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

        $scope.setActiveProject = function (project) {
            $scope.projectParams.activeProject = project;
        };

        function getProjects(project, setAsActive){
            ProjectService.getProjects().then(function (data) {
                $scope.projects = data;
                if(!$scope.projectParams.activeProject){
                    $scope.projectParams.activeProject = $scope.projects[0];
                }
                else if(setAsActive){
                    $scope.projectParams.activeProject = project;
                }
                else if(project && project._links.self.href === $scope.projectParams.activeProject._links.self.href){
                    $scope.projectParams.activeProject = project;
                }
            });
        }
        getProjects();

        $scope.removeProject = function (project) {
            ProjectService.removeProject(project).then(function () {
                getProjects(project);
            });
        };

        $scope.$on('add.project', function (event, project) {
            getProjects(project, true);
        });

        /** CREATE PROJECT MODAL **/
        $scope.createProjectDialog = function($event) {
            CommonService.createItemDialog($event, 'ProjectService', 'createProject').then(function (newProject) {
                $rootScope.$broadcast('add.project', newProject);
            });
        };
        /** END OF CREATE PROJECT MODAL **/

        $scope.updateProject = function ($event, project) {
            CommonService.editItemDialog($event, project, 'ProjectService', 'updateProject').then(function(updatedProject) {
                if($scope.projectParams.activeProject && $scope.projectParams.activeProject.id === updatedProject.id){
                    $scope.projectParams.activeProject = updatedProject;
                }
                getProjects(updatedProject);
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
