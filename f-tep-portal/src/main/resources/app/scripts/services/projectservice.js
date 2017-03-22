/**
 * @ngdoc service
 * @name ftepApp.ProjectService
 * @description
 * # ProjectService
 * Service in the ftepApp.
 */
define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {
    'use strict';

    ftepmodules.service('ProjectService', [ 'ftepProperties', '$q', 'MessageService', 'traverson', 'UserService',
                                            function (ftepProperties, $q, MessageService, traverson, UserService) {

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var projectsAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

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

        this.createProject = function(project){
            return $q(function(resolve, reject) {
                projectsAPI.from(rootUri + '/projects/')
                         .newRequest()
                         .post(project)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Project created', 'New project '.concat(project.name).concat(' created.'));
                        resolve(JSON.parse(document.data));
                    }
                    else {
                        MessageService.addError ('Could not create a project '.concat(project.name),
                                'Failed to create a project ' + project.name + getMessage(document));
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Could not create a project '.concat(project.name), error.doc.message);
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
                        resolve(document);
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

        this.projectOwnershipFilters = {
                ALL_PROJECTS: { id: 0, name: 'All', criteria: ''},
                MY_PROJECTS: { id: 1, name: 'Mine', criteria: undefined },
                SHARED_PROJECTS: { id: 2, name: 'Shared', criteria: undefined }
        };

        var that = this; //workaround for now
        UserService.getCurrentUser().then(function(currentUser){
            that.projectOwnershipFilters.MY_PROJECTS.criteria = { owner: {name: currentUser.name } };
            that.projectOwnershipFilters.SHARED_PROJECTS.criteria = { owner: {name: "!".concat(currentUser.name) } };
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
                  showProjects: true
              },
        };

        return this;

    }]);
});
