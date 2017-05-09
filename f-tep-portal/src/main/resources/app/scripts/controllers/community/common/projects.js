/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityProjectsCtrl
 * @description
 * # CommunityProjectsCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityProjectsCtrl', ['ProjectService', 'CommonService', '$scope', '$sce', function (ProjectService, CommonService, $scope, $sce) {

        /* Get stored Project details */
        $scope.projectParams = ProjectService.params.community;
        $scope.projectOwnershipFilters = ProjectService.projectOwnershipFilters;
        $scope.item = "Project";

        /* Get Projects */
        ProjectService.refreshProjects("community");

        /* Update Projects when polling */
        $scope.$on('poll.projects', function (event, data) {
            $scope.projectParams.projects = data;
        });

        $scope.getPage = function(url){
            ProjectService.getProjectsPage('community', url);
        };

        $scope.$on("$destroy", function() {
            ProjectService.stopPolling();
        });

        /* Select a Project */
        $scope.selectProject = function (item) {
            $scope.projectParams.selectedProject = item;
            ProjectService.refreshSelectedProject("community");
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.projectParams.displayFilters = !$scope.projectParams.displayFilters;
        };

        $scope.itemSearch = {
            searchText: $scope.projectParams.searchText
        };

        $scope.quickSearch = function (item) {
            if (item.name.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        /* Project Description Popup */
        var popover = {};
        $scope.getDescription = function (item) {
            if (!item.description) {
                item.description = "No description.";
            }
            var html =
                '<div class="metadata">' +
                    '<div class="row">' +
                        '<div class="col-sm-12">' + item.description + '</div>' +
                    '</div>' +
                '</div>';
            return popover[html] || (popover[html] = $sce.trustAsHtml(html));
        };

        /* Create Project */
        $scope.createItemDialog = function ($event) {
            CommonService.createItemDialog($event, 'ProjectService', 'createProject').then(function (newProject) {
                ProjectService.refreshProjects("community", "Create");
            });
        };

        /* Remove Project */
        $scope.removeItem = function (key, item) {
             ProjectService.removeProject(item).then(function (data) {
                 ProjectService.refreshProjects("community", "Remove", item);
            });
        };

        /* Edit Project */
        $scope.editItemDialog = function ($event, item) {
            CommonService.editItemDialog($event, item, 'ProjectService', 'updateProject').then(function (updatedProject) {
                ProjectService.refreshProjects("community");
            });
        };

    }]);
});
