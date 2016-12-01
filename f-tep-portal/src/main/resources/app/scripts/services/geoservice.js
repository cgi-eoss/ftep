/**
 * @ngdoc service
 * @name ftepApp.GeoService
 * @description
 * # GeoService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('GeoService', [ '$http', 'ftepProperties', '$q', function ($http, ftepProperties, $q) {

        /** Set the header defaults **/
        $http.defaults.headers.post['Content-Type'] = 'application/json';
        $http.defaults.withCredentials = true;

        /* private methods-variables */
        var resultCache;
        var ITEMS_PER_PAGE = 20;

        function setCache(results){
            if(results && results.length > 0 && results[0].results.totalResults > 0){
              resultCache = results;
            }
        }
        /* End of private methods-variables */

        this.getGeoResults = function(pageNumber){
            this.spinner.loading = true;
            var deferred = $q.defer();

            if(pageNumber){
                this.parameters.pageNumber = pageNumber;
            }
            else{
                this.parameters.pageNumber = 1;
            }

            // set times to midnight, and increment end date by one day to make it inclusive
            var start = angular.copy(this.parameters.startTime);
            var end = angular.copy(this.parameters.endTime);

            start.setHours(0,0,0,0);
            end.setHours(0,0,0,0);
            var userTimezoneOffset = new Date().getTimezoneOffset()*60000;
            start = new Date(start.getTime() - userTimezoneOffset);
            end = new Date(end.getTime() - userTimezoneOffset);
            end.setDate(end.getDate() +1);
            ///////////////////////////////

            var params = {
                    startDate: start,
                    endDate: end,
                    startPage: this.parameters.pageNumber,
                    maximumRecords: ITEMS_PER_PAGE,
                    sat: this.dataSources[0].id === this.selectedSource.value,
                    tep: this.dataSources[1].id === this.selectedSource.value,
                    ref: false
            }; //TODO ref data

            if(this.parameters.polygon){
                var bboxVal = [];
                for(var i = 0; i < this.parameters.polygon.length; i++){
                    bboxVal[i] = this.parameters.polygon[i].toFixed(0);
                }
                params.bbox = bboxVal.toString();
            }

            //TODO: Needs to work for multiple missions.
            if (this.parameters.mission) {
            /*  var allValid = true;
                params.platform = this.parameters.mission[0].name; // Temp fix!
                params.platform = this.parameters.mission.name;

                for (var mission in this.parameters.mission) {
                    //console.log(this.parameters.mission[mission].name);
                    if (this.parameters.mission[mission].name.indexOf('2') === -1) {
                        allValid = false;
                    }
                }

                if(allValid){
                    params.maxCloudCoverPercentage = this.parameters.mission.maxCloudCover;
                    params.minCloudCoverPercentage = this.parameters.mission.minCloudCover;
                    params.name = this.parameters.mission.text;
                } */

                params.platform = this.parameters.mission.name;

                if (this.parameters.mission.name.indexOf('2') > -1) {
                    params.minCloudCoverPercentage = this.parameters.mission.minCloudCover;
                    params.maxCloudCoverPercentage = this.parameters.mission.maxCloudCover;
                    params.name = this.parameters.mission.text;
                }

            }

            $http({
                method: 'GET',
                url: ftepProperties.URL + '/search',
                params: params,
            }).
            then(function(response) {
                setCache(response.data.data);
                deferred.resolve(response.data.data);
            }).
            catch(function(e) {
                deferred.reject();
            });

            return deferred.promise;
        };

        this.getResultCache = function(){
            return resultCache;
        };

        this.parameters = {startTime: undefined, endTime: undefined, polygon: undefined, pageNumber: 1, mission: undefined};

        this.dataSources = [
            {
                id: 1,
                name: "Satellite",
                icon: "satellite",
                description: "All Satellite data",
                value: false
            }, {
                id: 2,
                name: "Product",
                icon: "local_library",
                description: "All preprocessed data",
                value: false
            }, {
                id: 3,
                name: "Reference",
                icon: "streetview",
                description: "All user uploaded data",
                value: false
            }
        ];

        this.selectedSource = {
            value: 1
        };

        this.missions = [
            {
                name: "Sentinel-1A",
                id: 0
            }, {
                name: "Sentinel-1B",
                id: 1
            }, {
                name: "Sentinel-2A",
                id: 2
            }, {
                name: "Sentinel-2B",
                id: 3
            }
        ];

        this.polarisations = [
            {
                label: "None",
            }, {
                label: "HH",
                id: 0
            }, {
                label: "HV",
                id: 1
            }, {
                label: "VV",
                id: 2
            }, {
                label: "VH",
                id: 3
            }, {
                label: "HH+HV",
                id: 4
            }, {
                label: "VV+VH",
                id: 5
            }
        ];

        this.spinner = {loading: false};

        return this;
    }]);

});
