/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareCtrl
 * @description
 * # CommunityShareCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareCtrl', ['MessageService', 'CommunityService', '$rootScope', '$scope', '$mdDialog', '$sce', '$injector', function (MessageService, CommunityService, $rootScope, $scope, $mdDialog, $sce, $injector) {

        /* Share Object Modal */
        $scope.ace = {};
        $scope.shareObjectDialog = function($event, item, type, groups, serviceName, serviceMethod) {
            function ShareObjectController($scope, $mdDialog, GroupService, CommunityService) {

                var service = $injector.get(serviceName);
                $scope.permissions = CommunityService.permissionTypes;
                $scope.ace = item;
                $scope.ace.type = type;
                $scope.ace.permission = $scope.permissions.READ;
                $scope.groups = [];

                GroupService.getGroups().then(function(data){
                    $scope.groups = data;
                });

                $scope.shareObject = function (item) {
                    CommunityService.shareObject($scope.ace, groups).then(function (data) {
                        service[serviceMethod]('Community');
                    });

                    $mdDialog.hide();
                };

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            ShareObjectController.$inject = ['$scope', '$mdDialog', 'GroupService', 'CommunityService'];
            $mdDialog.show({
                controller: ShareObjectController,
                templateUrl: 'views/common/templates/shareitem.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {}
            });
        };

        $scope.updateGroups = function (item, type, groups, serviceName, serviceMethod) {
            var service = $injector.get(serviceName);
            CommunityService.updateObjectGroups(item, type, groups).then(function (data) {
                service[serviceMethod]('Community');
            });
        };

        $scope.removeGroup = function (item, type, group, groups, serviceName, serviceMethod) {
            var service = $injector.get(serviceName);
            CommunityService.removeAceGroup(item, type, group, groups).then(function (data) {
                service[serviceMethod]('Community');
            });
        };

    }]);
});
