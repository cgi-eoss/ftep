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

          var isPolling = false;
          var jobListCache;
          var isJobStillRunning = false; //whether a job exists, that is still running
          var connectionError = false, retriesLeft = 3;
          var waitingForNewJob = false;

          /** Return the jobs for the user **/
          this.getJobs = function(newJobPushed) {
              if(newJobPushed){
                  waitingForNewJob = newJobPushed;
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
                  /*$http.get(ftepProperties.URL + '/jobs?include=service&fields[service]=name').then(function (response) {
                          if (angular.equals(jobListCache, response.data) == false) {
                              jobListCache = response.data;
                              $rootScope.$broadcast('refresh.jobs', response.data);
                              findRunningJob();
                              waitingForNewJob = false;
                          }
                          deferred.resolve(response.data);
                          retriesLeft = 3;
                          connectionError = false;
                      })
                      .catch(function (e) {
                          connectionError = true;
                          window.alert('Could not get jobs. Retries left: ' + retriesLeft);
                          retriesLeft--;
                          deferred.reject();
                      })
                      .finally(function () {
                          if (isJobStillRunning === true || waitingForNewJob === true) {
                              isPolling = true;
                              $timeout(pollJobs, 4 * 1000);
                          }
                      });*/


                  // TODO Remove static test data.
                   var data = {"links":{"self":"https:\/\/192.168.3.83\/api\/v1.0\/jobs"},"data":[{"type":"jobs","id":"707","attributes":{"inputs":{"inputfile":["ftp:\/\/ftp.ceda.ac.uk\/neodc\/sentinel1a\/data\/EW\/L1_GRD\/m\/IPF_v2\/2015\/11\/18\/S1A_EW_GRDM_1SSH_20151118T001148_20151118T001252_008651_00C4B8_36CE.zip", "ftp:\/\/ftp.ceda.ac.uk\/neodc\/sentinel1a\/data\/EW\/L1_GRD\/mn\/IPF_v2\/2015\/11\/18\/S1A_EW_GRDM_1SSH_20151118T001148_20151118T001252_008651_00C4B8_36CE.zip", "ftp:\/\/ftp.ceda.ac.uk\/neodc\/sentinel1a\/data\/EW\/L1_GRD\/mn\/IPF_v2\/2015\/11\/18\/S1A_EW_GRDM_1SSH_20151118T001148_20151118T001252_008651_00C4B8_36CE1.zip", "ftp:\/\/ftp.ceda.ac.uk\/neodc\/sentinel1a\/data\/EW\/L1_GRD\/mn\/IPF_v2\/2015\/11\/18\/S1A_EW_GRDM_1SSH_20151118T001148_20151118T001252_008651_00C4B8_36CE2.zip", "ftp:\/\/ftp.ceda.ac.uk\/neodc\/sentinel1a\/data\/EW\/L1_GRD\/mn\/IPF_v2\/2015\/11\/18\/S1A_EW_GRDM_1SSH_20151118T001148_20151118T001252_008651_00C4B8_36CE3.zip"],"gui-timeout":["1"]},"outputs":"{}","status":"Running","step":"Step 2of3:Processing","guiEndpoint":"http:\/\/192.171.139.88:32781"},"relationships":{"service":{"data":[{"type":"services","id":"19"}]}}}],"included":[{"type":"services","id":"19","attributes":{"name":"MonteverdiAppV2"}}],"meta":{"total":1}};


                  deferred.resolve(data);
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
                  }).
                  catch(function(e) {
                      window.alert('Failed to remove job');
                      console.log(e);
                      reject();
                  });
              });
          };

          /** Gets job output files info **/
          this.getOutputs = function(jobId){
              var deferred = $q.defer();

              http.get( ftepProperties.URL + '/jobs/' + jobId + '/getOutputs').then(function(response) {
                  deferred.resolve(response.data.data);
              })
              .catch(function(e){
                  window.alert('Could not get job outputs');
                  deferred.reject();
              });

              return deferred.promise;

          };

          return this;
      }]);
});
