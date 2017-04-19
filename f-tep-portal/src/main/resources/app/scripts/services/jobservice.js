/**
 * @ngdoc service
 * @name ftepApp.JobService
 * @description
 * # JobService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('JobService', [ '$http', 'ftepProperties', '$q', '$timeout', '$rootScope', 'MessageService', 'UserService', 'TabService', 'CommunityService', 'traverson', 'moment', function ($http, ftepProperties, $q, $timeout, $rootScope, MessageService, UserService, TabService, CommunityService, traverson, moment) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.jobOwnershipFilters = {
            ALL_JOBS: {id: 0, name: 'All', criteria: ''},
            MY_JOBS: {id: 1, name: 'Mine', criteria: undefined},
            SHARED_JOBS: {id: 2, name: 'Shared', criteria: undefined}
        };

        UserService.getCurrentUser().then(function(currentUser){
            self.jobOwnershipFilters.MY_JOBS.criteria = { owner: {name: currentUser.name } };
            self.jobOwnershipFilters.SHARED_JOBS.criteria = {  owner: {name: "!".concat(currentUser.name) } };
        });

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            explorer: {
                jobs: undefined,
                selectedJob: undefined,
                jobSelectedOutputs: [], //selected outputs
                displayFilters: false, //whether filter section is opened or not
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
                selectedOwnershipFilter: this.jobOwnershipFilters.ALL_JOBS,
                jobCategoryInfo: {} //info about job categories, which ones are opened, etc.
            },
            community: {
                jobs: undefined,
                selectedJob: undefined,
                searchText: '',
                displayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.jobOwnershipFilters.ALL_JOBS,
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
        var startPolling = true;

        var pollJobs = function () {
            $timeout(function () {
                halAPI.from(rootUri + '/jobs/')
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        $rootScope.$broadcast('poll.jobs', document._embedded.jobs);
                        pollJobs();
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Jobs', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollJobs();
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.getJobs = function () {
            var deferred = $q.defer();
                halAPI.from(rootUri + '/jobs/')
                         .newRequest()
                         .getResource()
                         .result
                         .then(
                function (document) {
                    if(startPolling) {
                        pollJobs();
                        startPolling = false;
                    }
                    deferred.resolve(document._embedded.jobs);
                }, function (error) {
                    MessageService.addError ('Could not get Jobs', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        var getJob = function (job) {
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

        var getJobOwner = function (job) {
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

        var getJobDetails = function(job) {
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

        var getJobLogs = function(job) {
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

        this.getJobConfig = function(job) {
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

        var getJobOutputResult = function(job) {
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

        this.removeJob = function(job) {
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

        this.refreshJobs = function (page, action, job) {

            /* Get job list */
            this.getJobs().then(function (data) {

                self.params[page].jobs = data;

                /* Select last job if created */
                if (action === "Create") {
                    self.params[page].selectedJob = self.params[page].jobs[self.params[page].jobs.length-1];
                }

                /* Clear job if deleted */
                if (action === "Remove") {
                    if (job && job.id === self.params[page].selectedJob.id) {
                        self.params[page].selectedJob = undefined;
                    }
                }

                /* Update the selected job */
                self.refreshSelectedJob(page);
           });
        };


        this.refreshSelectedJob = function (page) {

            /* Get job contents if selected */
            if (self.params[page].selectedJob) {
                getJob(self.params[page].selectedJob).then(function (job) {
                    self.params[page].selectedJob = job;

                    getJobOwner(job).then(function (owner) {
                        self.params[page].selectedJob.owner = owner;
                    });

                    getJobDetails(job).then(function (data) {
                        self.params[page].selectedJob.details = data;

                        var startTime = self.params[page].selectedJob.details.startTime;
                        self.params[page].selectedJob.details.startTime =
                            moment().year(startTime.year)
                                    .month(startTime.monthValue)
                                    .day(startTime.dayOfMonth)
                                    .hour(startTime.hour)
                                    .minute(startTime.minute)
                                    .second(startTime.second)
                                    .millisecond(startTime.nano)
                                    .toDate();

                        var endTime = self.params[page].selectedJob.details.endTime;
                        self.params[page].selectedJob.details.endTime =
                            moment().year(endTime.year)
                                    .month(endTime.monthValue)
                                    .day(endTime.dayOfMonth)
                                    .hour(endTime.hour)
                                    .minute(endTime.minute)
                                    .second(endTime.second)
                                    .millisecond(endTime.nano)
                                    .toDate();

                        getJobLogs(job).then(function (logs) {
                             self.params[page].selectedJob.logs = logs;
                        });
                        self.getJobConfig(job).then(function (config) {
                             self.params[page].selectedJob.config = config;
                        });

                        if (self.params[page].selectedJob.outputs) {
                            self.params[page].selectedJob.outputs.links = [];
                            for (var result in self.params[page].selectedJob.outputs.result) {
                                if (self.params[page].selectedJob.outputs.result[result].substring(0,7) === "ftep://") {
                                    getJobOutputResult(job).then(function (result) {
                                        self.params[page].selectedJob.outputs.links.push(result._links.download.href);
                                    });
                                }
                            }
                        }
                    });

                    if(page === 'community'){
                        CommunityService.getObjectGroups(job, 'job').then(function (data) {
                            self.params.community.sharedGroups = data;
                        });
                    }
                    else if(page === 'explorer'){
                        self.params.explorer.jobSelectedOutputs = [];
                    }

                });
            }
        };


        this.launchJob = function(service, inputs){
            var deferred = $q.defer();
            createJobConfig(service, inputs).then(function(jobConfig){
                // Launch the jobConfig
                halAPI.from(jobConfig._links.self.href + '/launch')
                        .newRequest()
                        .post()
                        .result
                        .then(
                 function (document) {
                     MessageService.addInfo ('Job started', service.name + ' job started. Job id: ' + jobConfig.id);
                     deferred.resolve();
                 },
                 function(error){
                     MessageService.addError ('Could Not Launch Job', error);
                     deferred.reject();
                 });
            });

            return deferred.promise;
        };

        function createJobConfig(service, inputs){
            return $q(function(resolve, reject) {
                    halAPI.from(rootUri + '/jobConfigs/')
                    .newRequest()
                    .post({
                        service: service._links.self.href,
                        inputs: inputs
                    })
                    .result
                    .then(
                 function (document) {
                     console.log('JobConfig created: ', document);
                     resolve(JSON.parse(document.body));
                 }, function (error) {
                     MessageService.addError ('Could Not Launch Job', error);
                     reject();
                 });
            });
        }

        return this;
    }]);
});
