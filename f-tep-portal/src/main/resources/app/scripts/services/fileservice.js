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
                ALL_FILES: { id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'},
                MY_FILES: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner' },
                SHARED_FILES: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner' }
        };

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
                selectedOwnershipFilter: self.fileOwnershipFilters.ALL_FILES,
                progressPercentage: 0,
                uploadStatus: 'pending',
                uploadMessage: undefined
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

        this.uploadFile = function (page, newReference) {
            self.params[page].uploadStatus = "pending";
            self.params[page].uploadMessage = undefined;
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
                    self.params[page].uploadStatus = "complete";
                    self.params[page].uploadMessage = "resp.config.data.file.name uploaded successfully";
                    deferred.resolve(resp);
                }, function (resp) {
                    MessageService.addError('Error uploading File', resp.data);
                    self.params[page].uploadStatus = "failed";
                    self.params[page].uploadMessage = resp.data ? resp.data : "An undefined error occured";
                    deferred.reject();
                }, function (evt) {
                    self.params[page].progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
                });
            }
            return deferred.promise;
        };

        // For search items we have to create a respective file first
        this.createGeoResultFile = function(item){

            var newProductFile = {
                properties: {
                    productSource: item.properties.productSource,
                    productIdentifier: item.properties.productIdentifier,
                    originalUrl: item.properties._links.ftep.href,
                    extraParams: item.properties.extraParams
                },
                type: item.type,
                geometry: item.geometry
            };

            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/ftepFiles/externalProduct')
                         .newRequest()
                         .post(newProductFile)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        resolve(JSON.parse(document.data));
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

        this.getFile = function (file) {
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

        this.getFtepFilesByFilter = function (page) {
            if (self.params[page]) {
                var url = rootUri + '/ftepFiles/' + self.params[page].selectedOwnershipFilter.searchUrl +
                    '?sort=filename&filter=' + (self.params[page].searchText ? self.params[page].searchText : '') +
                    '&type=' + self.params[page].activeFileType;

                if(self.params[page].selectedOwnershipFilter !== self.fileOwnershipFilters.ALL_FILES){
                    url += '&owner=' + UserService.params.activeUser._links.self.href;
                }

                /* Get databasket list */
                self.getFtepFiles(page, self.params[page].activeFileType, url).then(function (data) {
                    self.params[page].files = data;
                });
            }
        };

        this.refreshFtepFiles = function (page, action, file) {
            if(self.params[page]){

                self.getFtepFiles(page, self.params[page].activeFileType).then(function (data) {
                    self.params[page].files = data;

                    /* Clear file if deleted */
                    if (action === "Remove" && self.params[page].selectedFile) {
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
                    self.getFile(self.params[page].selectedFile).then(function (file) {
                        self.params[page].fileDetails = file;
                        if(file.access.currentLevel === 'ADMIN') {
                            CommunityService.getObjectGroups(file, 'ftepFile').then(function (data) {
                                self.params[page].sharedGroups = data;
                            });
                        }
                    });
                }
            }

        };

        this.estimateFileDownload = function(file){
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/estimateCost/download/' + file.id)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                     resolve(document);
                 }, function (error) {
                    if (error.httpStatus === 402) {
                        MessageService.addError('Balance exceeded', error);
                    } else {
                        MessageService.addError('Could not get download cost estimation', error);
                    }
                    reject(JSON.parse(error.body));
                 });
            });
        };

    return this;
  }]);
});
