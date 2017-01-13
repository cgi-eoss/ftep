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
                    sat: this.dataSources[0].id === this.parameters.selectedDatasource.id,
                    tep: this.dataSources[1].id === this.parameters.selectedDatasource.id,
                    ref: false
            }; //TODO ref data

            if(this.parameters.polygon){
                var bboxVal = [];
                for(var i = 0; i < this.parameters.polygon.length; i++){
                    bboxVal[i] = this.parameters.polygon[i].toFixed(0);
                }
                params.bbox = bboxVal.toString();
            }

            if (this.parameters.mission) {
                params.platform = this.parameters.mission.name;

                if (this.parameters.mission.name.indexOf('1') > -1 && this.parameters.polarisation) {
                    params.polarisation = this.parameters.polarisation.label;
                }
                else if (this.parameters.mission.name.indexOf('2') > -1) {
                    params.maxCloudCoverPercentage = this.parameters.maxCloudCover;
                }
            }

            if(this.parameters.text &&  this.parameters.text != ''){
                params.name = this.parameters.text;
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

        this.getItemsPerPage = function(){
            return ITEMS_PER_PAGE;
        }

        // Default search time is a year until now
        var defaultStartTime = new Date();
        defaultStartTime.setMonth(defaultStartTime.getMonth() - 12);

        this.parameters = {selectedDatasource: undefined, startTime: defaultStartTime, endTime: new Date(), polygon: undefined, 
                text: undefined, pageNumber: 1, mission: undefined, polarisation: undefined, maxCloudCover: undefined};

        this.resetSearchParameters = function(){
            var pageCopy = this.parameters.pageNumber;
            var polygonCopy = this.parameters.polygon;
            this.parameters = {selectedDatasource: undefined, startTime: defaultStartTime, endTime: new Date(), polygon: polygonCopy, 
                    text: undefined, pageNumber: pageCopy, mission: undefined, polarisation: undefined, maxCloudCover: undefined};
            return this.parameters;
        }

        this.dataSources = [
            {
                id: 1,
                name: "Satellite",
                icon: "satellite",
                description: "All Satellite data",
                fields: {date: true, mission: true}
            }, {
                id: 2,
                name: "Product",
                icon: "local_library",
                description: "All preprocessed data",
                fields: {name: true}
            }, {
                id: 3,
                name: "Reference",
                icon: "streetview",
                description: "All user uploaded data",
                fields: {date: true, name: true}
            }
        ];

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
