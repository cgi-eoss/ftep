/**
 * @ngdoc service
 * @name ftepApp.MapService
 * @description
 * # MapService
 * Service in the ftepApp.
 */
define(['../ftepmodules', 'ol'], function (ftepmodules, ol) {
    'use strict';

    ftepmodules.service('MapService', [function () {

        this.aoi = { selectedArea: undefined, wkt: undefined };
        this.mapType = {active: 'MapBox'};
        this.searchLayerFeatures = new ol.Collection();
        this.resultLayerFeatures = new ol.Collection();
        this.basketLayerFeatures = new ol.Collection();

        this.getPolygonWkt = function(){
            return angular.copy(this.aoi.wkt);
        };

        this.resetSearchPolygon = function(){
            this.aoi.selectedArea = undefined;
            this.aoi.wkt = undefined;
        };

        return this;
    }]);
});
