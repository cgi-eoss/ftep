/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityShareCtrl
 * @description
 * # CommunityShareCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityShareCtrl', ['CommonService', 'CommunityService', '$scope', '$injector', function (CommonService, CommunityService, $scope, $injector) {

        /* Share Object Modal */
        $scope.ace = {};
        $scope.shareObjectDialog = function($event, item, type, groups, serviceName, serviceMethod) {
            CommonService.shareObjectDialog($event, item, type, groups, serviceName, serviceMethod, 'community');
        };

        $scope.updateGroups = function (item, type, groups, serviceName, serviceMethod) {
            var service = $injector.get(serviceName);
            CommunityService.updateObjectGroups(item, type, groups).then(function (data) {
                service[serviceMethod]('community');
            });
        };

        $scope.removeGroup = function (item, type, group, groups, serviceName, serviceMethod) {
            var service = $injector.get(serviceName);
            CommunityService.removeAceGroup(item, type, group, groups).then(function (data) {
                service[serviceMethod]('community');
            });
        };

    }]);
});
