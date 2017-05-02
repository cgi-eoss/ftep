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
                halAPI.from(rootUri + '/jobs/?size=100') //TODO implement paging
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
                halAPI.from(rootUri + '/jobs/?size=100') //TODO implement paging
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
                    MessageService.addError('Could not get Job logs', error);
                    deferred.reject();
                });
             });

            return deferred.promise;
        };

        this.getJobConfig = function(job) {
            var deferred = $q.defer();

            halAPI.from(rootUri + '/jobs/' + job.id)
                .newRequest()
                .follow('job')
                .getResource()
                .continue().then(function (nextBuilder) {
                    var nextRequest = nextBuilder.newRequest();
                    nextRequest
                    .follow('config')
                    .getResource()
                    .result
                    .then(function (document) {
                        deferred.resolve(document);
                    }, function (error) {
                        MessageService.addError('Could not get Job config', error);
                        deferred.reject();
                    });
                 });

            return deferred.promise;
        };

        var getJobOutput = function(outputLink) {
            var deferred = $q.defer();

            halAPI.from(outputLink)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Job output', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.refreshJobs = function (page, action, job) {

            /* Get job list */
            this.getJobs().then(function (data) {

                self.params[page].jobs = data;

                /* Select last job if created */
                if (action === "Create") {
                    self.params[page].selectedJob = self.params[page].jobs[self.params[page].jobs.length-1];
                }

                /* Update the selected job */
                self.refreshSelectedJob(page);

                if(page === 'explorer'){
                    $rootScope.$broadcast('update.jobGroups');
                }
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
                            new Date(startTime.year + "-" +
                                    getTwoDigitNumber(startTime.monthValue) + "-" +
                                    getTwoDigitNumber(startTime.dayOfMonth) + "T" +
                                    getTwoDigitNumber(startTime.hour) + ":" +
                                    getTwoDigitNumber(startTime.minute) + ":" +
                                    getTwoDigitNumber(startTime.second) + "." +
                                    getThreeDigitNumber(startTime.nano/1000000) + "Z").toISOString();

                        var endTime = self.params[page].selectedJob.details.endTime;
                        self.params[page].selectedJob.details.endTime =
                            new Date(endTime.year + "-" +
                                    getTwoDigitNumber(endTime.monthValue) + "-" +
                                    getTwoDigitNumber(endTime.dayOfMonth) + "T" +
                                    getTwoDigitNumber(endTime.hour) + ":" +
                                    getTwoDigitNumber(endTime.minute) + ":" +
                                    getTwoDigitNumber(endTime.second) + "." +
                                    getThreeDigitNumber(endTime.nano/1000000) + "Z").toISOString();

                        getJobLogs(job).then(function (logs) {
                             self.params[page].selectedJob.logs = logs;
                        });
                        self.getJobConfig(job).then(function (config) {
                             self.params[page].selectedJob.config = config;
                        });

                        if (self.params[page].selectedJob.outputs) {
                            self.params[page].selectedJob.outputs.links = [];
                            for (var itemKey in self.params[page].selectedJob.outputs) {
                                if (self.params[page].selectedJob.outputs[itemKey][0].substring(0,7) === "ftep://") {
                                    getJobOutput(self.params[page].selectedJob['output-' + itemKey].href).then(function (result) {
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

        function getTwoDigitNumber(num){
            return (num > 9 ? num : '0'+num);
        }

        function getThreeDigitNumber(num){
            return (num > 99 ? num : (num > 9 ? '0'+num : '00' + num));
        }

        this.launchJob = function(jobConfig, service){
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(jobConfig._links.self.href + '/launch')
                    .newRequest()
                    .post()
                    .result
                    .then(
             function (document) {
                 MessageService.addInfo ('Job started', 'A new ' + service.name + ' job started.');
                 deferred.resolve();
             },
             function(error){
                 MessageService.addError ('Could not launch Job', error);
                 deferred.reject();
             });

            return deferred.promise;
        };

        this.createJobConfig = function(service, inputs){
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
                     resolve(JSON.parse(document.body));
                 }, function (error) {
                     MessageService.addError ('Could not start Job', error);
                     reject();
                 });
            });
        };

        this.estimateJob = function(jobConfig){
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/estimateCost/jobConfig/' + jobConfig.id)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                     resolve(document);
                 }, function (error) {
                     MessageService.addError ('Could not get Job estimation', error);
                     reject();
                 });
            });
        };

        return this;
    }]);
});
