/**
 * @ngdoc service
 * @name ftepApp.JobService
 * @description
 * # JobService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('JobService', [ '$http', 'ftepProperties', '$q', '$timeout', '$rootScope', 'MessageService', 'UserService', 'TabService', 'CommunityService', 'traverson', function ($http, ftepProperties, $q, $timeout, $rootScope, MessageService, UserService, TabService, CommunityService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.jobOwnershipFilters = {
                ALL_JOBS: { id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'},
                MY_JOBS: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner' },
                SHARED_JOBS: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner' }
        };

        var userUrl;
        UserService.getCurrentUser().then(function(currentUser){
            userUrl = currentUser._links.self.href;
        });

        self.JOB_STATUSES = [
            { title: "Completed", name: "COMPLETED" },
            { title: "Running", name: "RUNNING" },
            { title: "Error", name: "ERROR" },
            { title: "Created", name: "CREATED" },
            { title: "Cancelled", name: "CANCELLED" }
         ];

        var JOB_STATUSES_STRING = "COMPLETED,RUNNING,ERROR,CREATED,CANCELLED";

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            explorer: {
                jobs: undefined,
                pollingUrl: rootUri + '/jobs/',
                pollingRequestOptions: {},
                pagingData: {},
                selectedJob: undefined,
                jobSelectedOutputs: [], //selected outputs
                displayFilters: false, //whether filter section is opened or not
                selectedStatuses: [],
                selectedOwnershipFilter: this.jobOwnershipFilters.ALL_JOBS,
                jobCategoryInfo: {} //info about job categories, which ones are opened, etc.
            },
            community: {
                jobs: undefined,
                pollingUrl: rootUri + '/jobs/',
                pollingRequestOptions: {},
                pagingData: {},
                selectedJob: undefined,
                searchText: '',
                displayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.jobOwnershipFilters.ALL_JOBS
            }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        function checkJobsQuery(page) {
            /* If no filter set to empty string*/
            if(!self.params[page].pollingRequestOptions.filter) {
               self.params[page].pollingRequestOptions.filter = '';
            }
            /* If no status set to all statuses */
            if (!self.params[page].pollingRequestOptions.status) {
                self.params[page].pollingRequestOptions.status = JOB_STATUSES_STRING;
            }
        }

        var pollJobs = function (page) {

            checkJobsQuery(page);

            pollingTimer = $timeout(function () {
                halAPI.from(self.params[page].pollingUrl + self.params[page].selectedOwnershipFilter.searchUrl)
                    .newRequest()
                    .withRequestOptions({
                        qs: {
                            sort:  self.params[page].pollingRequestOptions.sort,
                            filter:  self.params[page].pollingRequestOptions.filter,
                            owner:  self.params[page].pollingRequestOptions.owner,
                            status:  self.params[page].pollingRequestOptions.status
                        }
                    })
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.jobs', document._embedded.jobs);
                        pollJobs(page);
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Jobs', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollJobs(page);
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.stopPolling = function(){
            if(pollingTimer){
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        function getJobs(page) {

            checkJobsQuery(page);

            var deferred = $q.defer();
                halAPI.from(self.params[page].pollingUrl + self.params[page].selectedOwnershipFilter.searchUrl)
                    .newRequest()
                    .withRequestOptions({
                        qs: {
                            sort:  self.params[page].pollingRequestOptions.sort,
                            filter:  self.params[page].pollingRequestOptions.filter,
                            owner:  self.params[page].pollingRequestOptions.owner,
                            status:  self.params[page].pollingRequestOptions.status
                        }
                    })
                    .getResource()
                    .result
                 .then(
                function (document) {
                    if(startPolling) {
                        pollJobs(page);
                        startPolling = false;
                    }
                    self.params[page].pagingData._links = document._links;
                    self.params[page].pagingData.page = document.page;

                    deferred.resolve(document._embedded.jobs);
                }, function (error) {
                    MessageService.addError('Could not get Jobs', error);
                    deferred.reject();
                });

            return deferred.promise;
        }

        /* Fetch a new page */
        this.getJobsPage = function(page, url){
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get jobs list */
                getJobs(page).then(function (data) {
                    self.params[page].jobs = data;
                });
            }
        };

        this.getJobsByFilter = function (page) {
            if (self.params[page]) {

                /* Get sort parameter */
                self.params[page].pollingRequestOptions.sort = 'id,DESC';

                /* Get filter parameter */
                if(self.params[page].searchText) {
                    self.params[page].pollingRequestOptions.filter = self.params[page].searchText;
                }

                /* Get owner parameter */
                if(self.params[page].selectedOwnershipFilter !== self.jobOwnershipFilters.ALL_JOBS) {
                    self.params[page].pollingRequestOptions.owner = userUrl;
                }

                /* Get status parameter */
                var statusStr = '';
                for(var status = 0; self.params[page].selectedStatuses.length > status; status++){
                    statusStr += "," + self.params[page].selectedStatuses[status];
                }
                self.params[page].pollingRequestOptions.status = statusStr.substring(1);

                /* Get jobs list */
                getJobs(page).then(function (data) {
                    self.params[page].jobs = data;
                });
            }
        };

        var getJob = function (job) {
            var deferred = $q.defer();
                halAPI.from(rootUri + '/jobs/' + job.id + "?projection=detailedJob")
                         .newRequest()
                         .getResource()
                         .result
                         .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Job ' + job.id, error);
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
                    MessageService.addError('Could not get Job ' + job.id + ' owner', error);
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
                MessageService.addError('Could not get contents of Job ' + job.id, error);
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
                    MessageService.addError('Could not get logs for Job ' + job.id, error);
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
                        MessageService.addError('Could not get contents of Job ' + job.id, error);
                        deferred.reject();
                    });
                 });

            return deferred.promise;
        };

        var getJobOutput = function(job, outputLink) {
            var deferred = $q.defer();

            halAPI.from(outputLink + '?projection=detailedFtepFile')
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get output for Job ' + job.id, error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.refreshJobs = function (page, action, job) {

            /* Get job list */
            getJobs(page).then(function (data) {

                self.params[page].jobs = data;

                /* Select last job if created */
                if (action === "Create") {
                    self.params[page].selectedJob = self.params[page].jobs[self.params[page].jobs.length-1];
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
                            self.params[page].selectedJob.downloadLinks = {};
                            self.params[page].selectedJob.wmsLinks = [];
                            for (var itemKey in self.params[page].selectedJob.outputs) {
                                if (self.params[page].selectedJob.outputs[itemKey][0].substring(0,7) === "ftep://") {
                                    getJobOutput(self.params[page].selectedJob, self.params[page].selectedJob.details._links['output-' + itemKey].href).then(function (result) {
                                        var str = self.params[page].selectedJob.outputs[itemKey][0];
                                        self.params[page].selectedJob.downloadLinks[self.params[page].selectedJob.outputs[itemKey][0]] =  result._links.download.href;
                                        if(result._links.wms){
                                            var wmsObj = {
                                                    key: itemKey,
                                                    wms: result._links.wms.href,
                                                    geo: result.metadata.geometry
                                            };
                                            self.params[page].selectedJob.wmsLinks.push(wmsObj);
                                        }
                                    });
                                }
                            }
                        }
                    });

                    if(page === 'community' && job.access.currentLevel === 'ADMIN') {
                        CommunityService.getObjectGroups(job, 'job').then(function (data) {
                            self.params.community.sharedGroups = data;
                        });
                    }
                    else if(page === 'explorer'){
                        self.params.explorer.jobSelectedOutputs = [];
                        $rootScope.$broadcast('show.products', self.params[page].selectedJob);
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
                 MessageService.addInfo('Job started', 'A new ' + service.name + ' job started.');
                 deferred.resolve();
             },
             function(error){
                 MessageService.addError('Could not launch Job', error);
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
                     MessageService.addError('Could not create JobConfig', error);
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
                     MessageService.addError('Could not get Job cost estimation', error);
                     reject();
                 });
            });
        };

        return this;
    }]);
});
