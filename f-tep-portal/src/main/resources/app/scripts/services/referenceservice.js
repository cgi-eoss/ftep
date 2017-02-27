/**
 * @ngdoc service
 * @name ftepApp.ReferenceService
 * @description
 * # ReferenceService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('ReferenceService', [ 'ftepProperties', 'MessageService', 'Upload',
                                              function (ftepProperties, MessageService, Upload) {

        this.uploadFile = function(newReference){
          var file = newReference.file;
          if (!file.$error) {
            Upload.upload({
                url: ftepProperties.URLv2 + '/ftepFiles/refData/new',
                data: {
                  file: file,
                  geometry: newReference.geometry
                }
            }).then(function (result) {
                MessageService.addError('Error in file upload', 'Failed to upload the file');
            });
          }
        };

        return this;
    }]);

});

