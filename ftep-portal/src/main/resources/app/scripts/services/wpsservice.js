/**
 * @ngdoc service
 * @name ftepApp.WpsService
 * @description
 * # WpsService
 * Service in the ftepApp.
 */
define(['../ftepmodules', 'module', 'jquery', 'zoo', 'xml2json','ol', 'hgn!zoo-client/assets/tpl/describe_process_form1'],
    function (ftepmodules, module, $, Zoo, X2JS, ol, tpl_describeProcess) {
    'use strict';

    ftepmodules.service('WpsService', [ '$rootScope', '$http', 'ftepProperties', '$q', function ($rootScope, $http, ftepProperties, $q) {

          /** Set the header defaults **/
          $http.defaults.headers.post['Content-Type'] = 'application/json';
          $http.defaults.withCredentials = true;

          var zoo = new Zoo({
              url: 'https://192.171.139.83/wps',
              delay: 3000,
          });

          /** Returns the list of available processes **/
          this.getCapabilities = function(){
              var deferred = $q.defer();
              zoo.getCapabilities( {
                  type: 'POST',
                  success: function(data){
                      console.log("--------------------CAPABILITIES---------------------------");
                      console.log( data );
                      deferred.resolve(data);
                  }
              });
              return deferred.promise;
          };

          /** Returns the process description (inputs, outputs) **/
          this.getDescription = function(identifier){
              var deferred = $q.defer();
              zoo.describeProcess({
                  identifier: identifier,
                  type: "POST",
                  success: function(data) {
                      console.log("-------------------- PROCESS ---------------------------");
                      console.log( data );
                      deferred.resolve(data);
                  }
                  , error: function(data) {
                      console.log( data );
                      deferred.reject(data);
                  }
              });
              return deferred.promise;
          }

          this.execute = function(aProcess, iparams, oparams){
              var deferred = $q.defer();
              zoo.execute({
                  identifier: aProcess,
                  dataInputs: iparams,
                  dataOutputs: oparams,
                  type: 'POST',
                      storeExecuteResponse: true,
                      status: true,
                      success: function(data, launched) {
                          console.log("**** SUCCESS ****");
                          console.log(launched);

                          $rootScope.$broadcast('reload.jobs');

                          // Polling status
                          zoo.watch(launched.sid, {
                              onPercentCompleted: function(data) {
                                  console.log("**** PercentCompleted ****");
                                  console.log(data);

                                  $rootScope.$broadcast('update.job.progress', data.percentCompleted, aProcess);
                              },
                              onProcessSucceeded: function(data) {
                                  console.log("**** ProcessSucceeded ****");
                                  console.log(data);

                                  $rootScope.$broadcast('update.job.progress', 100, aProcess);
                                  deferred.resolve(data);
                              },
                              onError: function(data) {
                                  console.log("**** onError ****");
                                  console.log(data);
                                  deferred.reject(data);
                              },
                         });
                     },
                     error: function(data) {
                         console.log('Execute failed: ', data.ExceptionReport.Exception.ExceptionText);
                         deferred.reject('Execute failed: ' + data.ExceptionReport.Exception.ExceptionText);
                     }
                });
              return deferred.promise;
          }

          return this;
      }]);
});