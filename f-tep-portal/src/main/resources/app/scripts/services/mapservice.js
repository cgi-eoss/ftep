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

        this.mapstore = {
            aoi: { selectedArea: undefined, wkt: undefined },
            type: {active: 'Satellite Street'},
            location: [0, 51.28],
            zoom: 4
        };
        this.searchLayerFeatures = new ol.Collection();
        this.resultLayerFeatures = new ol.Collection();
        this.basketLayerFeatures = new ol.Collection();

        this.getPolygonWkt = function(){
            return angular.copy(this.mapstore.aoi.wkt);
        };

        this.resetSearchPolygon = function(){
            this.mapstore.aoi.selectedArea = undefined;
            this.mapstore.aoi.wkt = undefined;
        };

        return this;
    }]);
});
