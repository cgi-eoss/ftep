/**
 * @ngdoc service
 * @name ftepApp.FileService
 * @description
 * # FileService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('FileService', [ 'ftepProperties', '$q', 'MessageService', 'TabService', 'UserService', 'CommunityService', 'traverson', '$rootScope', '$timeout', function (ftepProperties, $q, MessageService, TabService, UserService, CommunityService, traverson, $rootScope, $timeout) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.fileOwnershipFilters = {
            ALL_FILES: { id: 0, name: 'All', criteria: ''},
            MY_FILES: { id: 1, name: 'Mine', criteria: undefined },
            SHARED_FILES: { id: 2, name: 'Shared', criteria: undefined }
        };

        UserService.getCurrentUser().then(function(currentUser){
            self.fileOwnershipFilters.MY_FILES.criteria = { owner: {name: currentUser.name } };
            self.fileOwnershipFilters.SHARED_FILES.criteria = { owner: {name: "!".concat(currentUser.name) } };
        });

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            community: {
                files: undefined,
                fileDetails: undefined,
                selectedFile: undefined,
                activeFileType: "REFERENCE_DATA",
                searchText: '',
                displayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnerhipFilter: self.fileOwnershipFilters.ALL_FILES,
                showFiles: true
             }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;

        var pollFtepFiles = function () {
            $timeout(function () {
                var request = halAPI.from(rootUri + '/ftepFiles/')
                    .newRequest()
                    .follow('search')
                    .getResource();
                request.result.then(function (document) {
                    request.continue().then(function (nextBuilder) {
                        var nextRequest = nextBuilder.newRequest();
                        nextRequest
                            .follow('findByType')
                            .withRequestOptions({
                                qs: {
                                    type: self.params.community.activeFileType
                                }
                            })
                            .getResource()
                            .result
                            .then(function (document) {
                                $rootScope.$broadcast('poll.ftepfiles', document._embedded.ftepFiles);
                                pollFtepFiles();
                             }, function (error) {
                                MessageService.addError('Could not get Files', error);
                                if (pollCount > 0) {
                                    pollCount -= 1;
                                    pollFtepFiles();
                                }
                            });
                    });
                }, function (error) {
                    MessageService.addError('Could not get Files', error);
                    if (pollCount > 0) {
                        pollCount -= 1;
                        pollFtepFiles();
                    }
                });
            }, POLLING_FREQUENCY);
        };

        /* File types: REFERENCE_DATA, OUTPUT_PRODUCT, EXTERNAL_PRODUCT */
        this.getFtepFiles = function (fileType) {
            var deferred = $q.defer();
            var request = halAPI.from(rootUri + '/ftepFiles/')
                .newRequest()
                .follow('search')
                .getResource();
            request.result.then(function (document) {
                request.continue().then(function (nextBuilder) {
                    var nextRequest = nextBuilder.newRequest();
                    nextRequest
                        .follow('findByType')
                        .withRequestOptions({
                            qs: {
                                type: fileType
                            }
                        })
                        .getResource()
                        .result
                        .then(function (document) {
                            if (startPolling) {
                                pollFtepFiles();
                                startPolling = false;
                            }
                            deferred.resolve(document._embedded.ftepFiles);
                        }, function (error) {
                            MessageService.addError('Could not get Files', error);
                            deferred.reject();
                        });
                });
            }, function (error) {
                MessageService.addError('Could not get Files', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.removeFtepFile = function(file) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/ftepFiles/' + file.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('File removed', 'File '.concat(file.name).concat(' deleted.'));
                        resolve(file);
                    } else {
                        MessageService.addError ('Failed to Remove File', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to Remove File', error);
                    reject();
                });
            });
        };

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
                    MessageService.addInfo('File uploaded', 'Success '.concat(resp.config.data.file.name).concat(' uploaded.'));
                    deferred.resolve(resp);
                }, function (resp) {
                    MessageService.addError('Error in file upload', resp.data ? resp.data : resp);
                    deferred.reject();
                }, function (evt) {
                    var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
                    console.log('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
                });
            }
            return deferred.promise;
        };

        this.updateFtepFile = function (file) {
            var newfile = {name: file.filename, description: file.description, geometry: file.geometry, tags: file.tags};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/ftepFiles/' + file.id)
                         .newRequest()
                         .patch(newfile)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('File successfully updated');
                        resolve(document);
                    } else {
                        MessageService.addError ('Failed to Update File', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Failed to update File', error);
                    reject();
                });
            });
        };

        var getFile = function (file) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/ftepFiles/' + file.id + "?projection=detailedFtepFile")
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError ('Could not get File', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.refreshFtepFiles = function (service, action, file) {

            if(service === "Community") {

                self.getFtepFiles(self.params.community.activeFileType).then(function (data) {
                    self.params.community.files = data;

                    /* Clear file if deleted */
                    if (action === "Remove") {
                        if (file && file.id === self.params.community.selectedFile.id) {
                            self.params.community.selectedFile = undefined;
                            self.params.community.fileDetails = undefined;
                        }
                    }

                    /* Update the selected file */
                    self.refreshSelectedFtepFile("Community");

                });
            }
        };

        this.refreshSelectedFtepFile = function (service) {

            if (service === "Community") {
                /* Get file contents if selected */
                if (self.params.community.selectedFile) {
                    getFile(self.params.community.selectedFile).then(function (file) {
                        self.params.community.fileDetails = file;
                        CommunityService.getObjectGroups(file, 'ftepFile').then(function (data) {
                            self.params.community.sharedGroups = data;
                        });
                    });
                }
            }

        };

    return this;
  }]);
});
