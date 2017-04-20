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

        this.searchPolygon = { selectedArea: undefined, wkt: undefined };
        this.mapType = {active: 'MapBox'};
        this.searchLayerFeatures = new ol.Collection();
        this.resultLayerFeatures = new ol.Collection();
        this.basketLayerFeatures = new ol.Collection();

        this.getPolygonWkt = function(){
            return angular.copy(this.searchPolygon.wkt);
        };

        this.resetSearchPolygon = function(){
            this.searchPolygon.selectedArea = undefined;
            this.searchPolygon.wkt = undefined;
        };

        var selectedStyle = new ol.style.Style({
              fill: new ol.style.Fill({
                color: 'rgba(0,255,255,0.8)'
              }),
              stroke: new ol.style.Stroke({
                color: 'rgba(255,75,255,0.8)',
                width: 3
              }),
              image: new ol.style.Circle({
                  fill: new ol.style.Fill({
                    color: 'rgba(255,255,0,0.2)'
                  }),
                  radius: 5,
                  stroke: new ol.style.Stroke({
                    color: 'rgba(255,75,255,0.8)',
                    width: 3
                  })
              })
        });

        this.selectClick = new ol.interaction.Select({
            condition: ol.events.condition.click,
            toggleCondition: ol.events.condition.shiftKeyOnly,
            style: selectedStyle
        });

        return this;
    }]);
});
