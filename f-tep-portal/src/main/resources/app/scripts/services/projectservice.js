/**
 * @ngdoc service
 * @name ftepApp.ProjectService
 * @description
 * # ProjectService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('ProjectService', [ 'ftepProperties', 'MessageService', 'CommunityService', 'UserService', 'traverson', '$q', function (ftepProperties, MessageService, CommunityService, UserService, traverson, $q) {

        var self = this; //workaround for now
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var projectsAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.projectOwnershipFilters = {
                ALL_PROJECTS: { id: 0, name: 'All', criteria: ''},
                MY_PROJECTS: { id: 1, name: 'Mine', criteria: undefined },
                SHARED_PROJECTS: { id: 2, name: 'Shared', criteria: undefined }
        };

        UserService.getCurrentUser().then(function(currentUser){
            self.projectOwnershipFilters.MY_PROJECTS.criteria = { owner: {name: currentUser.name } };
            self.projectOwnershipFilters.SHARED_PROJECTS.criteria = { owner: {name: "!".concat(currentUser.name) } };
        });

        /** PRESERVE USER SELECTIONS **/
        this.params = {
              explorer: {
                  showProjects: true,
                  activeProject: undefined,
                  displayFilters: false,
                  searchText: undefined,
                  selectedOwnerhipFilter: this.projectOwnershipFilters.ALL_PROJECTS
              },
              community: {
                  projects: undefined,
                  contents: undefined,
                  selectedProject: undefined,
                  searchText: '',
                  displayFilters: false,
                  contentsSearchText: '',
                  contentsDisplayFilters: false,
                  sharedGroups: undefined,
                  sharedGroupsSearchText: '',
                  sharedGroupsDisplayFilters: false,
                  selectedOwnershipFilter: self.projectOwnershipFilters.ALL_PROJECTS,
                  showProjects: true
              },
        };

        this.getProjects = function(){
            var deferred = $q.defer();
            projectsAPI.from(rootUri + '/projects/')
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document._embedded.projects);
            }, function (error) {
                MessageService.addError ('Could not get projects',
                        'Failed to get projects' + (error.doc.message ? ': ' + error.doc.message : '' ));
                deferred.reject();
            });
            return deferred.promise;
        };

        function getMessage(document){
            var message = '';
            if(document.data && document.data.indexOf('message') > 0){
                message = ': ' + JSON.parse(document.data).message;
            }
            return message;
        }

        this.createProject = function(name, description){
            return $q(function(resolve, reject) {
                projectsAPI.from(rootUri + '/projects/')
                         .newRequest()
                         .post({name: name, description: description})
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Project created', 'New project '.concat(name).concat(' created.'));
                        resolve(JSON.parse(document.data));
                    }
                    else {
                        MessageService.addError ('Could not create a project '.concat(name),
                                'Failed to create a project ' + name + getMessage(document));
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Could not create a project '.concat(name), error.doc.message);
                    reject();
                });
            });
        };

        this.updateProject = function(project){
            var editProject = {name: project.name, description: project.description};
            return $q(function(resolve, reject) {
                projectsAPI.from(rootUri + '/projects/' + project.id)
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
                        MessageService.addError('Could not update project '.concat(project.name),
                                'Failed to update project ' + project.name + getMessage(document));
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Failed to update project '.concat(project.name), error.doc.message);
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
                        MessageService.addInfo('Project deleted', 'Project '.concat(project.name).concat(' deleted.'));
                        resolve(project);
                    } else {
                        MessageService.addError ('Could not remove project '.concat(project.name), getMessage(document));
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Could not remove project '.concat(project.name), error.doc.message);
                    reject();
                });
            });
        };

        var getProject = function(project){
            var deferred = $q.defer();
            projectsAPI.from(rootUri + '/projects/' + project.id)
                       .newRequest()
                       .getResource()
                       .result
                       .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError ('Could not get Project contents', error );
                deferred.reject();
            });
            return deferred.promise;
        };

        this.refreshProjects = function (service, action, project) {

            if (service === "Community") {

                /* Get project list */
                this.getProjects().then(function (data) {

                    self.params.community.projects = data;

                    /* Select last project if created */
                    if (action === "Create") {
                        self.params.community.selectedProject = self.params.community.projects[self.params.community.projects.length-1];
                    }

                    /* Clear project if deleted */
                    if (action === "Remove") {
                        if (project && project.id === self.params.community.selectedProject.id) {
                            self.params.community.selectedProject = undefined;
                            self.params.community.contents = [];
                        }
                    }

                    /* Update the selected group */
                    self.refreshSelectedProject("Community");

                });

            }

        };

        this.refreshSelectedProject = function (service) {

            if (service === "Community") {
                /* Get project contents if selected */
                if (self.params.community.selectedProject) {
                    getProject(self.params.community.selectedProject).then(function (project) {
                        self.params.community.selectedProject = project;
                        CommunityService.getObjectGroups(project, 'project').then(function (data) {
                            self.params.community.sharedGroups = data;
                        });
                    });
                }
            }

        };

        return this;

    }]);
});
