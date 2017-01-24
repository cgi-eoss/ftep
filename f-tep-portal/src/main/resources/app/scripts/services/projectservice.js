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

        var messages = [
          {
              status: "Error",
              title: "Could not get projects",
              description: "Could not get projects"

          },
          {
              status: "Error",
              title: "Could not create project",
              description: "Could not create project: conflicts with an already existing one"

          },
          {
              status: "Error",
              title: "Failed to update project",
              description: "Failed to update project"

          },
        ];

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          this.getProjects = function(){
              var deferred = $q.defer();
              $http.get( ftepProperties.URL + '/projects').then(function(response) {
                  deferred.resolve(response.result.data);
              })
              .catch(function(e){
                  MessageService.addMessage(
                      'Error',
                      'Could not get projects',
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
                      resolve(response.result.data);
                  }).
                  catch(function(e) {
                      if(e.status == 409){
                          MessageService.addMessage(
                              'Error',
                              'Could not create project',
                              'Could not create project: conflicts with an already existing one'
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
                  }).
                  catch(function(e) {
                      MessageService.addMessage(
                          'Error',
                          'Failed to remove project',
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
                      resolve(response.result.data.data);
                  }).
                  catch(function(e) {
                      MessageService.addMessage(
                          'Error',
                          'Failed to update project',
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
