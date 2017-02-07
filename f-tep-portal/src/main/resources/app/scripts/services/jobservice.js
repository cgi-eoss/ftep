/**
 * @ngdoc service
 * @name ftepApp.JobService
 * @description
 * # JobService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('JobService', [ '$http', 'ftepProperties', '$q', '$timeout', '$rootScope', 'MessageService', 'UserService',
                                        function ($http, ftepProperties, $q, $timeout, $rootScope, MessageService, UserService) {

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          var POLLING_FREQUENCY = 4 * 1000;
          var isPolling = false;
          var jobListCache;
          var isJobStillRunning = false; //whether a job exists, that is still running
          var connectionError = false, retriesLeft = 3;
          var waitingForNewJob = false;

          var USE_TEST_DATA = false;

          /** Return the jobs for the user **/
          this.getJobs = function(newJobWasPushed) {
              if(USE_TEST_DATA){
                  var deferred = $q.defer();
                  $timeout(function () {
                      getTestData(deferred);
                  }, 100);
                  return deferred.promise;
              }

              if(newJobWasPushed){
                  waitingForNewJob = newJobWasPushed;
              }

              if(isPolling){
                  return $q.when(jobListCache);
              } else {
                  return pollJobs();
              }
          };

          //TODO only for prototyping
          function getTestData(deferred){
              console.log('USING TEST DATA for jobs');
              $.getJSON("temp_data/test_jobs.json", function(json) {
                  jobListCache = json;
                  deferred.resolve(json);
                  $rootScope.$broadcast('refresh.jobs', json);
              })
              .fail(function(e) {
                console.log( "error", e );
              });
          }

          /** Polls jobs every 4 sec **/
          var pollJobs = function () {

              if (connectionError && retriesLeft === 0) {
                  retriesLeft = 3;
                  connectionError = false;
              } else {
                  var deferred = $q.defer();
                  $http.get(ftepProperties.URL + '/jobs?include=service&fields[service]=name').then(function (response) {
                          if (angular.equals(jobListCache, response.data) == false) {
                              waitingForNewJob = ((jobListCache != undefined) && (jobListCache.meta.total <= response.data.meta.total));
                              jobListCache = response.data;
                              $rootScope.$broadcast('refresh.jobs', response.data);
                              findRunningJob();
                          }
                          deferred.resolve(response.data);
                          retriesLeft = 3;
                          connectionError = false;
                      })
                      .catch(function (e) {
                          connectionError = true;
                          MessageService.addError(
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
                      MessageService.addInfo('Job deleted', 'Job '.concat(job.id).concat(' deleted'));
                  }).
                  catch(function(e) {
                      MessageService.addError(
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
                  deferred.resolve(response.data.data);
              })
              .catch(function(e){
                  MessageService.addError(
                      'Could not get job outputs'
                  );
                  deferred.reject();
              });

              return deferred.promise;

          };

          this.jobOwnershipFilters = {
                  ALL_JOBS: {id: 0, name: 'All', criteria: ''},
                  MY_JOBS: {id: 1, name: 'Mine', criteria: undefined},
                  SHARED_JOBS: {id: 2, name: 'Shared', criteria: undefined}
          };

          var that = this; //workaround for now
          UserService.getCurrentUser().then(function(currentUser){
              that.jobOwnershipFilters.MY_JOBS.criteria = { owner: {name: currentUser.name } };
              that.jobOwnershipFilters.SHARED_JOBS.criteria = {  owner: {name: "!".concat(currentUser.name) } };
          });

          /** PRESERVE USER SELECTIONS **/
          this.params = {
              explorer: {
                  selectedJob: undefined,
                  jobOutputs: [], //all outputs of the selected job
                  jobSelectedOutputs: [], //selected outputs
                  displayFilters: false, //whether filter section is opened or not
                  jobStatuses: [    //filter options
                                {
                                    name: "Succeeded",
                                    value: true
                                }, {
                                    name: "Failed",
                                    value: true
                                }, {
                                    name: "Running",
                                    value: true
                                }
                              ],
                  selectedOwnershipFilter: this.jobOwnershipFilters.ALL_JOBS,
                  jobGroupInfo: {} //info about job groups, which ones are opened, etc.
              },
              community: {
              }
          };
          /** END OF PRESERVE USER SELECTIONS **/

          return this;
      }]);
});
