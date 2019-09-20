/**
 * @ngdoc function
 * @name ftepApp.controller:AccountCtrl
 * @description
 * # AccountCtrl
 * Controller of the account page
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('AccountCtrl', ['ftepProperties', '$scope', 'UserService', 'ApiKeyService', 'WalletService', 'TabService', 'MessageService', '$mdDialog', function (ftepProperties, $scope, UserService, ApiKeyService, WalletService, TabService, MessageService, $mdDialog) {

        var onUserChange = function() {
            $scope.user = UserService.params.activeUser;
            if ($scope.user.id) {
                $scope.checkForApiKey();
            }
        };

        /* Sidenav & Bottombar */
        $scope.navInfo = TabService.navInfo.admin;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.toggleBottomView = function(){
            $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
        };
        /* End Sidenav & Bottombar */

        $scope.ftepURL = ftepProperties.FTEP_URL;
        $scope.ssoURL = ftepProperties.SSO_URL;
        $scope.walletParams = WalletService.params.account;

        $scope.user = UserService.params.activeUser;
        WalletService.refreshUserTransactions('account', $scope.user);

        $scope.checkForApiKey = function() {
            $scope.apiKeyStatus = 'loading';
            ApiKeyService.checkForApiKey().then(function(hasApiKey) {
                $scope.hasApiKey = hasApiKey;
                $scope.apiKeyStatus = 'ready';
            }).catch(function(error) {
                $scope.apiKeyStatus = 'error'
            });
        };

        $scope.generateApiKey = function() {
            if ($scope.hasApiKey) {
                var confirmDialog = $mdDialog.confirm()
                    .title('New API key generation')
                    .textContent('Generating a new key will invalidate the previous one. Would you like to continue?')
                    .ariaLabel('API key generation')
                    .ok('Yes')
                    .cancel('No');

                $mdDialog.show(confirmDialog).then(function() {

                    $scope.apiKeyStatus = 'loading';
                    ApiKeyService.regenerateApiKey().then(function(apiToken) {
                        $scope.apiKeyStatus = 'ready';
                        showApiTokenDialog(apiToken);
                    }).catch(function(error) {
                        $scope.apiKeyStatus = 'ready';
                    });
                });

            } else {
                $scope.apiKeyStatus = 'loading';
                ApiKeyService.generateApiKey().then(function(apiToken) {
                    $scope.hasApiKey = true;
                    $scope.apiKeyStatus = 'ready';
                    showApiTokenDialog(apiToken);
                }).catch(function(error) {
                    $scope.apiKeyStatus = 'ready';
                });
            }
        }

        $scope.deleteApiKey = function() {
            var confirmDialog = $mdDialog.confirm()
            .title('Confirm API key deletion')
            .textContent('Are you sure you want to delete your existing API key?')
            .ariaLabel('Confirm API key deletion')
            .ok('Yes')
            .cancel('No');

            $mdDialog.show(confirmDialog).then(function() {
                $scope.apiKeyStatus = 'loading';
                ApiKeyService.deleteApiKey().then(function() {
                    $scope.apiKeyStatus = 'ready';
                    $scope.hasApiKey = false;
                }).catch(function(error) {
                    $scope.apiKeyStatus = 'ready';
                });
            })

        }

        function showApiTokenDialog(apiToken) {

            if (apiToken) {
                $mdDialog.show({
                    controller: ['$scope', '$mdDialog', function($scope, $mdDialog) {

                        $scope.apiToken = apiToken;

                        $scope.hideApiTokenDialog = function() {
                            $mdDialog.hide();
                        }

                        $scope.copyTokenToClipboard = function(input) {
                            var textArea = document.createElement("textarea");
                            textArea.value = apiToken;
                            document.body.appendChild(textArea);
                            textArea.select();
                            document.execCommand('copy');
                            document.body.removeChild(textArea);
                        }
                    }],
                    templateUrl: 'views/account/apitokendialog.html',
                    parent: angular.element(document.body),
                    clickOutsideToClose: false
                });
            }
        }

        $scope.$on('active.user', onUserChange);
        onUserChange();

        $scope.hideContent = true;
        var navbar, userdetails, sidenav;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'userdetails':
                    userdetails = true;
                    break;
            }

            if (navbar && sidenav && userdetails) {
                $scope.hideContent = false;
            }
        };

    }]);
});
