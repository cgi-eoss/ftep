/**
 * @ngdoc service
 * @name ftepApp.SearchService
 * @description
 * # SearchService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('SearchService', ['ftepProperties', '$http', '$q', 'MessageService', 'GeoService', function (ftepProperties, $http, $q, MessageService, GeoService) {

        this.submit = function (searchParameters) {
            var deferred = $q.defer();

            searchParameters.resultsPerPage = 20;

            GeoService.spinner.loading = true;

            $http({
                method: 'GET',
                url: ftepProperties.URLv2 + '/search',
                params: searchParameters
            }).then(function (response) {
                var searchResults = response.data;

                // TODO Crude hack abusing GeoService cache - refactor search results -> display loop
                var transformedFeatures = [];
                for (var i = 0; i < searchResults.features.length; i++) {
                    var feature = searchResults.features[i];

                    var transformedFeature = {};
                    transformedFeature.title = transformedFeature.identifier = feature.properties.productIdentifier;
                    if (feature.geometry && feature.geometry.type === 'Polygon') {
                        transformedFeature.geo = feature.geometry;
                    }
                    transformedFeature.link = feature.properties.ftepUri;
                    transformedFeature.usable = feature.properties.ftepUsable;
                    transformedFeature.meta = feature.properties;
                    if (feature.properties.services && feature.properties.services.download && feature.properties.services.download.size) {
                        transformedFeature.size = feature.properties.services.download.size;
                    }

                    transformedFeatures.push(transformedFeature);
                }

                var transformedResults = [{
                    datasource: searchResults.parameters.repo,
                    results: {
                        totalResults: searchResults.page.totalElements,
                        // startIndex: -1,
                        page: searchResults.page.number,
                        _links: searchResults._links,
                        entities: transformedFeatures
                    }
                }];

                GeoService.setCache(transformedResults);
                GeoService.pagingData = {
                    currentPage: searchResults.page.number + 1,
                    pageSize: 20,
                    total: searchResults.page.totalElements,
                    apiV2Params: searchParameters
                };

                deferred.resolve(transformedResults);
            }).catch(function (error) {
                deferred.reject();
                MessageService.addError('Search failed', error);
            });

            return deferred.promise;
        };

        return this;
    }]);

});
