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

        function getErrorMessage (error) {
            var message = '';
            if(error){
                if (typeof error === 'string') {
                   message = error;
                } else if (error.data && error.data.errors && error.data.errors.length > 0) {
                     message = error.data.errors[0].title;
                } else if (error.data && typeof error.data === 'string' && error.data.indexOf('message') > 0) {
                    message = JSON.parse(error.data).message;
                } else if (error.doc && error.doc.message) {
                     message = error.doc.message;
                }
            }
            return message;
        }

        function addMessage(status, title, description, response) {
            var message = {};
            id = id + 1;
            message.id = id;
            message.status = status;
            message.title = title;
            message.time = new Date().toUTCString();
            message.description = description ? description : null;

            if (response) {
                message.response = JSON.stringify(response, null, 4);

                if (message.response.indexOf('403') > 0) {
                    message.description = 'Access Denied. Please ensure your session has not timed out and that you have the correct access rights.';
                }
            }

            messages.push(message);
            $rootScope.$broadcast('update.messages');
        }

        this.addError = function(title, error){
            addMessage('Error', title, getErrorMessage(error), error);
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
