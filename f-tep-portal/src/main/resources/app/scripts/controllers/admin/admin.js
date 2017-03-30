/**
 * @ngdoc function
 * @name ftepApp.controller:AdminCtrl
 * @description
 * # AdminCtrl
 * Controller of the admin page
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('AdminCtrl', ['$scope', 'UserService', function ($scope, UserService) {

        $scope.user = undefined;

        UserService.getCurrentUser().then(function(data){
            $scope.user = data;
        });

    }]);
});
