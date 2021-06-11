/**
 * @ngdoc service
 * @name ftepApp.SubscriptionService
 * @description
 * # SubscriptionService
 * Service for subscriptions.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('SubscriptionService', [ 'ftepProperties', '$q', 'traverson', function (ftepProperties, $q, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.getUserSubscriptions = function(user) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/subscriptions/search/findByOwner?owner=' + user._links.self.href)
                .newRequest()
                .getResource()
                .result
                .then(
            function(document) {
                deferred.resolve(document);
            }, function(error) {
                MessageService.addError('Failed to get subscriptions for user ' + user.name, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.updateSubscription = function(subscription) {
            var patchedSubscription = {
                packageName: subscription.packageName,
                storageQuota: subscription.storageQuota,
                processingQuota: subscription.processingQuota,
                subscriptionStart: subscription.subscriptionStart,
                subscriptionEnd: subscription.subscriptionEnd,
                commentText: subscription.commentText
            };

            var deferred = $q.defer();
            halAPI.from(rootUri + '/subscriptions/' + subscription.id)
                .newRequest()
                .patch(patchedSubscription)
                .result
                .then(
            function(document) {
                deferred.resolve(document);
            }, function(error) {
                MessageService.addError('Failed to update subscription ' + subscription.id, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.createSubscription = function(subscription, subscriptionOwner, subscriptionCreator) {
            var newSubscription = {
                owner: subscriptionOwner._links.self.href,
                packageName: subscription.packageName,
                storageQuota: subscription.storageQuota,
                processingQuota: subscription.processingQuota,
                subscriptionStart: subscription.subscriptionStart,
                subscriptionEnd: subscription.subscriptionEnd,
                commentText: subscription.commentText,
                creator: subscriptionCreator._links.self.href
            };

            var deferred = $q.defer();
            halAPI.from(rootUri + '/subscriptions')
                .newRequest()
                .post(newSubscription)
                .result
                .then(
            function(document) {
                deferred.resolve(document);
            }, function(error) {
                MessageService.addError('Failed to update subscription ' + subscription.id, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.deleteSubscription = function(subscription) {
            var deferred = $q.defer();
            deleteAPI.from(rootUri + '/subscriptions/' + subscription.id)
                .newRequest()
                .delete()
                .result
                .then(
            function(document) {
                if (200 <= document.status && document.status < 300) {
                    deferred.resolve(document);
                } else {
                    MessageService.addError('Failed to delete subscription ' + subscription.id, error);
                    deferred.reject();
                }
            }, function(error) {
                MessageService.addError('Failed to delete subscription ' + subscription.id, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.cancelSubscription = function(subscription) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/subscriptions/' + subscription.id + "/cancel")
                .newRequest()
                .post()
                .result
                .then(
            function(document) {
                deferred.resolve(document);
            }, function(error) {
                MessageService.addError('Failed to cancel subscription ' + subscription.id, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        return this;
    }]);
});
