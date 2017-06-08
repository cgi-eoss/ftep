/**
 * @ngdoc service
 * @name ftepApp.PublishingService
 * @description
 * # PublishingService
 * Service in the ftepApp.
 */
'use strict';
define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('PublishingService', [ 'MessageService', 'ftepProperties', '$q', 'traverson', '$mdDialog', function (MessageService, ftepProperties, $q, traverson, $mdDialog) {

        var _this = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.publishItem = function (item, action, type) {
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/contentAuthority/services/' + action + '/' + item.id)
                    .newRequest()
                    .post(item)
                    .result
                    .then(
                    function (document) {
                        MessageService.addInfo(type +  ' has been published', item.name + ' has been successfully published.');
                        resolve(document);
                    }, function (error) {
                        MessageService.addError('Could not publish ' + item.name, error);
                        reject();
                    }
                );
            });
        };

        this.requestPublication = function (item, type) {
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/publishingRequests/requestPublishService/' + item.id)
                    .newRequest()
                    .post()
                    .result
                    .then(
                    function (document) {
                        MessageService.addInfo(type +  ' publication requested', item.name + ' has been successfully requested to be made public.');
                        resolve(document);
                    }, function (error) {
                        MessageService.addError('Could not make publication request', error);
                        reject();
                    }
                );
            });
        };

        this.publishItemDialog = function ($event, service) {
            function PublishItemController($scope, $mdDialog, ProductService, PublishingService) {

                /* TODO: Uncomment statuses once backend implemented */
                $scope.statusResponses = {
                    GRANTED: { text: "Grant Publication", value:"publish" }/*,
                    NEEDS_INFO: { text: "Needs Info", value:"needs_info" },
                    REJECTED: { text: "Reject Publication", value:"reject" }*/
                };

                $scope.action = {};
                $scope.action.value = $scope.statusResponses.GRANTED.value;

                $scope.respond = function() {
                    PublishingService.publishItem(service, $scope.action.value, 'Service').then(function (data) {
                        ProductService.refreshServices("community");
                        $mdDialog.hide();
                    });
                };

                 $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }
            PublishItemController.$inject = ['$scope', '$mdDialog', 'ProductService', 'PublishingService'];
            $mdDialog.show({
                controller: PublishItemController,
                templateUrl: 'views/community/manage/templates/publishresponse.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {}
            });
        };

        return this;
    }]);
});
