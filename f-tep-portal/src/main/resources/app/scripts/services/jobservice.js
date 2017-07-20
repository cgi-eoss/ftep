/**
 * @ngdoc service
 * @name ftepApp.JobService
 * @description
 * # JobService
 * Service in the ftepApp.
 */
'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('JobService', [ 'ftepProperties', '$q', '$timeout', '$rootScope', 'MessageService', 'CommonService', 'UserService', 'CommunityService', 'traverson', function (ftepProperties, $q, $timeout, $rootScope, MessageService, CommonService, UserService, CommunityService, traverson) {

        /* TODO: Migrate self to _this as self is a reserved word and is causing scoping issues */
        var self = this;
        var _this = this;
        var launchedJobID;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        var userUrl;
        UserService.getCurrentUser().then(function(currentUser){
            userUrl = currentUser._links.self.href;
        });

        this.jobOwnershipFilters = {
            ALL_JOBS: { id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'},
            MY_JOBS: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner' },
            SHARED_JOBS: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner' }
        };

        var JOB_STATUSES_STRING = "COMPLETED,RUNNING,ERROR,CREATED,CANCELLED";
        this.JOB_STATUSES = [
            { title: "Completed", name: "COMPLETED" },
            { title: "Running", name: "RUNNING" },
            { title: "Error", name: "ERROR" },
            { title: "Created", name: "CREATED" },
            { title: "Cancelled", name: "CANCELLED" }
         ];

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            explorer: {
                jobs: undefined,
                pollingUrl: rootUri + '/jobs?sort=id,DESC',
                pollingRequestOptions: {},
                pagingData: {},
                selectedJob: undefined,
                jobSelectedOutputs: [], //selected outputs
                wms: {
                    isAllVisible: false,
                    visibleList: []
                },
                displayFilters: false, //whether filter section is opened or not
                selectedStatuses: [],
                selectedOwnershipFilter: this.jobOwnershipFilters.MY_JOBS,
                jobCategoryInfo: {} //info about job categories, which ones are opened, etc.
            },
            community: {
                jobs: undefined,
                pollingUrl: rootUri + '/jobs/?sort=id,DESC',
                pollingRequestOptions: {},
                pagingData: {},
                selectedJob: undefined,
                searchText: '',
                displayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.jobOwnershipFilters.MY_JOBS
            }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollJobs = function (page) {
            pollingTimer = $timeout(function () {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
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
                    }
                );
            }, POLLING_FREQUENCY);
        };

        this.stopPolling = function(){
            if(pollingTimer){
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        function getJobs(page) {
            var deferred = $q.defer();
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
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
                getJobs(page).then(function(data) {
                    self.params[page].jobs = data;
                });
            }
        };

        this.getJobsByFilter = function (page) {
            filterJobs(page);
            getJobs(page).then(function (data) {
                self.params[page].jobs = data;
            });
        };

        function filterJobs(page) {
            if (_this.params[page]) {

                /* Set base URL */
                _this.params[page].pollingUrl = rootUri + '/jobs/' + _this.params[page].selectedOwnershipFilter.searchUrl;

                /* Get sort parameter */
                _this.params[page].pollingUrl += '?sort=id,DESC';

                /* Get owner parameter */
                if(_this.params[page].selectedOwnershipFilter !== _this.jobOwnershipFilters.ALL_JOBS) {
                    _this.params[page].pollingUrl += '&owner=' + userUrl;
                }

                /* Get status parameter */
                var statusStr = '';
                if(_this.params[page].selectedStatuses) {
                    statusStr = _this.params[page].selectedStatuses.join(',');
                }
                if (statusStr) {
                    _this.params[page].pollingUrl += '&status=' + statusStr;
                } else {
                    _this.params[page].pollingUrl += '&status=' + JOB_STATUSES_STRING;
                }

                /* Get text search parameter */
                if(_this.params[page].searchText) {
                    _this.params[page].pollingUrl += '&filter=' +  _this.params[page].searchText;
                } else {
                    _this.params[page].pollingUrl += "&filter=";
                }
            }
        }

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

        function getOutputFiles(job){
            var deferred = $q.defer();

            halAPI.from(rootUri + '/jobs/' + job.id + '/outputFiles?projection=detailedFtepFile')
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get outputs for Job ' + job.id, error);
                    deferred.reject();
                });

            return deferred.promise;
        }

        this.refreshJobs = function (page, action, job) {

            /* Get job list */
            filterJobs(page);
            getJobs(page).then(function (data) {

                self.params[page].jobs = data;

                /* Select last job if created */
                if (action === "Create") {
                    for (job in self.params[page].jobs) {
                        if (self.params[page].jobs[job].id === launchedJobID) {
                            self.params[page].selectedJob = self.params[page].jobs[job];
                        }
                    }
                }

                /* Update the selected job */
                self.refreshSelectedJob(page);
           });
        };


        this.refreshSelectedJob = function (page) {

            /* Get job contents if selected */
            if (_this.params[page].selectedJob) {

                getJob(_this.params[page].selectedJob).then(function (job) {

                    getJobOwner(job).then(function (owner) {
                        job.owner = owner;
                    });

                    getJobDetails(job).then(function (details) {
                        job.details = details;

                        getJobLogs(job).then(function (logs) {
                             job.logs = logs;
                        });
                        _this.getJobConfig(job).then(function (config) {
                            job.config = config;
                        });

                        if (job.outputs) {
                            getOutputFiles(job).then(function(result){
                                job.outputFiles = result._embedded.ftepFiles;
                                _this.params[page].selectedJob = job;
                                if(page === 'explorer'){
                                    _this.params.explorer.jobSelectedOutputs = [];
                                }
                            });
                        } else {
                            _this.params[page].selectedJob = job;
                        }
                    });

                    if(page === 'community' && job.access.currentLevel === 'ADMIN') {
                        CommunityService.getObjectGroups(job, 'job').then(function (data) {
                            _this.params.community.sharedGroups = data;
                        });
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

        this.launchJob = function(jobConfig, service, page) {
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(jobConfig._links.self.href + '/launch')
                    .newRequest()
                    .post()
                    .result
                    .then(
             function (document) {
                 launchedJobID = JSON.parse(document.data).content.id;
                 MessageService.addInfo('Job ' + launchedJobID + ' started', 'A new ' + service.name + ' job started.');
                 deferred.resolve();
             },
             function(error){
                 MessageService.addError('Could not launch Job', error);
                 deferred.reject();
             });

            return deferred.promise;
        };

        this.createJobConfig = function(service, inputs, label){
            return $q(function(resolve, reject) {
                    halAPI.from(rootUri + '/jobConfigs/')
                    .newRequest()
                    .post({
                        service: service._links.self.href,
                        inputs: inputs,
                        label: label
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

        this.estimateJob = function(jobConfig, $event){
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/estimateCost/jobConfig/' + jobConfig.id)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                     resolve(document);
                 }, function (error) {
                    if (error.httpStatus === 402) {
                        MessageService.addError('Balance exceeded', error);
                    } else {
                        MessageService.addError('Could not get Job cost estimation', error);
                    }
                    reject(JSON.parse(error.body));
                 });
            });
        };

        this.terminateJob = function(job) {
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(job._links.terminate)
                    .newRequest()
                    .post()
                    .result
                    .then(
             function (document) {
                 MessageService.addInfo('Job ' + job.id + ' cancelled', 'Job ' + job.id + ' terminated by the user.');
                 deferred.resolve();
             },
             function(error){
                 MessageService.addError('Could not terminate the Job', error);
                 deferred.reject();
             });

            return deferred.promise;
        };

        return this;
    }]);
});
