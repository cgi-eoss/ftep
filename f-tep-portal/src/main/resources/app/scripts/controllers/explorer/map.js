/**
 * @ngdoc function
 * @name ftepApp.controller:MapCtrl
 * @description
 * # MapCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules', 'ol', 'x2js', 'clipboard'], function (ftepmodules, ol, X2JS, clipboard) {

    ftepmodules.controller('MapCtrl', [ '$scope', '$rootScope', '$mdDialog', 'ftepProperties', 'MapService', 'SearchService', '$timeout', function($scope, $rootScope, $mdDialog, ftepProperties, MapService, SearchService, $timeout) {

        var EPSG_3857 = "EPSG:3857", // Spherical Mercator projection used by most web map applications (e.g Google, OpenStreetMap, Mapbox).
            EPSG_4326 = "EPSG:4326"; // Standard coordinate system used in cartography, geodesy, and navigation (including GPS).

        var SHAPE = {
            NONE: {},
            POLYGON : {type: 'Polygon', img: 'images/polygon.png', location: 'polygon-selection', name:"Polygon"},
            BOX : {type: 'LineString', img: 'images/box.png', location: 'box-selection', name:"Square"}
        };

        $scope.drawType = SHAPE.NONE;
        var searchLayerFeatures = MapService.searchLayerFeatures;
        $scope.mapType = MapService.mapType;
        $scope.aoi = MapService.aoi;

        /** ----- MAP STYLES TYPES ----- **/
        /* Red highlight */
        var resultStyle = new ol.style.Style({
            fill: new ol.style.Fill({ color: 'rgba(255,128,171,0.2)' }),
            stroke: new ol.style.Stroke({ color: 'rgba(255,64,129,0.6)', width: 2 }),
            image: new ol.style.Circle({
                fill: new ol.style.Fill({
                  color: 'rgba(255,128,171,0.2)'
                }),
                radius: 5,
                stroke: new ol.style.Stroke({
                  color: 'rgba(255,64,129,0.6)',
                  width: 3
                })
            })
        });

        /* Blue highlight */
        var searchBoxStyle = new ol.style.Style({
            fill: new ol.style.Fill({ color: 'rgba(188,223,241,0.4)' }),
            stroke: new ol.style.Stroke({ color: 'rgba(49,112,143,0.4)', width: 2, lineDash: [15, 5] })
        });
        /** ----- END OF MAP STYLES TYPES ----- **/

        /** ----- MAP LAYER TYPES ----- **/
        var layerOSM = new ol.layer.Tile({
            name: 'Test_OSM',
            source: new ol.source.OSM()
        });

        var layerMapBox = new ol.layer.Tile({
            source: new ol.source.XYZ({
                tileSize: [512, 512],
                url: ftepProperties.MAPBOX_URL
            })
        });
        /** ----- END OF MAP LAYER TYPES ----- **/

        /** ----- MAP INTERACTIONS FOR POLYGONS ----- **/
        var modify = new ol.interaction.Modify({
            features: searchLayerFeatures,
            /* the SHIFT key must be pressed to delete vertices, so that new
               vertices can be drawn at the same position of existing vertices */
            deleteCondition: function(event) {
              return ol.events.condition.shiftKeyOnly(event) &&
                  ol.events.condition.singleClick(event);
            }
        });

        modify.on('modifyend',function(evt){
            updateSearchPolygon(evt.features.getArray()[0].getGeometry());
        });

        var dragAndDropInteraction = new ol.interaction.DragAndDrop({
            formatConstructors: [
                ol.format.KML
                //TODO other formats?
            ]
        });

        /* Green highlight */
        var selectClick = new ol.interaction.Select({
            condition: ol.events.condition.click,
            toggleCondition: ol.events.condition.shiftKeyOnly,
            style: new ol.style.Style({
                fill: new ol.style.Fill({
                    color: 'rgba(174,213,129,0.8)'
                }),
                stroke: new ol.style.Stroke({
                    color: 'rgba(85,139,47,0.8)',
                    width: 3
                }),
                image: new ol.style.Circle({
                    fill: new ol.style.Fill({
                        color: 'rgba(174,213,129,0.8)'
                    }),
                    radius: 5,
                    stroke: new ol.style.Stroke({
                        color: 'rgba(85,139,47,0.8)',
                        width: 3
                    })
                })
            }),
            filter: function(feature, layer) {
                return feature.get('data') !== undefined;
            }
        });

        selectClick.on('select', function(evt) {
            var selectedItems = [];
            for(var i = 0; i < selectClick.getFeatures().getLength(); i++){
                if(selectClick.getFeatures().item(i) && selectClick.getFeatures().item(i).get('data')){
                    selectedItems.push(selectClick.getFeatures().item(i).get('data'));
                }
            }
            $rootScope.$broadcast('map.item.toggled', selectedItems);
            // Refresh Map: avoid invisible-selection glitch and non-refreshed selection problem
            $scope.map.changed();
            resultsLayer.changed();
        });

        /** ----- END OF MAP INTERACTIONS FOR POLYGONS ----- **/

        /** ----- MOUSE POSITION COORDINATES ----- **/
        var mousePositionControl = new ol.control.MousePosition({
            coordinateFormat: ol.coordinate.createStringXY(4),
            projection: EPSG_4326,
            undefinedHTML: '&nbsp;'
        });
        /** ----- END OF MOUSE POSITION COORDINATES ----- **/

        /** ----- MAP CONFIG & DRAW INTERACTION ----- **/
        $scope.draw = function(shape, opt_options) {

            var options = opt_options || {};

            var button = document.createElement('button');
            button.innerHTML = '<img class="map-control" src="' + shape.img + '"/>';

            var handleSelection = function() {
                if($scope.drawType !== shape){
                    $scope.drawType = shape;
                    $scope.map.removeInteraction(draw);
                    addInteraction();
                }
            };

            button.addEventListener('click', handleSelection, false);
            button.addEventListener('touchstart', handleSelection, false);

            var element = document.createElement('div');
            element.className = shape.location + ' ol-unselectable ol-control';
            element.setAttribute("title", "Draw " + shape.name);
            element.appendChild(button);

            ol.control.Control.call(this, {
                element: element,
                target: options.target
            });
        };

        ol.inherits($scope.draw, ol.control.Control);

        $scope.map = new ol.Map({
            interactions: ol.interaction.defaults().extend([modify, dragAndDropInteraction, selectClick]),
            controls: ol.control.defaults({
                attributionOptions: ({
                    collapsible: false
                })
            }).extend([
                new $scope.draw(SHAPE.POLYGON),
                new $scope.draw(SHAPE.BOX),
                new ol.control.ScaleLine(),
                mousePositionControl
            ]),
            target: 'map',
            layers: [layerMapBox],
            view: new ol.View({
                center: ol.proj.fromLonLat([0, 51.28]),
                zoom: 4
            })
        });

        var draw; // global so we can remove it later
        function addInteraction() {
            if(droppedFileLayer){
                $scope.map.removeLayer(droppedFileLayer);
            }
            if($scope.drawType !== SHAPE.NONE){
                var geometryFunction, maxPoints;
                if($scope.drawType === SHAPE.BOX){
                    maxPoints = 2;
                    geometryFunction = function(coordinates, geometry) {
                        if (!geometry) {
                            geometry = new ol.geom.Polygon(null);
                        }
                        var start = coordinates[0];
                        var end = coordinates[1];
                        geometry.setCoordinates([
                            [start, [start[0], end[1]], end, [end[0], start[1]], start]
                        ]);
                        return geometry;
                    };
                }
                draw = new ol.interaction.Draw({
                    features: searchLayerFeatures,
                    type: ($scope.drawType.type) /** @type {ol.geom.GeometryType} */ ,
                    geometryFunction: geometryFunction,
                    maxPoints: maxPoints
                });

                draw.on('drawstart', function (event) {
                    if(searchAreaLayer){
                        searchAreaLayer.getSource().clear();
                    }
                    selectClick.setActive(false);
                });

                draw.on('drawend', function (event) {
                    $scope.drawType = SHAPE.NONE;
                    $scope.map.removeInteraction(draw);
                    updateSearchPolygon(event.feature.getGeometry());
                    setTimeout(function () {
                        selectClick.setActive(true);
                    }, 300);
                });

                $scope.map.addInteraction(draw);
            }
        }
        addInteraction();

        $scope.setMapType = function(newType) {
            if(newType === 'OSM' && $scope.mapType.active !== "Open Street") {
                $scope.map.removeLayer(layerMapBox);
                $scope.map.getLayers().insertAt(0, layerOSM);
                $scope.mapType.active = "Open Street";
            } else if (newType === 'MB' &&  $scope.mapType.active !== "MapBox") {
                $scope.map.removeLayer(layerOSM);
                $scope.map.getLayers().insertAt(0, layerMapBox);
                $scope.mapType.active = "MapBox";
            }
        };
        /** ----- END OF MAP CONFIG & DRAW INTERACTION ----- **/

        /** ----- SEARCH LAYER ----- **/
         var searchAreaLayer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: searchLayerFeatures
            }),
            style: searchBoxStyle
        });
        $scope.map.addLayer(searchAreaLayer);

        function updateSearchPolygon(geom, editedWkt, refit){
            $scope.aoi.selectedArea = geom.clone();

            //set the WKT when editing AOI manually
            var area = angular.copy($scope.aoi.selectedArea);
            $scope.aoi.wkt = (editedWkt ? editedWkt : new ol.format.WKT().writeGeometry(area.transform(EPSG_3857, EPSG_4326)));

            // re-fit when user has edited AOI manually
            if(refit && refit === true){
                $scope.map.getView().fit(geom.getExtent(), /** @type {ol.Size} */ ($scope.map.getSize()));
            }

            // If no digest/apply is in progress, trigger it to update bound components
            if (!$scope.$$phase) {
                $scope.$apply();
            }
        }
        /** ----- END OF SEARCH LAYER ----- **/

        /* ----- MAP BUTTONS ----- */
        // Copy the coordinates to clipboard which can be then pasted to service input fields
        $scope.copyPolygon = function(){
            if($scope.aoi.wkt){
                clipboard.copy($scope.aoi.wkt);
            }
        };

        $scope.clearSearchPolygon = function(){
            if(droppedFileLayer){
                $scope.map.removeLayer(droppedFileLayer);
            }
            if(searchAreaLayer){
                searchAreaLayer.getSource().clear();
            }
            MapService.resetSearchPolygon();
            $scope.drawType = SHAPE.NONE;
        };

        // Dialog to enable editing the polygon coordinates manually (coordinates shown in EPSG_4326 projection)
        $scope.editPolygonDialog = function($event) {
            $event.stopPropagation();
            $event.preventDefault();
            function EditPolygonController($scope, $mdDialog, MapService) {
                $scope.polygon = { wkt: MapService.getPolygonWkt(), valid: false};
                if(MapService.aoi.selectedArea) {
                    $scope.polygon.valid = true;
                }
                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
                $scope.updatePolygon = function(searchPolygonWkt){
                    if(searchAreaLayer){
                        searchAreaLayer.getSource().clear();
                    }
                    if(searchPolygonWkt && searchPolygonWkt !== ''){
                        var newPol = new ol.format.WKT().readFeature(searchPolygonWkt, {
                            dataProjection: EPSG_4326,
                            featureProjection: EPSG_3857
                        });
                        searchAreaLayer.getSource().addFeature(newPol);

                        updateSearchPolygon(newPol.getGeometry(), searchPolygonWkt, true);
                    }
                    $mdDialog.hide();
                };
                $scope.validateWkt = function(wkt){
                    try{
                        new ol.format.WKT().readFeature(wkt, {
                            dataProjection: EPSG_4326,
                            featureProjection: EPSG_3857
                        });
                        $scope.polygon.valid = true;
                    }
                    catch(error){
                        $scope.polygon.valid = false;
                    }
                };
            }
            EditPolygonController.$inject = ['$scope', '$mdDialog', 'MapService'];
            $mdDialog.show({
                controller: EditPolygonController,
                templateUrl: 'views/explorer/templates/editpolygon.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        $scope.clearMap = function() {
            $scope.clearSearchPolygon();
            SearchService.params.geoResults = [];
            if (resultsLayer) {
                resultsLayer.getSource().clear();
                // Also removing the highlighted results from the Map
                selectClick.getFeatures().clear();
            }
            if (basketLayer) {
                basketLayer.getSource().clear();
            }
            if (productLayers) {
                for (var i = 0; i < productLayers.length; i++) {
                    $scope.map.removeLayer(productLayers[i]);
                }
                productLayers = [];
            }
            SearchService.params.geoResults = [];
            $scope.map.getView().setZoom(4);
            $scope.map.getView().setCenter(ol.proj.fromLonLat([0, 51.28]));
            $rootScope.$broadcast('map.cleared');
        };

        /* Clear map when results is reset */
        $scope.$on('results.cleared', function () {
            $scope.clearMap();
        });
        /* ----- END OF MAP BUTTONS ----- */

        /* ----- VARIOUS MAP FUNCTIONS ----- */
        /* Custom KML reader, when file has GroundOverlay features, which OL3 doesn't support */
        var reader = new FileReader();
        reader.onload = function(){
            var data = reader.result;
            if(data.indexOf('GroundOverlay') > -1){
                var x2js = new X2JS();
                var kmlJson = x2js.xml2js(data);
                var latlonbox = kmlJson.kml.Document.GroundOverlay.LatLonBox;
                var polygonWSEN = [parseFloat(latlonbox.west), parseFloat(latlonbox.south), parseFloat(latlonbox.east), parseFloat(latlonbox.north)];
                var polyExtent = ol.proj.transformExtent(polygonWSEN, EPSG_4326, EPSG_3857);
                var pol =  ol.geom.Polygon.fromExtent(polyExtent);

                var kmlVector = new ol.source.Vector({
                    features: [new ol.Feature({
                        geometry: pol
                    })]
                });

                droppedFileLayer = new ol.layer.Vector({
                    source: kmlVector,
                    style: searchBoxStyle
                });

                $scope.map.addLayer(droppedFileLayer);
                updateSearchPolygon(pol, undefined, true);
            }
        };

        /* Update map on screen resize */
        window.onresize = function() {
            $scope.map.updateSize();
        };

        /* Fixes map not loading on slow connection speeds */
        $scope.updateMap = function() {
            $timeout(function () {
                $scope.map.updateSize();
            }, 1000);
        };

        /* Get geometry to display on map */
        function getOlGeometryObject(geometry) {
            if(geometry && geometry.coordinates) {
                if(geometry.type === 'Point'){
                    return new ol.geom[geometry.type](geometry.coordinates).transform(EPSG_4326, EPSG_3857);
                }
                else {
                    var lonlatPoints = [];
                    for(var k = 0; k < geometry.coordinates.length; k++){
                        for(var m = 0; m < geometry.coordinates[k].length; m++){
                            var point = geometry.coordinates[k][m];
                            lonlatPoints.push(point);
                        }
                    }
                    return new ol.geom[geometry.type]([lonlatPoints]).transform(EPSG_4326, EPSG_3857);
                }
            }
            return undefined;
        }
        /* ----- END OF VARIOUS MAP FUNCTIONS ----- */

        /* ----- DROPPED FILE LAYER ----- */
        var droppedFileLayer;
        dragAndDropInteraction.on('addfeatures', function(event) {
            if(droppedFileLayer){
                $scope.map.removeLayer(droppedFileLayer);
            }
            if(searchAreaLayer){
                searchAreaLayer.getSource().clear();
            }
            if(event.features.length < 1){
                //no features could be parsed, try a custom approach
                reader.readAsText(event.file);
            }
            else {
                var vectorSource = new ol.source.Vector({
                    features: event.features
                });
                droppedFileLayer = new ol.layer.Image({
                    opacity: 0.50,
                    source: new ol.source.ImageVector({
                        source: vectorSource,
                        style: searchBoxStyle
                    })
                });
                $scope.map.addLayer(droppedFileLayer);
                $scope.map.getView().fit(vectorSource.getExtent(), /** @type {ol.Size} */ ($scope.map.getSize()));
            }
        });
        /* ----- END OF DROPPED FILE LAYER ----- */

        /** ----- RESULTS LAYER ----- **/
        var resultLayerFeatures = MapService.resultLayerFeatures;
        var resultsLayer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: resultLayerFeatures
            }),
            style: resultStyle
        });
        $scope.map.addLayer(resultsLayer);

        $scope.$on('update.geoResults', function(event, results) {
            selectAll(false);
            resultLayerFeatures.clear();
            if(results && results.features && results.features.length > 0) {
                var zoomToPlace = false;
                for (var result in results.features) {
                    var item = results.features[result];
                    var resultItem = new ol.Feature({
                        data: item
                    });
                    if (item.geometry) {
                        resultItem.setGeometry(getOlGeometryObject(item.geometry));
                        zoomToPlace = true;
                    }
                    resultLayerFeatures.push(resultItem);
                }
                if (zoomToPlace) {
                    $scope.map.getView().fit(resultsLayer.getSource().getExtent(), $scope.map.getSize());
                }
            }
        });

        $scope.$on('results.item.selected', function(event, item, selected) {
            selectItem(item, selected);
        });

        function selectItem(item, selected) {
            var features = resultsLayer.getSource().getFeatures();
            for (var i in features) {
                var feature = features[i];
                if(item.properties.productIdentifier && item.properties.productIdentifier === feature.get('data').properties.productIdentifier){
                    if(selected){
                        selectClick.getFeatures().push(feature);
                        $scope.map.getView().fit(feature.getGeometry().getExtent(), $scope.map.getSize()); //center the map to the selected vector
                        var zoomLevel = 3;
                        if(feature.getGeometry() instanceof ol.geom.Point){
                            zoomLevel = 6;
                        }
                        else if($scope.map.getView().getZoom() > 3){
                            zoomLevel = $scope.map.getView().getZoom()-2;
                        }
                        $scope.map.getView().setZoom(zoomLevel); //zoom out a bit, to show the location better
                    }
                    else {
                        selectClick.getFeatures().remove(feature);
                    }
                    break;
                }
            }
            // Refresh Map: avoid invisible-selection glitch and non-refreshed selection problem
            $scope.map.changed();
            resultsLayer.changed();
        }

        $scope.$on('results.select.all', function(event, selected) {
            selectAll(selected);
        });

        function selectAll(selected){
            if(resultsLayer){
                selectClick.getFeatures().clear();
                if(selected){
                    for(var i = 0; i < resultsLayer.getSource().getFeatures().length; i++){
                        selectClick.getFeatures().push(resultsLayer.getSource().getFeatures()[i]);
                    }
                }
            }
        }

        $scope.$on('results.invert', function(event, items) {
            selectAll(false);
            for(var i = 0; i < items.length; i++){
                selectItem(items[i], true);
            }
        });
        /** ----- END OF RESULTS LAYER ----- **/

        /** ----- BASKET LAYER ----- **/
        var basketLayerFeatures = MapService.basketLayerFeatures;
        var basketLayer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: basketLayerFeatures
            }),
            style: resultStyle
        });

        $scope.$on('load.basket', function(event, basketFiles) {
            $scope.map.removeLayer(resultsLayer);
            basketLayerFeatures.clear();
            if(basketFiles && basketFiles.length > 0){
                for(var i = 0; i < basketFiles.length; i++){
                    var item = basketFiles[i];
                    if(item.metadata && item.metadata.geometry && item.metadata.geometry.coordinates){
                        var pol = getOlGeometryObject(item.metadata.geometry);
                        var resultItem =  new ol.Feature({
                             geometry: pol,
                             data: item
                        });
                        basketLayerFeatures.push(resultItem);
                    }
                }
                $scope.map.addLayer(basketLayer);
                $scope.map.getView().fit(basketLayer.getSource().getExtent(), $scope.map.getSize());
            }
        });

        $scope.$on('unload.basket', function(event) {
            $scope.map.removeLayer(basketLayer);
            $scope.map.addLayer(resultsLayer);
        });

        $scope.$on('databasket.item.selected', function(event, item, selected) {
            selectDatabasketItem(item, selected);
        });

        function selectDatabasketItem(item, selected){
            var features = basketLayer.getSource().getFeatures();

            for(var i = 0; i < features.length; i++) {
                if((item.id && item.id === features[i].get('data').properties.productIdentifier)){
                    if(selected){
                        selectClick.getFeatures().push(features[i]);
                        $scope.map.getView().fit(features[i].getGeometry().getExtent(), $scope.map.getSize());
                        var zoomLevel = 3;
                        if($scope.map.getView().getZoom() > 3) {
                            zoomLevel = $scope.map.getView().getZoom()-2;
                        }
                        $scope.map.getView().setZoom(zoomLevel);
                    } else {
                        selectClick.getFeatures().remove(features[i]);
                    }
                    break;
                }
            }

        }
        /** ----- END OF BASKET LAYER ----- **/

        /* ----- WMS LAYER ----- */
        var productLayers = [];

        $scope.$on('update.wmslayer', function(event, files) {
            /* Remove previous product layers */
            for (var i = 0; i < productLayers.length; i++) {
                $scope.map.removeLayer(productLayers[i]);
            }
            productLayers = [];

            if (files.length > 0) {
                // Create layer for each output file
                for (var i = 0; i < files.length; i++) {
                    if (files[i]._links && files[i]._links.wms) {
                        var source = new ol.source.ImageWMS({
                            url: files[i]._links.wms.href,
                            params: {
                                format: 'image/png'
                            },
                            projection: EPSG_3857
                        });
                        var productLayer = new ol.layer.Image({
                            source: source
                        });
                        productLayers.push(productLayer);
                        $scope.map.addLayer(productLayer);
                    }
                }

                // For example the Product Search result items apparently don't have this field
                if (files[files.length-1].metadata) {
                    // Zoom into place
                    var polygon = getOlGeometryObject(files[files.length-1].metadata.geometry);
                    $scope.map.getView().fit(polygon.getExtent(), $scope.map.getSize());
                }
            }
        });
        /* ----- END OF WMS LAYER ----- */

    }]);
});
