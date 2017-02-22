/**
 * @ngdoc service
 * @name ftepApp.ProjectService
 * @description
 * # ProjectService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('ProjectService', [ '$http', 'ftepProperties', '$q', 'MessageService', function ($http, ftepProperties, $q, MessageService) {

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          this.getProjects = function(){
              var deferred = $q.defer();
              $http.get( ftepProperties.URL + '/projects').then(function(response) {
                  deferred.resolve(response.data.data);
              })
              .catch(function(e){
                  MessageService.addError(
                      'Could not get projects'
                  );
                  deferred.reject();
              });
              return deferred.promise;
          };

          this.createProject = function(name, desc){
              return $q(function(resolve, reject) {

                  var project = {type: 'projects', attributes:{name: name, description: (desc ? desc : '')}};
                  $http({
                      method: 'POST',
                      url: ftepProperties.URL + '/projects',
                      data: '{"data": ' + JSON.stringify(project) + '}',
                  }).
                  then(function(response) {
                      resolve(response.data.data);
                      MessageService.addInfo('Project created', 'New project '.concat(name).concat(' created'));
                  }).
                  catch(function(e) {
                      if(e.status == 409){
                          MessageService.addError(
                              'Could not create project',
                              'Conflicts with an already existing one'
                          );
                      }
                      reject();
                  });
              });
          };

          this.removeProject = function(project){
              return $q(function(resolve, reject) {
                  $http({
                      method: 'DELETE',
                      url: ftepProperties.URL + '/projects/' + project.id
                  }).
                  then(function(response) {
                      resolve(project);
                      MessageService.addInfo('Project deleted', 'Project '.concat(project.attributes.name).concat(' deleted'));
                  }).
                  catch(function(e) {
                      MessageService.addError(
                          'Failed to remove project'
                      );
                      console.log(e);
                      reject();
                  });
              });
          };

          this.updateProject = function(project){
              return $q(function(resolve, reject) {
                  $http({
                      method: 'PATCH',
                      url: ftepProperties.URL + '/projects/' + project.id,
                      data: '{"data": ' + JSON.stringify(project) + '}',
                  }).
                  then(function(response) {
                      resolve(response.data.data);
                      MessageService.addInfo('Project updated', 'Project '.concat(project.attributes.name).concat(' updated'));
                  }).
                  catch(function(e) {
                      MessageService.addError(
                          'Failed to update project'
                      );
                      console.log(e);
                      reject();
                  });
              });
          };

          return this;
      }]);
});
