/**
 * @ngdoc function
 * @name ftepApp.controller:MessagesCtrl
 * @description
 * # MessagesCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('MessagesCtrl', ['$scope', '$rootScope', '$mdDialog', 'CommonService', 'MessageService',
                                 function ($scope, $rootScope, $mdDialog, CommonService, MessageService) {

            $scope.messages = MessageService.getMessages();
            $scope.messageStatuses = [
                {
                    name: "Error",
                    value: true
                }, {
                    name: "Warning",
                    value: true
                }, {
                    name: "Info",
                    value: true
                }
            ];
            $scope.selected = {};
            $scope.filteredMessages = [];
            $scope.displayFilters = false;

            $scope.toggleFilters = function () {
                $scope.displayFilters = !$scope.displayFilters;
                $scope.$broadcast('rebuild:scrollbar');
            };

            $scope.filterMessages = function () {
                $scope.filteredMessages = [];
                for (var i = 0; i < $scope.messageStatuses.length; i++) {
                    if ($scope.messageStatuses[i].value === true) {
                        $scope.filteredMessages.push($scope.messageStatuses[i].name);
                    }
                }
            };
            $scope.filterMessages();

            $scope.newMessage = function (status, title, description) {
                MessageService.addMessage(status, title, description);
            };

            $scope.selectMessage = function (message) {
                $rootScope.$broadcast('show.message', message);
                var container = document.getElementById('bottombar');
                container.scrollTop = 0;
            };

            $scope.$on('refresh.messages', function (event, result) {
                $scope.messages = result.data;
            });

            $scope.clearMessage = function (event, message) {
                MessageService.clearMessage(message);
            };

            $scope.clearAll = function (event) {
                CommonService.confirm(event, 'Are you sure you want to clear all messages?').then(function (confirmed) {
                    if (confirmed !== false) {
                        MessageService.clearAll();
                    }
                });
            };

            $scope.getColor = function (status) {
                return CommonService.getColor(status);
            };

    }]);
});
