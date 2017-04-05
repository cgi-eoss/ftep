/**
 * @ngdoc service
 * @name ftepApp.JobService
 * @description
 * # JobService
 * Service in the ftepApp.
 */
'use strict';
define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('JobService', [ '$http', 'ftepProperties', '$q', '$timeout', '$rootScope', 'MessageService', 'UserService', 'TabService', 'traverson', 'moment', function ($http, ftepProperties, $q, $timeout, $rootScope, MessageService, UserService, TabService, traverson, moment) {

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        var that = this;

        this.jobOwnershipFilters = {
            ALL_JOBS: {id: 0, name: 'All', criteria: ''},
            MY_JOBS: {id: 1, name: 'Mine', criteria: undefined},
            SHARED_JOBS: {id: 2, name: 'Shared', criteria: undefined}
        };

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
                jobCategoryInfo: {} //info about job categories, which ones are opened, etc.
            },
            community: {
                jobs: undefined,
                job: undefined,
                selectedJob: undefined,
                searchText: '',
                displayFilters: false,
                jobStatuses: [    //filter options
                    {
                        title: "Completed",
                        name: "COMPLETED",
                        value: true
                    }, {
                        title: "Running",
                        name: "RUNNING",
                        value: true
                    }, {
                        title: "Error",
                        name: "ERROR",
                        value: true
                    }
                ],
            }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;

        var pollJobsV2 = function () {
            $timeout(function () {
                halAPI.from(rootUri + '/jobs/')
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        $rootScope.$broadcast('poll.jobsV2', document._embedded.jobs);
                        if(TabService.startPolling().jobs) {
                            pollJobsV2();
                        }
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Jobs', error);
                        pollCount -= 1;
                        if (pollCount >= 0 && TabService.startPolling().jobs) {
                            pollJobsV2();
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.getJobsV2 = function () {
            var deferred = $q.defer();
                halAPI.from(rootUri + '/jobs/')
                         .newRequest()
                         .getResource()
                         .result
                         .then(
                function (document) {
                    if(TabService.startPolling().jobs) {
                        pollJobsV2();
                    }
                    deferred.resolve(document._embedded.jobs);
                }, function (error) {
                    MessageService.addError ('Could not get Jobs', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        var getJobV2 = function (job) {
            var deferred = $q.defer();
                halAPI.from(rootUri + '/jobs/' + job.id)
                         .newRequest()
                         .getResource()
                         .result
                         .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError ('Could not get Job', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        var getJobOwnerV2 = function (job) {
            var deferred = $q.defer();
                halAPI.from(rootUri + '/jobs/' + job.id)
                         .newRequest()
                         .follow('owner')
                         .getResource()
                         .result
                         .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError ('Could not get Job', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        var getJobRequest;

        var getJobDetailsV2 = function(job) {
            var deferred = $q.defer();

            getJobRequest = halAPI.from(rootUri + '/jobs/' + job.id)
                            .newRequest()
                            .follow('job')
                            .getResource();
            getJobRequest.result.then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError ('Could not get Job contents', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        var getJobLogsV2 = function(job) {
            var deferred = $q.defer();

            getJobRequest.continue().then(function (nextBuilder) {
                var nextRequest = nextBuilder.newRequest();
                nextRequest
                .follow('logs')
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Job contents', error);
                    deferred.reject();
                });
             });

            return deferred.promise;
        };

        var getJobConfigV2 = function(job) {
            var deferred = $q.defer();

            getJobRequest.continue().then(function (nextBuilder) {
                var nextRequest = nextBuilder.newRequest();
                nextRequest
                .follow('config')
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Job contents', error);
                    deferred.reject();
                });
             });

            return deferred.promise;
        };

        var getJobOutputResultV2 = function(job) {
            var deferred = $q.defer();

            getJobRequest.continue().then(function (nextBuilder) {
                var nextRequest = nextBuilder.newRequest();
                nextRequest
                .follow('output-result')
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Job Output Results', error);
                    deferred.reject();
                });
             });

            return deferred.promise;
        };

        this.removeJobV2 = function(job) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/jobs/' + job.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Job deleted', 'Job '.concat(job.name).concat(' deleted.'));
                        resolve(job);
                    } else {
                        MessageService.addError ('Failed to Remove Job', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to Remove Job', error);
                    reject();
                });
            });
        };

        this.refreshJobsV2 = function (service, action, job) {

            if (service === "Community") {

                /* Get job list */
                this.getJobsV2().then(function (data) {

                    that.params.community.jobs = data;

                    /* Select last job if created */
                    if (action === "Create") {
                        that.params.community.selectedJob = that.params.community.jobs[that.params.community.jobs.length-1];
                    }

                    /* Clear job if deleted */
                    if (action === "Remove") {
                        if (job && job.id === that.params.community.selectedJob.id) {
                            that.params.community.selectedJob = undefined;
                            that.params.community.job = [];
                        }
                    }

                    /* Update the selected job */
                    that.refreshSelectedJobV2("Community");

                });

            }

        };

        this.refreshSelectedJobV2 = function (service) {

            if (service === "Community") {
                /* Get job contents if selected */
                if (that.params.community.selectedJob) {
                    getJobV2(that.params.community.selectedJob).then(function (job) {
                        that.params.community.selectedJob = job;
                        that.params.community.job = job;

                        getJobOwnerV2(job).then(function (owner) {
                            that.params.community.job.owner = owner;
                            that.params.community.job.owner.name = owner.name;
                        });

                        getJobDetailsV2(job).then(function (data) {
                            that.params.community.job.details = data;

                            var startTime = that.params.community.job.details.startTime;
                            that.params.community.job.details.startTime =
                                moment().year(startTime.year)
                                        .month(startTime.monthValue)
                                        .day(startTime.dayOfMonth)
                                        .hour(startTime.hour)
                                        .minute(startTime.minute)
                                        .second(startTime.second)
                                        .millisecond(startTime.nano)
                                        .toDate();

                            var endTime = that.params.community.job.details.endTime;
                            that.params.community.job.details.endTime =
                                moment().year(endTime.year)
                                        .month(endTime.monthValue)
                                        .day(endTime.dayOfMonth)
                                        .hour(endTime.hour)
                                        .minute(endTime.minute)
                                        .second(endTime.second)
                                        .millisecond(endTime.nano)
                                        .toDate();

                            getJobLogsV2(job).then(function (logs) {
                                 that.params.community.job.logs = logs;
                            });
                            getJobConfigV2(job).then(function (config) {
                                 that.params.community.job.config = config;
                            });

                            if (that.params.community.job.outputs) {
                                that.params.community.job.outputs.links = [];
                                for (var result in that.params.community.job.outputs.result) {
                                    if (that.params.community.job.outputs.result[result].substring(0,7) === "ftep://") {
                                        getJobOutputResultV2(job).then(function (result) {
                                            that.params.community.job.outputs.links.push(result._links.download.href);
                                        });
                                    } else {
                                        that.params.community.job.outputs.links.push(null);
                                    }
                                }
                            }
                        });
                    });
                }
            }

        };





        /** API V1 **/

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          //var POLLING_FREQUENCY = 4 * 1000;
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

          return this;
      }]);
});
