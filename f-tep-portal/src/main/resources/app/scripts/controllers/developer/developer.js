/**
 * @ngdoc function
 * @name ftepApp.controller:DeveloperCtrl
 * @description
 * # DeveloperCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('DeveloperCtrl', ['$scope', 'TabService', 'MessageService',
                                             function ($scope, TabService, MessageService) {

        $scope.developerSideNavs = TabService.getDeveloperSideNavs();
        $scope.navInfo = TabService.navInfo.developer;
        $scope.bottombarNavInfo = TabService.navInfo.bottombar;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.toggleDevPage = function (tab) {
            $scope.navInfo.activeSideNav = tab;
        };

        $scope.toggleBottomView = function(){
            $scope.bottombarNavInfo.bottomViewVisible = !$scope.bottombarNavInfo.bottomViewVisible;
        };

    }]);

});
