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

        var that = this;

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
            that.fileOwnershipFilters.MY_FILES.criteria = { owner: {name: currentUser.name } };
            that.fileOwnershipFilters.SHARED_FILES.criteria = { owner: {name: "!".concat(currentUser.name) } };
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
                selectedOwnerhipFilter: that.fileOwnershipFilters.ALL_FILES,
                showFiles: true
             }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;

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
                                    type: that.params.community.activeFileType
                                }
                            })
                            .getResource()
                            .result
                            .then(function (document) {
                                $rootScope.$broadcast('poll.ftepfiles', document._embedded.ftepFiles);
                                if (TabService.startPolling().files) {
                                    pollFtepFiles();
                                }
                            }, function (error) {
                                MessageService.addError('Could not get Files', error);
                                pollCount -= 1;
                                if (pollCount >= 0 && TabService.startPolling().files) {
                                    pollFtepFiles();
                                }
                            });
                    });
                }, function (error) {
                    MessageService.addError('Could not get Files', error);
                    pollCount -= 1;
                    if (pollCount >= 0 && TabService.startPolling().files) {
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
                            if (TabService.startPolling().files) {
                                pollFtepFiles();
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

        this.updateFtepFile = function (file) {
            var newfile = {name: file.name, description: file.description, geometry: file.geometry, tags: file.tags};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/ftepFiles/' + file.id)
                         .newRequest()
                         .patch(newfile)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('File successfully updated');
                    resolve(document);
                }, function (error) {
                    MessageService.addError('Failed to update File', error);
                    reject();
                });
            });
        };

        var getFileV2 = function (file) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/ftepFiles/' + file.id)
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

        this.refreshFtepFilesV2 = function (service, action, file) {

            if(service === "Community") {

                that.getFtepFiles(that.params.community.activeFileType).then(function (data) {
                    that.params.community.files = data;

                    /* Clear file if deleted */
                    if (action === "Remove") {
                        if (file && file.id === that.params.community.selectedFile.id) {
                            that.params.community.selectedFile = undefined;
                            that.params.community.fileDetails = undefined;
                        }
                    }

                    /* Update the selected file */
                    that.refreshSelectedFtepFileV2("Community");

                });
            }
        };

        this.refreshSelectedFtepFileV2 = function (service) {

            if (service === "Community") {
                /* Get file contents if selected */
                if (that.params.community.selectedFile) {
                    getFileV2(that.params.community.selectedFile).then(function (file) {
                        that.params.community.fileDetails = file;
                        CommunityService.getObjectGroups(file, 'ftepFile').then(function (data) {
                            that.params.community.sharedGroups = data;
                        });
                    });
                }
            }

        };

    return this;
  }]);
});
