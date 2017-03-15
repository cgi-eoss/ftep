/**
 * @ngdoc function
 * @name ftepApp.controller:DeveloperCtrl
 * @description
 * # DeveloperCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('DeveloperCtrl', ['$scope', 'TabService', 'MessageService', 'ReferenceService',function ($scope, TabService, MessageService, ReferenceService) {

        /* Set active page */
        $scope.navInfo = TabService.navInfo;
        $scope.navInfo.sideViewVisible = false;
        $scope.navInfo.activeTab = TabService.getTabs().DEVELOPER;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        /* Sidebar navigation */
        $scope.developerSideNavs = TabService.getDeveloperSideNavs();
        $scope.navInfo = TabService.navInfo;

        $scope.toggleDevPage = function (tab) {
            $scope.navInfo.activeDeveloperPage = tab;
        };

        $scope.toggleBottomView = function(){
            $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
            $scope.$broadcast('rebuild:scrollbar');
        };

        $scope.newReference = {};

        $scope.addReferenceFile = function () {
            ReferenceService.uploadFile($scope.newReference);
        };

    }]);

});
