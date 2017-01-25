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

        this.addMessage = function (status, title, description) {
            var message = {};
            id = id + 1;
            message.id = id;
            message.status = status;
            message.title = title;
            message.time = new Date().toUTCString();
            message.description = description;
            messages.push(message);
            $rootScope.$broadcast('update.messages');
        };

        this.getMessages = function () {
            return messages;
        };

        this.countMessages = function () {
            return messages.length;
        };

        this.clearMessage = function (message) {
            messages.pop(message);
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
