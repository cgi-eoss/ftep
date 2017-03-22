/**
 * @ngdoc function
 * @name ftepApp.controller:ProjectCtrl
 * @description
 * # ProjectCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ProjectCtrl', ['$scope', '$rootScope', '$mdDialog', 'ProjectService',
                                               function ($scope, $rootScope, $mdDialog, ProjectService) {

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
                else if(project && project.name === $scope.projectParams.activeProject.name){
                    $scope.projectParams.activeProject = $scope.projects[0];
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
        $scope.newProject = {name: undefined, description: undefined};
        $scope.createProjectDialog = function($event) {
            $event.stopPropagation();
            $event.preventDefault();
            function CreateProjectController($scope, $mdDialog, ProjectService) {
                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
                $scope.addProject = function() {
                    ProjectService.createProject($scope.newProject).then(function(project){
                        $rootScope.$broadcast('add.project', project);
                    });
                    $mdDialog.hide();
                };
            }
            CreateProjectController.$inject = ['$scope', '$mdDialog', 'ProjectService'];
            $mdDialog.show({
              controller: CreateProjectController,
              templateUrl: 'views/explorer/templates/createproject.tmpl.html',
              parent: angular.element(document.body),
              targetEvent: $event,
              clickOutsideToClose: true,
              locals: {
                  items: $scope.items
              }
           });
        };
        /** END OF CREATE PROJECT MODAL **/

        var projectCache = {};

        $scope.cacheProject = function (project) {
            if (projectCache[project.id] === undefined) {
                projectCache[project.id] = angular.copy(project);
            }
        };

        $scope.getProjectCache = function (project) {
            var result;
            if (projectCache[project.id] !== undefined) {
                result = angular.copy(projectCache[project.id]);
                projectCache[project.id] = undefined;
            }
            return result;
        };

        $scope.updateProject = function (enterClicked, project) {
            if (enterClicked) {
                ProjectService.updateProject(project).then(function () {
                    projectCache[project.id] = undefined;
                });
            }
            return !enterClicked;
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
