/**
 * @ngdoc service
 * @name ftepApp.ProjectService
 * @description
 * # ProjectService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('ProjectService', [ 'ftepProperties', 'MessageService', 'CommunityService', 'UserService', 'traverson', '$q', '$rootScope', '$timeout', function (ftepProperties, MessageService, CommunityService, UserService, traverson, $q, $rootScope, $timeout) {

        var self = this; //workaround for now
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.projectOwnershipFilters = {
                ALL_PROJECTS: { id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'},
                MY_PROJECTS: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner' },
                SHARED_PROJECTS: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner' }
        };

        var userUrl;
        UserService.getCurrentUser().then(function(currentUser){
            userUrl = currentUser._links.self.href;
        });

        /** PRESERVE USER SELECTIONS **/
        this.params = {
              explorer: {
                  projects: undefined,
                  pollingUrl: rootUri + '/projects/?sort=name',
                  pagingData: {},
                  selectedProject: undefined,
                  displayFilters: false,
                  searchText: undefined,
                  selectedOwnershipFilter: self.projectOwnershipFilters.ALL_PROJECTS
              },
              community: {
                  projects: undefined,
                  contents: undefined,
                  selectedProject: undefined,
                  pollingUrl: rootUri + '/projects/?sort=name',
                  pagingData: {},
                  searchText: '',
                  displayFilters: false,
                  contentsSearchText: '',
                  contentsDisplayFilters: false,
                  sharedGroups: undefined,
                  sharedGroupsSearchText: '',
                  sharedGroupsDisplayFilters: false,
                  selectedOwnershipFilter: self.projectOwnershipFilters.ALL_PROJECTS
              },
        };

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollProjects = function (page) {
            pollingTimer = $timeout(function () {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.projects', document._embedded.projects);
                        pollProjects(page);
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Projects', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollProjects(page);
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

        function getProjects(page){
            var deferred = $q.defer();
            halAPI.from(self.params[page].pollingUrl)
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                if (startPolling) {
                    pollProjects(page);
                    startPolling = false;
                }
                self.params[page].pagingData._links = document._links;
                self.params[page].pagingData.page = document.page;

                deferred.resolve(document._embedded.projects);
            }, function (error) {
                MessageService.addError('Could not get Projects', error);
                deferred.reject();
            });
            return deferred.promise;
        }

        function getMessage(document){
            var message = '';
            if(document.data && document.data.indexOf('message') > 0){
                message = ': ' + JSON.parse(document.data).message;
            }
            return message;
        }

        this.createProject = function(name, description){
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/projects/')
                         .newRequest()
                         .post({name: name, description: description})
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Project created', 'New project ' + name + (' created.'));
                        resolve(JSON.parse(document.data));
                    }
                    else {
                        MessageService.addError('Could not create a Project ' + name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not create a Project ' + name, error);
                    reject();
                });
            });
        };

        this.updateProject = function(project){
            var editProject = {name: project.name, description: project.description};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/projects/' + project.id)
                         .newRequest()
                         .patch(editProject)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Project updated', 'Project ' + project.name + ' successfully updated');
                        resolve(JSON.parse(document.data));
                    }
                    else {
                        MessageService.addError('Could not update Project ' + project.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not update Project ' + project.name, error);
                    reject();
                });
            });
        };

        this.removeProject = function(project){
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/projects/' + project.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Project deleted', 'Project ' + project.name + ' deleted.');
                        resolve(project);
                    } else {
                        MessageService.addError('Could not remove Project ' + project.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not remove Project ' + project.name, error);
                    reject();
                });
            });
        };

        var getProject = function(project){
            var deferred = $q.defer();
            halAPI.from(rootUri + '/projects/' + project.id + "?projection=detailedProject")
                       .newRequest()
                       .getResource()
                       .result
                       .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError('Could not get Project contents', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        /* Fetch a new page */
        this.getProjectsPage = function(page, url){
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get projects list */
                getProjects(page).then(function (data) {
                    self.params[page].projects = data;
                });
            }
        };

        this.getProjectsByFilter = function (page) {
            if (self.params[page]) {
                var url = rootUri + '/projects/' + self.params[page].selectedOwnershipFilter.searchUrl +
                    '?sort=name&filter=' + (self.params[page].searchText ? self.params[page].searchText : '');

                if(self.params[page].selectedOwnershipFilter !== self.projectOwnershipFilters.ALL_PROJECTS){
                    url += '&owner=' + userUrl;
                }
                self.params[page].pollingUrl = url;

                /* Get projects list */
                getProjects(page).then(function (data) {
                    self.params[page].projects = data;
                });
            }
        };

        this.refreshProjects = function (page, action, project) {

            if (self.params[page]) {

                /* Get project list */
                getProjects(page).then(function (data) {

                    self.params[page].projects = data;

                    /* Select last project if created */
                    if (action === "Create") {
                        self.params[page].selectedProject = self.params[page].projects[self.params[page].projects.length-1];
                    }

                    /* Clear project if deleted */
                    if (action === "Remove") {
                        if (project && project.id === self.params[page].selectedProject.id) {
                            self.params[page].selectedProject = undefined;
                            self.params[page].contents = [];
                        }
                    }

                    /* Update the selected group */
                    self.refreshSelectedProject(page);
                });
            }

        };

        this.refreshSelectedProject = function (page) {

            if (self.params[page]) {
                if(page === 'explorer' && self.params[page].selectedProject === undefined && self.params[page].projects){
                    self.params[page].selectedProject = self.params[page].projects[0];
                }

                /* Get project contents if selected */
                if (self.params[page].selectedProject) {
                    getProject(self.params[page].selectedProject).then(function (project) {
                        self.params[page].selectedProject = project;
                        if(project.access.currentLevel === 'ADMIN') {
                            CommunityService.getObjectGroups(project, 'project').then(function (data) {
                                self.params[page].sharedGroups = data;
                            });
                        }
                    });
                }
            }

        };

        return this;

    }]);
});
