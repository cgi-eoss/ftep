/**
 * @ngdoc service
 * @name ftepApp.GeoService
 * @description
 * # GeoService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('GeoService', [ '$http', '$rootScope', 'ftepProperties', '$q', 'MessageService',
                                        function ($http, $rootScope, ftepProperties, $q, MessageService) {

        /** Set the header defaults **/
        $http.defaults.headers.post['Content-Type'] = 'application/json';
        $http.defaults.withCredentials = true;

        /* private methods-variables */
        var resultCache;
        var ITEMS_PER_PAGE = 20;
        var MAX_ITEMS_ALLOWED = 100000;

        this.getMaxItemsAllowed = function(){
            return MAX_ITEMS_ALLOWED;
        };

        function setCache(results){
            if(results && results.length > 0 && results[0].results.totalResults > 0){
                resultCache = results;
            }
            else{
                resultCache = {};
            }
        }
        /* End of private methods-variables */

        this.getGeoResults = function(pageNumber){
            this.spinner.loading = true;

            if(pageNumber){
                this.pagingData.currentPage = pageNumber;
            }
            else{
                this.pagingData.currentPage = 1;
            }

            // set times to midnight, and increment end date by one day to make it inclusive
            var start = angular.copy(this.searchParameters.startTime);
            var end = angular.copy(this.searchParameters.endTime);

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
                    startPage: this.pagingData.currentPage,
                    maximumRecords: ITEMS_PER_PAGE,
                    sat: this.dataSources[0].id === this.searchParameters.selectedDatasource.id,
                    tep: this.dataSources[1].id === this.searchParameters.selectedDatasource.id,
                    ref: this.dataSources[2].id === this.searchParameters.selectedDatasource.id
            }; //TODO ref data

            if(this.searchParameters.polygon){
                var bboxVal = [];
                for(var i = 0; i < this.searchParameters.polygon.length; i++){
                    bboxVal[i] = this.searchParameters.polygon[i].toFixed(0);
                }
                params.bbox = bboxVal.toString();
            }

            if (this.searchParameters.mission) {
                params.mission = this.searchParameters.mission.name;

                if (this.searchParameters.mission.name.indexOf('1') > -1 && this.searchParameters.polarisation) {
                    params.polarisation = this.searchParameters.polarisation.label;
                }
                else if (this.searchParameters.mission.name.indexOf('2') > -1) {
                    params.maxCloudCoverPercentage = this.searchParameters.maxCloudCover;
                }
            }

            if(this.searchParameters.text &&  this.searchParameters.text != ''){
                params.name = this.searchParameters.text;
            }

            $http({
                method: 'GET',
                url: ftepProperties.URL + '/search',
                params: params,
            }).
            then(function(response) {
                setCache(response.data.data);
                $rootScope.$broadcast('update.geoResults', response.data.data);

                //When result count exceeds the limit, notify user with a message
                if(response.data.data[0].results.totalResults > MAX_ITEMS_ALLOWED){
                    MessageService.addWarning('Too many results', 'Search results limited to ' + MAX_ITEMS_ALLOWED +
                            '. Please refine the search parameters to get more precise results.');
                }
            }).
            catch(function(e) {
                this.spinner.loading = false;
                var errorMsg = '';
                switch(e.status){
                    case -1:
                        errorMsg = 'Session expired';
                        break;
                    default:
                        errorMsg = e.statusText;
                        break;
                }
                MessageService.addError('Search failed', errorMsg);
            });
        };

        this.getResultCache = function(){
            return resultCache;
        };

        // Default search time is a year until now
        var defaultStartTime = new Date();
        defaultStartTime.setMonth(defaultStartTime.getMonth() - 12);

        /** PRESERVE USER SELECTIONS **/
        this.searchParameters = {
                selectedDatasource: undefined,
                startTime: defaultStartTime,
                endTime: new Date(),
                polygon: undefined,
                text: undefined,
                mission: undefined,
                polarisation: undefined,
                maxCloudCover: 5 //default value of 5% for max cloudiness
        };

        this.resetSearchParameters = function(){
            var polygonCopy = this.searchParameters.polygon;
            this.searchParameters = {
                    selectedDatasource: undefined,
                    startTime: defaultStartTime,
                    endTime: new Date(),
                    polygon: polygonCopy,
                    text: undefined,
                    mission: undefined,
                    polarisation: undefined,
                    maxCloudCover: 5
            };
            return this.searchParameters;
        };

        this.pagingData = {
                currentPage: 1,
                pageSize: ITEMS_PER_PAGE,
                total: 0
        };

        this.params = {
                selectedResultItems: []
        };

        /** END OF PRESERVE USER SELECTIONS **/

        this.dataSources = [
            {
                id: 1,
                name: "Satellite",
                icon: "satellite",
                description: "All Satellite data",
                fields: {date: true, mission: true}
            }, {
                id: 2,
                name: "Existing Products",
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
                name: "Sentinel-1",
                id: 0
            }, {
                name: "Sentinel-2",
                id: 1
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

        this.getResultsNameExtention = function(){
            var nameExtention = '';
            if(this.searchParameters.mission){
                nameExtention = ': ' + this.searchParameters.mission.name;
            }
            else if(this.searchParameters.selectedDatasource){
                nameExtention = ': ' + this.searchParameters.selectedDatasource.name;
            }
            return nameExtention;
        };

        return this;
    }]);

});
