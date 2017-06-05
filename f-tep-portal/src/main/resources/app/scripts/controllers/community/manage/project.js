/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageProjectCtrl
 * @description
 * # CommunityManageProjectCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageProjectCtrl', ['CommunityService', 'ProjectService', '$scope', '$mdDialog', function (CommunityService, ProjectService, $scope, $mdDialog) {

        /* Get stored Projects & Contents details */
        $scope.projectParams = ProjectService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
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
                $scope.contentsSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.toggleSharingFilters = function () {
            $scope.projectParams.sharedGroupsDisplayFilters = !$scope.projectParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.projectParams.sharedGroupsSearchText.toLowerCase()) > -1) {
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
                templateUrl: 'views/community/manage/templates/addcontents.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
           });
        };

        /* Remove file from project */
        $scope.removeProjectItem = function(files, file) {
            ProjectService.removeProjectItem($scope.projectParams.selectedProject, files, file).then(function (data) {
                ProjectService.refreshProjects("community");
                /* TODO: Implement removeProjectItem in ProjectService */
            });
        };

    }]);
});
