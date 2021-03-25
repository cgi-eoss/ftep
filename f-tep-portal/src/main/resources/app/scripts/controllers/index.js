/**
* @ngdoc function
* @name ftepApp.controller:IndexCtrl
* @description
* # IndexCtrl
* Controller of the ftepApp
*/
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('IndexCtrl', ['ftepProperties', '$scope', '$location', '$window', 'UserService', function (ftepProperties, $scope, $location, $window, UserService) {

        $scope.ftepUrl = ftepProperties.FTEP_URL;
        $scope.sessionEnded = false;
        $scope.timeoutDismissed = false;
        $scope.subscribed = true;
        $scope.activeUser = {};

        $scope.$on('no.user', function() {
            $scope.sessionEnded = true;
        });

        $scope.$on('active.user', function(event, user) {
            if ($scope.activeUser.id && user.id) {
                if ($scope.activeUser.id != user.id) {
                    // User changed, check for subscription
                    $scope.checkActiveSubscription();
                }
            } else if ($scope.activeUser.id || user.id) {
                // User logged in, check for subscription
                $scope.checkActiveSubscription();
            }
            $scope.activeUser = user;
        });

        $scope.hideTimeout = function() {
            $scope.sessionEnded = false;
            $scope.timeoutDismissed = true;
        };

        $scope.reloadRoute = function() {
            $window.location.reload();
        };

        $scope.checkActiveSubscription = function() {
            UserService.getCurrentUserWallet().then(function(wallet) {
                if (wallet.balance <= 0) {
                    $scope.subscribed = false;
                }
            });
        }

        $scope.startTrial = function() {
            UserService.startTrial().then(function() {
                $scope.subscribed = true;
            });
        };

        $scope.goTo = function (path) {
            $location.path(path);
        };

        $scope.version = document.getElementById("version").content;

        $scope.checkActiveSubscription();
        // Trigger a user check to ensure controllers load correctly
        UserService.checkLoginStatus();
    }]);
});
