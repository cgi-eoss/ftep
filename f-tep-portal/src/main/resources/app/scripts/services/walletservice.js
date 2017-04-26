/**
 * @ngdoc service
 * @name ftepApp.WalletService
 * @description
 * # WalletService
 * Service for the user's wallet to keep track of used coins.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('WalletService', [ 'ftepProperties', '$q', 'traverson', 'MessageService', function (ftepProperties, $q, traverson, MessageService) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.getUserWallet = function(user){
            var deferred = $q.defer();
            halAPI.from(user._links.self.href)
                .newRequest()
                .follow('wallet')
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                 }, function (error) {
                    deferred.reject();
                });
            return deferred.promise;
        };

        this.getUserWalletByUserId = function(userId){
            var deferred = $q.defer();
            halAPI.from(rootUri + '/users/' + userId)
                .newRequest()
                .follow('wallet')
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                 }, function (error) {
                    deferred.reject();
                });
            return deferred.promise;
        };

        this.makeTransaction = function(wallet, coins){
           //TODO
        };

    }]);
});