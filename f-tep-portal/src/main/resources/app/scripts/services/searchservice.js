/**
 * @ngdoc service
 * @name ftepApp.SearchService
 * @description
 * # SearchService
 * Service in the ftepApp.
 */
define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    'use strict';

    ftepmodules.service('SearchService', ['ftepProperties', '$http', '$q', 'MessageService', 'traverson', function (ftepProperties, $http, $q, MessageService, traverson) {

        var _this = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();

        this.spinner = { loading: false };

        this.params = {
            savedSearch: {},
            selectedCatalog: {},
            pagingData: {},
            results: {}
        };

        /* Get Groups for share functionality to fill the combobox */
        this.getSearchParameters = function(){
            var deferred = $q.defer();
            halAPI.from(rootUri + '/search/parameters')
                .newRequest()
                .getResource()
                .result
                .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Search Data', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        /* Get search name to display in the bottombar tab */
        this.getSearchName = function() {
            if(this.params.savedSearch.mission) {
                return ': ' + this.params.savedSearch.mission;
            } else {
                return '';
            }
        };

        /* Get results by page */
        this.getResultsPage = function (url) {
            this.spinner.loading = true;
            var deferred = $q.defer();

            halAPI.from(url)
                .newRequest()
                .getResource()
                .result
                .then(
                function (response) {
                    _this.spinner.loading = false;
                    deferred.resolve(response);
                }, function (error) {
                    _this.spinner.loading = false;
                    MessageService.addError('Search failed', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        /* Submit search and get results */
        this.submit = function(searchParameters){
            this.spinner.loading = true;
            var deferred = $q.defer();

            halAPI.from(rootUri + '/search')
                .newRequest()
                .withRequestOptions({ qs: searchParameters })
                .getResource()
                .result
                .then(
                function (response) {
                    _this.spinner.loading = false;
                    deferred.resolve(response);
                }, function (error) {
                    _this.spinner.loading = false;
                    MessageService.addError('Search failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        return this;
    }]);
});
