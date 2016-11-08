/**
 * @ngdoc function
 * @name ftepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the ftepApp
 */
define(['../ftepmodules', 'hgn!zoo-client/assets/tpl/ftep_describe_process'], function (ftepmodules, tpl_describeProcess) {
    'use strict';
    
    ftepmodules.controller('WorkspaceCtrl', [ '$scope', '$rootScope', '$mdDialog', '$sce', '$document', 'WpsService',  function ($scope, $rootScope, $mdDialog, $sce, $document, WpsService) {
        this.awesomeThings = [
          'HTML5 Boilerplate',
          'AngularJS',
          'Karma'
        ];

        $scope.workingList = [];

        $scope.selectedService;
        $scope.myHtml;
        $scope.info;
        $scope.progress = {};

        $scope.$on('update.selectedService', function(event, service) {
            $scope.selectedService = service;
            $scope.myHtml = 'loading..';
            delete $scope.info;

            WpsService.getDescription(service.attributes.name).then(function(data){

                data["Identifier1"]=data.ProcessDescriptions.ProcessDescription.Identifier.__text.replace(/\./g,"__");
                data.ProcessDescriptions.ProcessDescription.Identifier1=data.ProcessDescriptions.ProcessDescription.Identifier.__text.replace(/\./g,"__");
                for(var i in data.ProcessDescriptions.ProcessDescription.DataInputs.Input){
                    if(data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i]._minOccurs=="0")
                    data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i].optional=true;
                    else
                    data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i].optional=false;
                }
                var details =  tpl_describeProcess(data);
                $scope.myHtml = $sce.trustAsHtml(details);
            });
        });

        $scope.launchProcessing = function() {
            console.log('Process..');
            var aProcess = $scope.selectedService.attributes.name;
            notify('Running '+aProcess+' service..');
            var iparams=[];
            var id1 = '#wps_' + aProcess;

            $(id1).find("input[type=text],select").each(function(){
                var lname=$(this).attr("id").replace(/wps_i_/g,"");
                console.log(lname);
                if($(this).is(":visible") && lname!=$(this).attr("id")){
                    iparams.push({
                        identifier: lname,
                        value: cropQuotes($(this).val()),
                        dataType: "string"
                    });
                }
            });

            $(id1).find("input[type=hidden]").each(function(){
                var lname=$(this).attr("id").replace(/wps_i_/g,"");
                console.log(lname);
                if($(this).parent().is(":visible") && lname!=$(this).attr("id")){
                    if($(this).val()==raster){
                        iparams.push({
                            identifier: lname,
                            href: mapWCSUrl+"?service=WCS&version=2.0.0&request=GetCoverage&CoverageId="+$(this).val(),
                            mimeType: "image/tiff"
                        });
                    }
                    else{
                        iparams.push({
                            identifier: lname,
                            href: mapWFSUrl+"?service=WFS&version=1.0.0&request=GetFeature&srsName=EPSG:4326&typename="+$(this).val(),
                            mimeType: "text/xml"
                        });
                    }
                }
            });

            var oparams=[];
            $(id1).find("select").each(function(){
                var lname=$(this).attr("id").replace(/format_wps_o_/g,"");
                console.log(lname);
                if($(this).is(":visible") && lname!=$(this).attr("id")){
                    oparams.push({
                        identifier: lname,
                        mimeType: cropQuotes($(this).val()),
                        asReference: "true"
                    });
                }
            });

            console.log("----In ----");
            console.log(iparams);
            console.log("----Out ----");
            console.log(oparams);

            WpsService.execute(aProcess, iparams, oparams).then(function(data){
                notify(processName, ' service run successfully');
            }, function(error) {
                notify(error);
            });
        }

        function cropQuotes(val){
            return val.replace(/"/g, "");
        }

        $scope.isWpsLoaded = function(){
            return $scope.myHtml && $scope.myHtml != 'loading..';
        }

        $scope.$on('update.job.progress', function(event, percentage, processName) {
            $scope.progress[processName] = percentage;
        });

        function notify(text){
            $scope.info = text;
        }
        
        $scope.removeSelectedItem = function (field, item){
            var index = field.list.indexOf(item);
            field.list.splice(index, 1);
        }

        $scope.getNameShort = function(item){
            var label = item.title ? item.title : item.identifier; //when a geo-result is dropped
            if(!label){
                label = item.attributes.fname; //then its an output file
            }
            return label.substring(0, 8).concat('..');
        }

    }]);
});