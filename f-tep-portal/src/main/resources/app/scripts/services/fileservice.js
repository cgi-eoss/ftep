/**
 * @ngdoc service
 * @name ftepApp.FileService
 * @description
 * # FileService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('FileService', [ 'ftepProperties', '$q', 'MessageService', 'traverson', function (ftepProperties, $q, MessageService, traverson) {

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        /** PRESERVE USER SELECTIONS **/
        this.params = {

        };
        /** END OF PRESERVE USER SELECTIONS **/

        /* File types: REFERENCE_DATA, OUTPUT_PRODUCT, EXTERNAL_PRODUCT */
         this.getFtepFiles = function (filetype) {
            var deferred = $q.defer();
            var request = halAPI.from(rootUri + '/ftepFiles/')
                          .newRequest()
                          .follow('search')
                          .getResource();

            request.result.then(function (document) {
            }, function (error) {
                MessageService.addError ('Could not get Files', error);
                deferred.reject();
            });

            request.continue().then(function(request) {
                request
                .follow('findByType')
                .withRequestOptions({
                    qs: { type: filetype }
                })
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document._embedded.ftepFiles);
                }, function (error) {
                    MessageService.addError ('Could not get Files', error);
                    deferred.reject();
                });
            });

            return deferred.promise;
        };

    return this;
  }]);
});
