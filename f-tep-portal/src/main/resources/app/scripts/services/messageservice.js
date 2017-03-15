/**
 * @ngdoc service
 * @name ftepApp.MessageService
 * @description
 * # MessageService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('MessageService', [ '$rootScope', '$q', function ($rootScope, $q) {

        var messages = [];
        var id = 0;

        function addMessage(status, title, description) {
            var message = {};
            id = id + 1;
            message.id = id;
            message.status = status;
            message.title = title;
            message.time = new Date().toUTCString();
            message.description = description ? description : title;
            messages.push(message);
            $rootScope.$broadcast('update.messages');
        }

        this.addError = function(title, description){
            addMessage('Error', title, description);
        };

        this.addWarning = function(title, description){
            addMessage('Warning', title, description);
        };

        this.addInfo = function(title, description){
            addMessage('Info', title, description);
        };

        this.getMessages = function () {
            return messages;
        };

        this.countMessages = function () {
            return messages.length;
        };

        this.clearMessage = function (message) {
            var index = messages.indexOf(message);
            messages.splice( index, 1 );
            $rootScope.$broadcast('update.messages');
        };

        this.clearAll = function () {
            while (messages.length) {
                messages.pop();
            }
            $rootScope.$broadcast('update.messages');
        };

        this.params = {
                messageStatuses: [
                    {
                         name: "Error",
                         value: true
                    },
                    {
                         name: "Warning",
                         value: true
                    },
                    {
                         name: "Info",
                         value: true
                    }
                ],
                selectedMessage: undefined,
                displayFilters: false
        };

        return this;
    }]);
});
