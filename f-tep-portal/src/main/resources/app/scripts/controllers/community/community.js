/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityCtrl
 * @description
 * # CommunityCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityCtrl', ['$scope', 'GroupService', 'UserService', 'MessageService', 'TabService', function ($scope, GroupService, UserService, MessageService, TabService) {

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        /* Sidebar navigation */
        $scope.communityTabs = TabService.getCommunityNavTabs();
        $scope.navInfo = TabService.navInfo;
        $scope.togglePage = function (tab) {
            $scope.navInfo.activeCommunityPage = tab;
        };

        /** Bottom bar **/
        $scope.displayTab = function(tab){
            $scope.navInfo.bottomViewVisible = true;
            $scope.navInfo.activeBottomNav = tab;
        };

        $scope.toggleBottomView = function(){
            $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
        };

    }]);
});
