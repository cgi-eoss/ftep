/**
 * @ngdoc service
 * @name ftepApp.JobService
 * @description
 * # JobService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('JobService', [ '$http', 'ftepProperties', '$q', '$timeout', '$rootScope', 'MessageService', function ($http, ftepProperties, $q, $timeout, $rootScope, MessageService) {

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          var POLLING_FREQUENCY = 4 * 1000;
          var isPolling = false;
          var jobListCache;
          var isJobStillRunning = false; //whether a job exists, that is still running
          var connectionError = false, retriesLeft = 3;
          var waitingForNewJob = false;

          /** Return the jobs for the user **/
          this.getJobs = function(newJobWasPushed) {
              if(newJobWasPushed){
                  waitingForNewJob = newJobWasPushed;
              }

              if(isPolling){
                  return $q.when(jobListCache);
              } else {
                  return pollJobs();
              }
          };

          /** Polls jobs every 4 sec **/
          var pollJobs = function () {

              if (connectionError && retriesLeft === 0) {
                  retriesLeft = 3;
                  connectionError = false;
              } else {
                  var deferred = $q.defer();
                  $http.get(ftepProperties.URL + '/jobs?include=service&fields[service]=name').then(function (response) {
                          if (angular.equals(jobListCache, response.result) == false) {
                              waitingForNewJob = ((jobListCache != undefined) && (jobListCache.meta.total <= response.result.meta.total));
                              jobListCache = response.result;
                              $rootScope.$broadcast('refresh.jobs', response.result);
                              findRunningJob();
                          }
                          deferred.resolve(response.result);
                          retriesLeft = 3;
                          connectionError = false;
                      })
                      .catch(function (e) {
                          connectionError = true;
                          MessageService.addMessage(
                              'Error',
                              'Could not get jobs',
                              'Could not get jobs. Retries left: ' + retriesLeft
                          );
                          retriesLeft--;
                          deferred.reject();
                      })
                      .finally(function () {
                          if (isJobStillRunning === true || waitingForNewJob === true) {
                              isPolling = true;
                              $timeout(pollJobs, POLLING_FREQUENCY);
                          }
                          else {
                              isPolling = false;
                          }
                      });

                  return deferred.promise;
              }
          };

          var findRunningJob = function () {
              var statusList = jobListCache.data.filter(isJobRunning);
              if (statusList.length > 0) {
                  isJobStillRunning = true;
              } else {
                  isJobStillRunning = false;
              }
          };

          function isJobRunning(item) {
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
                      if(jobListCache != undefined){
                          var i = jobListCache.data.indexOf(job);
                          jobListCache.data.splice(i, 1);
                          jobListCache.meta.total = (jobListCache.meta.total - 1);
                      }
                  }).
                  catch(function(e) {
                      MessageService.addMessage(
                          'Error',
                          'Failed to remove job',
                          'Failed to remove job'
                      );
                      console.log(e);
                      reject();
                  });
              });
          };

          /** Gets job output files info **/
          this.getOutputs = function(jobId){
              var deferred = $q.defer();

              $http.get( ftepProperties.URL + '/jobs/' + jobId + '/getOutputs').then(function(response) {
                  deferred.resolve(response.result.data);
              })
              .catch(function(e){
                  MessageService.addMessage(
                      'Error',
                      'Could not get job outputs',
                      'Could not get job outputs'
                  );
                  deferred.reject();
              });

              return deferred.promise;

          };

          return this;
      }]);
});
