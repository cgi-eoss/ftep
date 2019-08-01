/**
 * @ngdoc service
 * @name ftepApp.ApiKeyService
 * @description
 * # ApiKeyService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules'], function(ftepmodules) {

    ftepmodules.service('ApiKeyService', ['$rootScope', '$http', 'ftepProperties', '$q', '$timeout', 'MessageService', 'UserService', function($rootScope, $http, ftepProperties, $q, $timeout, MessageService, UserService) {

        var self = this;
        var rootUri = ftepProperties.URLv2;


        this.generateApiKey = function() {
            return $http({
                method: 'POST',
                url: rootUri + '/apiKeys/generate'
            }).
            then(function (response) {
                return response.data;
            })
            .catch(function(error) {
                MessageService.addError('Unable to generate API key', error);
                throw error;
            });
        };

        this.deleteApiKey = function(id) {
            return $http({
                method: 'DELETE',
                url: rootUri + '/apiKeys/delete'
            })
            .catch(function(error) {
                MessageService.addError('Unable to delete API key', error);
                throw error;
            });
        };

        this.checkForApiKey = function(id, value) {
            return $http({
                method: 'GET',
                url: rootUri + '/apiKeys/exists'
            }).
            then(function (response) {
                return response.data;
            })
            .catch(function(error) {
                MessageService.addError('Unable to check for API key', error);
                throw error;
            });
        };

        this.regenerateApiKey = function() {
            return $http({
                method: 'POST',
                url: rootUri + '/apiKeys/regenerate'
            }).
            then(function (response) {
                return response.data;
            })
            .catch(function(error) {
                MessageService.addError('Unable to generate API key', error);
                throw error;
            });
        };

        return this;
    }]);
});
