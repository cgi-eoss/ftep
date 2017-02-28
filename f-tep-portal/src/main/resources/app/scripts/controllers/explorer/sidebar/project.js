/**
 * @ngdoc function
 * @name ftepApp.controller:ProjectCtrl
 * @description
 * # ProjectCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('ProjectCtrl', ['$scope', 'ProjectService', function ($scope, ProjectService) {

        $scope.projects = [{
                "type": "project",
                "id": "0",
                "attributes": {
                    "name": "Default Project",
                    "description": "Default project"
                }
            },
            {
                "type": "project",
                "id": "1",
                "attributes": {
                    "name": "Test Project",
                    "description": "Test project"
                }
            }];

        $scope.activeProject = $scope.projects[0];

        $scope.setActiveProject = function (project) {
            $scope.activeProject = project;
            $('.project-list').on('click', 'li', function () {
                $(this).addClass('active').siblings().removeClass('active');
            });
        };

        ProjectService.getProjects().then(function (data) {
            $scope.projects.push.apply($scope.projects, data);
        });

        $scope.removeProject = function (project) {
            ProjectService.removeProject(project).then(function () {
                var i = $scope.projects.indexOf(project);
                $scope.projects.splice(i, 1);
            });
        };

        $scope.$on('add.project', function (event, data) {
            $scope.projects.push(data);
        });

        $scope.shareProject = function ($event) {
            $event.stopPropagation();
            $event.preventDefault();
        };

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
