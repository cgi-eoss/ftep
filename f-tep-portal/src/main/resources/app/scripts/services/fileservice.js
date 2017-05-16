/**
 * @ngdoc service
 * @name ftepApp.FileService
 * @description
 * # FileService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('FileService', [ 'ftepProperties', '$q', 'MessageService', 'UserService', 'CommunityService', 'traverson', '$rootScope', '$timeout', 'Upload', function (ftepProperties, $q, MessageService, UserService, CommunityService, traverson, $rootScope, $timeout, Upload) {

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
                pollingUrl: undefined,
                pagingData: {},
                files: undefined,
                fileDetails: undefined,
                selectedFile: undefined,
                activeFileType: "REFERENCE_DATA",
                searchText: '',
                displayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnerhipFilter: self.fileOwnershipFilters.ALL_FILES
             }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollFtepFiles = function (page) {
            pollingTimer = $timeout(function () {
                var request = halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pollingUrl = document._links.self.href;
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.ftepfiles', document._embedded.ftepFiles);
                        pollFtepFiles(page);
                     }, function (error) {
                        MessageService.addError('Could not get Files', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollFtepFiles(page);
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.stopPolling = function(){
            if(pollingTimer){
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        /* File types: REFERENCE_DATA, OUTPUT_PRODUCT, EXTERNAL_PRODUCT */
        this.getFtepFiles = function (page, fileType, url) {
            if(url){
                self.params[page].pollingUrl = url;
            }
            else {
                self.params[page].pollingUrl = rootUri + '/ftepFiles/search/findByType' + '?type=' + fileType;
            }

            var deferred = $q.defer();
            var request = /* Get files list */
                halAPI.from(self.params[page].pollingUrl)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                        if (startPolling) {
                            pollFtepFiles(page);
                            startPolling = false;
                        }

                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        deferred.resolve(document._embedded.ftepFiles);
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
                        MessageService.addInfo('File removed', 'File ' + file.name + ' deleted.');
                        resolve(file);
                    } else {
                        MessageService.addError('Could not remove File ' + file.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not remove File ' + file.name, error);
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
                    MessageService.addInfo('File uploaded', 'Success ' + resp.config.data.file.name + ' uploaded.');
                    deferred.resolve(resp);
                }, function (resp) {
                    MessageService.addError('Error uploading File', resp);
                    deferred.reject();
                }, function (evt) {
                    var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
                    console.log('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
                });
            }
            return deferred.promise;
        };

        // For search items we have to create a respective file first
        this.createGeoResultFile = function(item, source){
            var newProductFile = {
                    properties: {
                        productSource: source,
                        productIdentifier: item.identifier,
                        originalUrl: item.link,
                        extraParams: item.details
                    },
                    type: 'Feature',
                    geometry: item.geo
            };

            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/ftepFiles/externalProduct')
                         .newRequest()
                         .post(newProductFile)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        resolve(document);
                    } else {
                        reject();
                    }
                }, function (error) {
                    reject();
                });
            });
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
                        MessageService.addInfo('File successfully updated', 'File ' + file.filename + ' has been updated.');
                        resolve(document);
                    } else {
                        MessageService.addError('Could not update File ' + file.filename, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not update File ' + file.filename, error);
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
                MessageService.addError('Could not get File ' + file.filename, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        /* Fetch a new page */
        this.getFtepFilesPage = function(page, url){
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get files list */
                self.getFtepFiles(page, self.params[page].activeFileType, url).then(function (data) {
                    self.params[page].files = data;
                });

                //the selected file will not exist on the new page
                self.params[page].selectedFile = undefined;
                self.params[page].fileDetails = undefined;
            }
        };

        this.refreshFtepFiles = function (page, action, file) {

            if(self.params[page]){

                self.getFtepFiles(page, self.params[page].activeFileType).then(function (data) {
                    self.params[page].files = data;

                    /* Clear file if deleted */
                    if (action === "Remove") {
                        if (file && file.id === self.params[page].selectedFile.id) {
                            self.params[page].selectedFile = undefined;
                            self.params[page].fileDetails = undefined;
                        }
                    }

                    /* Update the selected file */
                    self.refreshSelectedFtepFile(page);
                });
            }
        };

        this.refreshSelectedFtepFile = function (page) {

            if (self.params[page]) {
                /* Get file contents if selected */
                if (self.params[page].selectedFile) {
                    getFile(self.params[page].selectedFile).then(function (file) {
                        self.params[page].fileDetails = file;
                        CommunityService.getObjectGroups(file, 'ftepFile').then(function (data) {
                            self.params[page].sharedGroups = data;
                        });
                    });
                }
            }

        };

    return this;
  }]);
});
