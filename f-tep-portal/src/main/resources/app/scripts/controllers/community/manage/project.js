/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageProjectCtrl
 * @description
 * # CommunityManageProjectCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityManageProjectCtrl', ['ProjectService', 'MessageService', '$rootScope', '$scope', '$mdDialog', function (ProjectService, MessageService, $rootScope, $scope, $mdDialog) {

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

    }]);
});
