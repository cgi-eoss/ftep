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

        this.jobOwnershipFilters = {
            ALL_JOBS: { id: 0, name: 'All', searchUrl: 'search/findByFilterAndIsNotSubjob', subjobsSearchUrl: 'search/findByFilterAndParent'},
            MY_JOBS: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndIsNotSubjobAndOwner', subjobsSearchUrl: 'search/findByFilterAndParentAndOwner' },
            SHARED_JOBS: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndIsNotSubjobAndNotOwner', subjobsSearchUrl: 'search/findByFilterAndParentAndNotOwner' }
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
                parentId: null,
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
                selectedOwnershipFilter: self.jobOwnershipFilters.MY_JOBS,
                jobTab: undefined
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
            self.params[page].jobs = [];
            getJobs(page).then(function (data) {
                self.params[page].jobs = data;
            });
        };

        function filterJobs(page) {
            if (_this.params[page] && UserService.params.activeUser._links) {

                var searchUrlKey = _this.params[page].parentId ? 'subjobsSearchUrl' : 'searchUrl';

                /* Set base URL */
                _this.params[page].pollingUrl = rootUri + '/jobs/' + _this.params[page].selectedOwnershipFilter[searchUrlKey];


                /* Get sort parameter */
                _this.params[page].pollingUrl += '?sort=id,DESC';

                /* Get owner parameter */
                if (_this.params[page].selectedOwnershipFilter !== _this.jobOwnershipFilters.ALL_JOBS) {
                    _this.params[page].pollingUrl += '&owner=' + UserService.params.activeUser._links.self.href;
                }

                if (_this.params[page].parentId) {
                    _this.params[page].pollingUrl += '&parentId=' + _this.params[page].parentId;
                }

                /* Get status parameter */
                var statusStr = '';
                if (_this.params[page].selectedStatuses) {
                    statusStr = _this.params[page].selectedStatuses.join(',');
                }
                if (statusStr) {
                    _this.params[page].pollingUrl += '&status=' + statusStr;
                } else {
                    _this.params[page].pollingUrl += '&status=' + JOB_STATUSES_STRING;
                }

                /* Get text search parameter */
                if (_this.params[page].searchText) {
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

        this.getShortJob = function (job) {
            var deferred = $q.defer();
                halAPI.from(rootUri + '/jobs/' + job.id + "?projection=shortJob")
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

        var getJobLogs = function(job) {
            var deferred = $q.defer();

            halAPI.from(rootUri + '/jobs/' + job.id)
                .newRequest()
                .follow('logs')
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get logs for Job ' + job.id, error);
                    deferred.reject();
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

        // Switch the boolean flag to initiate the proper behavior with the Jobs List scroll action
        this.broadcastNewjob = function(){
            $rootScope.$broadcast('newjob.started.init');
        };

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
                            _this.params[page].selectedJob = self.params[page].jobs[job];
                            // Notify the Explorer to trigger the Job List scroll when the Bottombar has opened
                            $rootScope.$broadcast('newjob.started.show', job);
                        }
                    }
                }

                /* Update the selected job */
                self.refreshSelectedJob(page);
           });
        };


        this.refreshSelectedJob = function(page) {

            /* Get job contents if selected */
            if (_this.params[page].selectedJob) {

                var job = _this.params[page].selectedJob;

                getJobOwner(job).then(function(owner) {
                    job.owner = owner;
                });

                _this.getJobConfig(job).then(function(config) {
                    job.config = config;
                });

                if (page === 'community') {
                    CommunityService.getObjectGroups(job, 'job').then(function(data) {
                        _this.params.community.sharedGroups = data;
                    });
                }

                _this.params[page].selectedJob = job;
            }
        };

        this.fetchJobOutputs = function(page) {
            if (_this.params[page].selectedJob.outputFiles) {
                return;
            }
            getJob(_this.params[page].selectedJob).then(function (job) {
                if (job.outputs) {
                    getOutputFiles(job).then(function(result) {
                        job.outputFiles = result._embedded.ftepFiles;
                        _this.params[page].selectedJob = job;
                        if (page === 'explorer') {
                            _this.params.explorer.jobSelectedOutputs = [];
                        }
                    });
                }
            });
        };

        this.fetchJobLogs = function(page) {
            if (_this.params[page].selectedJob.logs) {
                return;
            }
            getJobLogs(_this.params[page].selectedJob).then(function(logs) {
                 _this.params[page].selectedJob.logs = logs;
            });
        };

        this.launchJob = function(jobConfig, service) {
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(jobConfig._links.self.href + '/launch')
                    .newRequest()
                    .post()
                    .result
                    .then(
            function (document) {
                if (document.status == 403) {
                    deferred.reject(document);
                } else {
                    launchedJobID = JSON.parse(document.data).id;
                    MessageService.addInfo('Job ' + launchedJobID + ' started', 'A new ' + service.name + ' job started.');
                    deferred.resolve();
                }
            },
            function(error) {
                MessageService.addError('Could not launch Job', error);
                deferred.reject();
            });

            return deferred.promise;
        };

        this.createJobConfig = function(jobConfig) {
            return $q(function(resolve, reject) {
                    halAPI.from(rootUri + '/jobConfigs/')
                    .newRequest()
                    .post(jobConfig)
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
                    if (error.httpStatus === 400) {
                        MessageService.addError('Could not evaluate process job parameters', error);
                    } else if (error.httpStatus === 402) {
                        MessageService.addError('Balance exceeded', error);
                    } else {
                        MessageService.addError('Could not get Job cost estimation', error);
                    }
                    try {
                        reject(JSON.parse(error.body));
                    } catch(e) {
                        reject(error.body);
                    }
                 });
            });
        };

        this.terminateJob = function(job) {
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(job._links.terminate.href)
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

        this.updateJobTab = function(page, tab) {
            _this.params[page].jobTab = tab;
        };

        return this;
    }]);
});
