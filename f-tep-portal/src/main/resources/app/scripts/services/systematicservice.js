/**
 * @ngdoc service
 * @name ftepApp.SystematicService
 * @description
 * # SystematicService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('SystematicService', [ 'ftepProperties', '$q', '$timeout', '$rootScope', 'MessageService', 'CommonService', 'UserService', 'CommunityService', 'traverson', function (ftepProperties, $q, $timeout, $rootScope, MessageService, CommonService, UserService, CommunityService, traverson) {

        var rootUri = ftepProperties.URLv2;
        var _this = this;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();

        this.estimateMonthlyCost =  function(service, systematicParam, inputParams, searchParams) {
            var params = [];
            for (var key in searchParams) {
                params.push(key + '=' + searchParams[key]);
            }

            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/estimateCost/systematic?' + params.join('&'))
                .newRequest()
                .post({
                    service: {
                        id: service.id,
                        name: service.name,
                        owner: service.owner,
                        type: service.type,
                        _links: service._links,
                        access: service.access,
                        description: service.description,
                        dockerTag: service.dockerTag,
                        status: service.status,
                        licence: service.licence
                    },
                    inputs: inputParams,
                    systematicParameter: systematicParam
                })
                .result
                .then(function (document) {
                    resolve(JSON.parse(document.body));
                 }, function (error) {
                    if (error.httpStatus === 402) {
                        MessageService.addError('Balance exceeded', error);
                    } else {
                        MessageService.addError('Could not get Job cost estimation', error);
                    }
                    reject(JSON.parse(error.body));
                 });
            });
        };

        this.launchSystematicProcessing = function(service, systematicParam, inputParams, searchParams, label) {
            var params = [];
            for (var key in searchParams) {
                params.push(key + '=' + searchParams[key]);
            }
            return $q(function(resolve, reject) {
                    halAPI.from(rootUri + '/jobConfigs/launchSystematic?' + params.join('&'))
                    .newRequest()
                    .post({
                        service: {
                            id: service.id,
                            name: service.name,
                            owner: service.owner,
                            type: service.type,
                            _links: service._links,
                            access: service.access,
                            description: service.description,
                            dockerTag: service.dockerTag,
                            status: service.status,
                            licence: service.licence
                        },
                        inputs: inputParams,
                        systematicParameter: systematicParam,
                        label: label
                    })
                    .result
                    .then(
                 function (document) {
                     resolve(JSON.parse(document.body));
                 }, function (error) {
                     MessageService.addError('Could not create systematic job', error);
                     reject();
                 });
            });
        };
        return this;
    }]);
});
