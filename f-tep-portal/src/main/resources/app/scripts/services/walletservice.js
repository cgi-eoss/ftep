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
        var userWallet;

        this.params = {
            account: {
                transactions: undefined,
                wallet: undefined
            }
        };

        this.getUserWallet = function(user){
            var deferred = $q.defer();
            userWallet = halAPI.from(rootUri + '/users/' + user.id)
                .newRequest()
                .follow('wallet')
                .getResource();

            userWallet.result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Failed to get Wallet', error);
                    deferred.reject();
                }
            );
            return deferred.promise;
        };

        this.refreshUserTransactions = function (page, user) {
            self.getUserWallet(user).then(function (wallet) {
                self.params[page].wallet = wallet;
                userWallet.continue().then(function(request) {
                    request.follow('transactions')
                        .getResource()
                        .result
                        .then(function (document) {
                            self.params[page].transactions = document._embedded.walletTransactions;
                        }, function (error) {
                           MessageService.addError('Failed to get Transaction History', error);
                       }
                    );
                });
            });
        };

        this.makeTransaction = function(user, wallet, coins){
            return $q(function(resolve, reject) {
                var credit = {amount: coins};
                traverson.from(rootUri).json().useAngularHttp().from(wallet._links.self.href + '/credit')
                         .newRequest()
                         .post(credit)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('User Coin Balance updated', coins + ' coins added to user '.concat(user.name));
                    resolve();
                }, function (error) {
                    MessageService.addError('Failed to update Coin Balance', error);
                    reject();
                });
            });
        };

        return this;
    }]);
});
