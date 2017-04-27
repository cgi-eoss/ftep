/**
 * @ngdoc function
 * @name ftepApp.controller:MessagesCtrl
 * @description
 * # MessagesCtrl
 * Controller of the ftepApp
 */
define(['../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('MessagesCtrl', ['$scope', 'CommonService', 'MessageService',
                                 function ($scope, CommonService, MessageService) {

            $scope.messages = MessageService.getMessages();
            $scope.msgParams = MessageService.params;

            $scope.filteredMessages = [];

            $scope.toggleFilters = function () {
                $scope.msgParams.displayFilters = !$scope.msgParams.displayFilters;
            };

            $scope.filterMessages = function () {
                $scope.filteredMessages = [];
                for (var i = 0; i < $scope.msgParams.messageStatuses.length; i++) {
                    if ($scope.msgParams.messageStatuses[i].value === true) {
                        $scope.filteredMessages.push($scope.msgParams.messageStatuses[i].name);
                    }
                }
            };
            $scope.filterMessages();

            $scope.selectMessage = function (message) {
                $scope.msgParams.selectedMessage = message;
                var container = document.getElementById('bottombar');
                container.scrollTop = 0;
            };

            $scope.deselectMessage  = function () {
                $scope.msgParams.selectedMessage = undefined;
            };

            $scope.$on('refresh.messages', function (event, result) {
                $scope.messages = result.data;
            });

            $scope.clearMessage = function (event, message) {
                MessageService.clearMessage(message);
                if($scope.msgParams.selectedMessage && $scope.msgParams.selectedMessage.id === message.id){
                    delete $scope.msgParams.selectedMessage;
                }
            };

            $scope.clearAll = function (event) {
                CommonService.confirm(event, 'Are you sure you want to clear all messages?').then(function (confirmed) {
                    if (confirmed !== false) {
                        MessageService.clearAll();
                        delete $scope.msgParams.selectedMessage;
                    }
                });
            };

    }]);
});
