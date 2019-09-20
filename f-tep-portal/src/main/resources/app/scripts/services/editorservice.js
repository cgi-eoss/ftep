/**
 * @ngdoc service
 * @name ftepApp.EditorService
 * @description
 * # EditorService
 * Service in the ftepApp.
 */
'use strict';
define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('EditorService', ['MessageService', '$http', 'ftepProperties', '$q', 'traverson', function (MessageService, $http, ftepProperties, $q, traverson) {

        var self = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();

        this.getFileList =  function(files)  {
            var filename;
            var list = [];
            for (var file in files) {
                var indent = 0;
                filename = files[file].filename;
                while(filename.indexOf('/') !== -1) {
                    var folderexists = false;
                    for(var i=0; i < list.length; i++) {
                        if(list[i].name.indexOf(filename.slice(0, filename.indexOf('/')))  !== -1) {
                            folderexists = true;
                        }
                    }
                    if(!folderexists) {
                        list.push({name: filename.slice(0, filename.indexOf('/')), type: 'folder', indent: indent});
                    }
                    filename = filename.substring(filename.indexOf('/') + 1);
                    indent++;
                }
                list.push({name: filename, type: 'file', indent: indent, contents: files[file]});
            }

            var previousIndent = 0;
            var nextIndent;
            for(var item = 0; item < list.length; item++) {
                var currentIndent = list[item].indent;

                if(list.length > item + 1) {
                    nextIndent = list[item + 1].indent;
                } else {
                    nextIndent = 'end';
                }

                if(nextIndent === 'end' && currentIndent === 0) {
                    list[item].tree = '└─';
                } else if(currentIndent === 0) {
                    list[item].tree='├';
                } else {
                    list[item].tree='│';
                    for(var j = 0; j < currentIndent; j++) {
                        if (j < currentIndent -1) {
                            list[item].tree = list[item].tree + '...';
                            if(currentIndent > 0) {
                                list[item].tree = list[item].tree + '│';  //Needs forward logic to check if │ or ...
                            }
                        } else {
                            list[item].tree = list[item].tree + '...';
                            if(nextIndent === 'end') {
                                list[item].tree = list[item].tree + '└─';
                            } else if(currentIndent === nextIndent) {
                                list[item].tree = list[item].tree + '├─';
                            } else if(currentIndent < nextIndent) {
                                list[item].tree = list[item].tree + '├─'; //Needs forward logic to check if ├─ or └─
                            } else if(currentIndent > nextIndent) {
                                list[item].tree = list[item].tree + '└─';
                            }
                        }
                    }
                }
                previousIndent = currentIndent;
            }

            return list;
        };

        this.setFileType = function (filename) {
            var extension = filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2).toLowerCase();
            var modes = ['Text', 'Dockerfile', 'Javascript', 'Perl', 'PHP', 'Python', 'Properties', 'Shell', 'XML', 'YAML' ];

            if (filename === 'Dockerfile') {
                return modes[1];
            } else {
                switch(extension) {
                    case 'js':
                        return modes[2];
                    case 'pl':
                        return modes[3];
                    case 'php':
                        return modes[4];
                    case 'py':
                        return modes[5];
                    case 'properties':
                        return modes[6];
                    case 'sh':
                        return modes[7];
                    case 'xml':
                        return modes[8];
                    case 'yml':
                        return modes[9];
                    default:
                        return modes[0];
                }
            }
        };

        this.addDefaultFiles = function(service) {
            $http.get('scripts/templates/Dockerfile')
                .success(function(dockerFile) {
                    self.addFile({
                        filename: 'Dockerfile',
                        content: btoa(dockerFile),
                        service: service._links.self.href
                    }, '/serviceFiles/', 'Service');
                    $http.get('scripts/templates/workflow.sh')
                        .success(function(workflowFile) {
                            self.addFile({
                                filename: 'workflow.sh',
                                content: btoa(workflowFile),
                                service: service._links.self.href,
                                executable: true
                            }, '/serviceFiles/', 'Service');
                        })
                        .error(function(error) {
                            MessageService.addError('Could not get workflow.sh Template', error);
                        });
                })
                .error(function(error) {
                    MessageService.addError('Could not get Docker Template', error);
                });
        };

        this.addFile = function(file, url, type){
            var deferred = $q.defer();
            halAPI.from(rootUri + url)
                .newRequest()
                .post(file)
                .result
                .then(
                    function (result) {
                        MessageService.addInfo(type + ' File added', file.filename + ' added');
                        deferred.resolve(JSON.parse(result.data));
                    }, function (error) {
                        MessageService.addError('Could not add ' + type + ' File ' + file.filename, error);
                        deferred.reject();
                    });
            return deferred.promise;
        };

        function updateFile(file) {
            var deferred = $q.defer();
            var editedFile = angular.copy(file);
            editedFile.content =  btoa(file.content);
            halAPI.from(file._links.self.href)
                .newRequest()
                .patch(editedFile)
                .result
                .then(
                    function (result) {
                        deferred.resolve();
                    }, function (error) {
                        MessageService.addError('Could not update File ' + file.name, error);
                        deferred.reject();
                    }
                );
            return deferred.promise;
        }

        this.saveFiles = function(fileContainer, containerType) {
            if(fileContainer.files) {
                var promises = [];
                for(var i = 0; i < fileContainer.files.length; i++){
                    var partialPromise = updateFile(fileContainer.files[i]);
                    promises.push(partialPromise);
                }
                $q.all(promises).then(function(){
                    MessageService.addInfo(containerType + ' updated', containerType + ' ' + fileContainer.name + ' successfully updated');
                });
            }
        };

        this.getFileDetails = function(page, file, container) {
            var deferred = $q.defer();
            halAPI.from(file._links.self.href)
                .newRequest()
                .getResource()
                .result
                .then(
                    function (document) {
                        if (container && !container.files) {
                            container.files = [];
                        }
                        container.files.push(document);

                        deferred.resolve();
                    }, function (error) {
                        MessageService.addError('Could not get Service File details', error);
                        deferred.reject();
                    }
                );
            return deferred.promise;
        };

        this.sortFiles = function(a, b) {
            if (a.filename < b.filename) {
                return -1;
            }
            if (a.filename > b.filename) {
                return 1;
            }
            return 0;
        };

        return this;

    }]);
});
