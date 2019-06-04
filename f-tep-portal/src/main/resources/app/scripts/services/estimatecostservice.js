/**
 * @ngdoc service
 * @name ftepApp.EstimateCostService
 * @description
 * # EstimateCostService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal' ], function(ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('EstimateCostService', ['ftepProperties', '$q', 'traverson', '$mdDialog', 'MessageService', 'CommonService', function(ftepProperties, $q, traverson, $mdDialog, MessageService, CommonService) {

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var rootUri = ftepProperties.URLv2;

        var estimateRecurrenceTypes = {
            'ONE_OFF': 'one-off',
            'MONTHLY': 'monthly',
            'DAILY': 'daily',
            'HOURLY': 'hourly'
        }

        var formatCostRecurrence = function(recurrence, capitalize) {
            var formatted;
            if (!recurrence) {
                formatted = 'one-off'
            } else {
                formatted = estimateRecurrenceTypes[recurrence] || recurrence.toLowerCase()
            }
            if (capitalize) {
                formatted = formatted.charAt(0).toUpperCase() + formatted.slice(1);
            }
            return formatted;
        }

        var getCostMessage = function(estimation) {
            var currency = ( estimation.estimatedCost === 1 ? 'coin' : 'coins' );
            return 'This operation will have '
                + (estimation.isRough ? 'an estimated ' : 'a ')
                + formatCostRecurrence(estimation.recurrence)
                + ' cost of ' + estimation.estimatedCost + ' ' + currency + '.';
        }

        this.showCostDialog = function($event, estimation, onConfirm) {

            if (estimation.hasError) {
                if (estimation.insufficientBalance) {
                    var currency = ( estimation.estimatedCost === 1 ? 'coin' : 'coins' );

                    CommonService.infoBulletin($event,
                        'The cost of this operation exceeds your balance. The operation cannot be run.'
                        + '\nYour balance: ' + estimation.currentWalletBalance
                        + '\n' + formatCostRecurrence(estimation.recurrence, true)
                        + ' cost estimation: ' + estimation.estimatedCost + ' ' + currency
                    );
                } else {
                    CommonService.infoBulletin($event, 'Error retrieving cost estimation. Unable to continue.');
                }
            } else {
                var costMessage = getCostMessage(estimation);
                CommonService.confirm($event, costMessage + '\nAre you sure you want to continue?').then(function (confirmed) {
                    if (confirmed && onConfirm) {
                        onConfirm(costMessage);
                    }
                });
            }
        }

        this.showDownloadDialog = function($event, resource, estimation) {

            var costMessage = getCostMessage(estimation);

            function DownloadController($scope, $mdDialog) {

                $scope.estimation = {
                    message: costMessage,
                    link: resource._links.download.href
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }

            DownloadController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: DownloadController,
                templateUrl: 'views/common/templates/downloadfile.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {}
            });
        }

        var parseEstimationRequestError = function(error) {
            var response
            try {
                response = JSON.parse(error.body);
            } catch(e) {
                response = {
                }
            }
            response.hasError = true;

            if (error.httpStatus === 402) {
                MessageService.addError('Balance exceeded', error);
                response.insufficientBalance = true;
            } else {
                MessageService.addError('Could not get operation cost estimation', error);
            }

            return response;
        }


        this.estimateFileDownload = function(file){
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/estimateCost/download/' + file.id)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    resolve(document);
                 }, function (error) {
                    reject(parseEstimationRequestError(error));
                 });
            });
        };

        this.estimateJob = function(jobConfig){
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/estimateCost/jobConfig/' + jobConfig.id)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                     resolve(document);
                 }, function (error) {
                    reject(parseEstimationRequestError(error));
                 });
            });
        };

        this.estimateJobRerun = function(job){
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/estimateCost/jobRelaunch/' + job.id)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                     resolve(document);
                 }, function (error) {
                    reject(parseEstimationRequestError(error));
                 });
            });
        };

        this.estimateSystematicCost =  function(service, systematicParam, inputParams, searchParams){

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
                    if (200 <= document.status && document.status < 300) {
                        let response = (JSON.parse(document.body));
                        response.isRough = true;
                        resolve(response);
                    } else {
                        reject(parseEstimationRequestError({
                            httpStatus: document.status,
                            body: document.body,
                            data: {
                                errors: document.statusText
                            }
                        }));
                    }
                 }, function (error) {
                    reject(parseEstimationRequestError(error));
                 });
            });
        };

        return this;
    }]);
});