/**
 * @ngdoc service
 * @name ftepApp.JobService
 * @description
 * # JobService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('JobService', [ '$http', 'ftepProperties', '$q', '$timeout', '$rootScope', function ($http, ftepProperties, $q, $timeout, $rootScope) {

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          var is_polling = false;
          var jobListCache;
          var isJobStillRunning = false; //whether a job exists, that is still running
          var connectionError = false, retriesLeft = 3;

          /** Return the jobs for the user **/
          this.getJobs = function(){
              if(is_polling){
                  return $q.when(jobListCache);
              }
              else {
                  return pollJobs();
              }
          }

          /** Polls jobs every 4 sec **/
          var pollJobs = function(){
              if(connectionError && retriesLeft === 0){
                  retriesLeft = 3;
                  connectionError = false;
              }
              else{
                  var deferred = $q.defer();
                  $http.get( ftepProperties.URL + '/jobs?include=service&fields[service]=name').then(function(response) {
                      if(angular.equals(jobListCache, response.data) == false){
                          jobListCache = response.data;
                          $rootScope.$broadcast('refresh.jobs', response.data);
                          findRunningJob();
                      }
                      deferred.resolve(response.data);
                      retriesLeft = 3;
                      connectionError = false;
                  })
                  .catch(function(e){
                      connectionError = true;
                      alert('Could not get jobs. Retries left: ' + retriesLeft );
                      retriesLeft--;
                      deferred.reject();
                  })
                  .finally(function() {
                      if(isJobStillRunning){
                          is_polling = true;
                          $timeout(pollJobs, 4*1000);
                      }
                  });
                  return deferred.promise;
              }
          }

          var findRunningJob = function(){
              var statusList = jobListCache.data.filter(isJobRunning);
              if(statusList.length > 0){
                  isJobStillRunning = true;
              }
              else{
                  isJobStillRunning = false;
              }
          }

          function isJobRunning(item){
              return item.attributes.status === 'Running';
          }

          /** Deletes a job from database **/
          this.removeJob = function(job){
              return $q(function(resolve, reject) {
                  $http({
                      method: 'DELETE',
                      url: ftepProperties.URL + '/jobs/' + job.id
                  }).
                  then(function(response) {
                      resolve(job);
                  }).
                  catch(function(e) {
                      alert('Failed to remove job');
                      console.log(e);
                      reject();
                  });
              });
          }

          /** Gets job output files info **/
          this.getOutputs = function(jobId){
              var deferred = $q.defer();
              $http.get( ftepProperties.URL + '/jobs/' + jobId + '/getOutputs').then(function(response) {
                  deferred.resolve(response.data.data);
              })
              .catch(function(e){
                  alert('Could not get job outputs');
                  deferred.reject();
              });
              return deferred.promise;
          }

          return this;
      }]);
});