/**
 * @ngdoc service
 * @name ftepApp.ReferenceService
 * @description
 * # ReferenceService
 * Service in the ftepApp.
 */
'use strict';
define(['../ftepmodules'], function (ftepmodules) {

    ftepmodules.service('ReferenceService', ['ftepProperties', 'MessageService', 'Upload', '$q', function (ftepProperties, MessageService, Upload, $q) {

        this.uploadFile = function (newReference) {
            var deferred = $q.defer();
            var file = newReference.file;
            if (!file.$error) {
                Upload.upload({
                    url: ftepProperties.URLv2 + '/ftepFiles/refData',
                    data: {
                        file: file,
                        geometry: newReference.geometry
                    }
                }).then(function (resp) {
                    MessageService.addInfo('File uploaded', 'Success '.concat(resp.config.data.file.name).concat(' uploaded. Response: ').concat(resp.data));
                    deferred.resolve(resp);
                }, function (resp) {
                    if (resp.data) {
                        MessageService.addError('Error in file upload', resp.data.message);
                    } else {
                        MessageService.addError('Error in file upload', resp);
                    }
                    deferred.reject();
                }, function (evt) {
                    var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
                    console.log('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
                });
            }
            return deferred.promise;
        };

        return this;
    }]);

});
