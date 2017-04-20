/**
* @ngdoc function
* @name ftepApp.controller:MapCtrl
* @description
* # MapCtrl
* Controller of the ftepApp
*/
define(['../../ftepmodules', 'ol', 'xml2json', 'clipboard'], function (ftepmodules, ol, X2JS, clipboard) {
    'use strict';

    ftepmodules.controller('MapCtrl', [ '$scope', '$rootScope', '$mdDialog', 'ftepProperties', 'MapService',
                                    function($scope, $rootScope, $mdDialog, ftepProperties, MapService) {

        var EPSG_3857 = "EPSG:3857", //Spherical Mercator projection used by most web map applications (e.g Google, OpenStreetMap, Mapbox).
            EPSG_4326 = "EPSG:4326"; //Standard coordinate system used in cartography, geodesy, and navigation (including GPS).

        var SHAPE = {
                NONE: {},
                POLYGON : {type: 'Polygon', img: 'images/polygon.png', location: 'polygon-selection', name:"Polygon"},
                BOX : {type: 'LineString', img: 'images/box.png', location: 'box-selection', name:"Square"}
        };

        $scope.drawType = SHAPE.NONE;
        var searchLayerFeatures = MapService.searchLayerFeatures;
        $scope.mapType = MapService.mapType;
        $scope.searchPolygon = MapService.searchPolygon;

        var resultStyle = new ol.style.Style({
            fill: new ol.style.Fill({
                color: 'rgba(255, 255, 255, 0.15)'
            }),
            stroke: new ol.style.Stroke({
                color: '#ffcc33',
                width: 2
            })
        });

        var searchBoxStyle = new ol.style.Style({
            fill: new ol.style.Fill({
                color: 'rgba(255, 255, 255, 0.2)'
              }),
            stroke: new ol.style.Stroke({
                color: '#ed0707',
                width: 2,
                lineDash: [15, 5]
            })
        });

        /** MAP LAYER TYPES **/
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
        /** END OF MAP LAYER TYPES **/

        /** MAP INTERACTIONS FOR THE POLYGONS **/
        var modify = new ol.interaction.Modify({
            features: searchLayerFeatures,
            // the SHIFT key must be pressed to delete vertices, so
            // that new vertices can be drawn at the same position
            // of existing vertices
            deleteCondition: function(event) {
              return ol.events.condition.shiftKeyOnly(event) &&
                  ol.events.condition.singleClick(event);
            }
        });

        modify.on('modifyend',function(e){
            updateSearchPolygon(e.features.getArray()[0].getGeometry());
        });

        var dragAndDropInteraction = new ol.interaction.DragAndDrop({
            formatConstructors: [
                ol.format.KML
                //TODO other formats?
            ]
        });

        var selectClick = MapService.selectClick;

        selectClick.on('select', function(evt) {
            var selectedItems = [];
            for(var i = 0; i < selectClick.getFeatures().getLength(); i++){
                if(selectClick.getFeatures().item(i) && selectClick.getFeatures().item(i).get('data')){
                    selectedItems.push(selectClick.getFeatures().item(i).get('data'));
                }
            }
            $rootScope.$broadcast('map.item.toggled', selectedItems);
        });

        /** END OF MAP INTERACTIONS FOR THE POLYGONS **/

        /** MOUSE POSITION COORDINATES **/
        var mousePositionControl = new ol.control.MousePosition({
            coordinateFormat: ol.coordinate.createStringXY(4),
            projection: EPSG_4326,
            undefinedHTML: '&nbsp;'
        });
        /** END OF MOUSE POSITION COORDINATES **/

        $scope.draw = function(shape, opt_options) {

            var options = opt_options || {};

            var button = document.createElement('button');
            button.innerHTML = '<img class="map-control" src="' + shape.img + '"/>';

            var handleSelection = function() {
                if($scope.drawType != shape){
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
                });

                draw.on('drawend', function (event) {
                    $scope.drawType = SHAPE.NONE;
                    $scope.map.removeInteraction(draw);
                    updateSearchPolygon(event.feature.getGeometry());
                });

                $scope.map.addInteraction(draw);
            }
        }
        addInteraction();

        $scope.setMapType = function(newType) {
            if(newType === 'OSM') {
                $scope.map.removeLayer(layerMapBox);
                $scope.map.addLayer(layerOSM);
                $scope.mapType.active = "Open Street";
            } else if (newType === 'MB') {
                $scope.map.removeLayer(layerOSM);
                $scope.map.addLayer(layerMapBox);
                $scope.mapType.active = "MapBox";
            }

            $scope.map.addLayer(searchAreaLayer);
        };

        var searchAreaLayer = new ol.layer.Vector({
          source: new ol.source.Vector({
                  features: searchLayerFeatures
              }),
              style: searchBoxStyle
        });
        $scope.map.addLayer(searchAreaLayer);

        function updateSearchPolygon(geom, editedWkt, refit){
            var polygonWSEN = ol.extent.applyTransform(geom.getExtent(), ol.proj.getTransform(EPSG_3857, EPSG_4326));
            $rootScope.$broadcast('polygon.drawn', polygonWSEN);
            if(refit && refit === true){
                $scope.map.getView().fit(geom.getExtent(), /** @type {ol.Size} */ ($scope.map.getSize()));
            }

            $scope.searchPolygon.selectedArea = geom.clone();
            var area = angular.copy($scope.searchPolygon.selectedArea);
            $scope.searchPolygon.wkt = (editedWkt ? editedWkt : new ol.format.WKT().writeGeometry(area.transform(EPSG_3857, EPSG_4326)));
        }

        // Copy the coordinates to clipboard which can be then pasted to service input fields
        $scope.copyPolygon = function(){
            if($scope.searchPolygon.wkt){
                clipboard.copy($scope.searchPolygon.wkt);
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
            $rootScope.$broadcast('polygon.drawn', undefined);
        };

        //Dialog to enable editing the polygon coordinates manually (coordinates shown in EPSG_4326 projection)
        $scope.editPolygonDialog = function($event, polygon) {
            $event.stopPropagation();
            $event.preventDefault();
            function EditPolygonController($scope, $mdDialog, MapService) {
                $scope.polygon = { wkt: angular.copy(MapService.getPolygonWkt()), valid: false};
                if(polygon.selectedArea) {
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

        /* Custom KML reader, when file has GroundOverlay features, which OL3 doesn't support */
        var reader = new FileReader();
        reader.onload = function(){
          var data = reader.result;
          if(data.indexOf('GroundOverlay') > -1){
              var x2js = new X2JS();
              var kmlJson = x2js.xml_str2json(data);
              console.log('<GroundOverlay>');
              console.log(kmlJson.kml.Document.GroundOverlay.LatLonBox);
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
              $scope.map.getView().fit(polyExtent, /** @type {ol.Size} */ ($scope.map.getSize()));

              $rootScope.$broadcast('polygon.drawn', polygonWSEN);
          }
        };

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
                var polygonWSEN = ol.extent.applyTransform(vectorSource.getExtent(), ol.proj.getTransform(EPSG_3857, EPSG_4326));
                $rootScope.$broadcast('polygon.drawn', polygonWSEN);
            }
        });

        /** RESULTS LAYER **/
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
            if(results){
                for(var folderNr = 0; folderNr < results.length; folderNr++){
                    var isLONLAT = true;
                    if(results[folderNr].datasource == 'CEDA2'){
                        isLONLAT = false;
                    }
                    if(results[folderNr].results && results[folderNr].results.entities && results[folderNr].results.entities.length > 0){
                        for(var i = 0; i < results[folderNr].results.entities.length; i++){
                            var item = results[folderNr].results.entities[i];
                            var lonlatPoints = [];
                            for(var k = 0; k < item.geo.coordinates.length; k++){
                                for(var m = 0; m < item.geo.coordinates[k].length; m++){
                                    var p = item.geo.coordinates[k][m];
                                    if(isLONLAT === false){
                                        p = p.reverse(); //We get the coordinates as LAT,LON, but ol3 needs LON,LAT - so reverse is needed.
                                    }
                                    lonlatPoints.push(p);
                                }
                            }
                            var pol = new ol.geom[item.geo.type]( [lonlatPoints] ).transform(EPSG_4326, EPSG_3857);

                            var resultItem =  new ol.Feature({
                                geometry: pol,
                                data: item
                            });

                            resultLayerFeatures.push(resultItem);
                        }
                        $scope.map.getView().fit(resultsLayer.getSource().getExtent(), $scope.map.getSize());
                    }
                }
            }
        });

        $scope.$on('results.item.selected', function(event, item, selected) {
            selectItem(item, selected);
        });

        function selectItem(item, selected){
            var features = resultsLayer.getSource().getFeatures();
            for(var i = 0; i < features.length; i++){
                if((item.identifier && item.identifier == features[i].get('data').identifier)){
                    if(selected){
                        selectClick.getFeatures().push(features[i]);
                        $scope.map.getView().fit(features[i].getGeometry().getExtent(), $scope.map.getSize()); //center the map to the selected vector
                        var zoomLevel = 3;
                        if($scope.map.getView().getZoom() > 3){
                            zoomLevel = $scope.map.getView().getZoom()-2;
                        }
                        $scope.map.getView().setZoom(zoomLevel); //zoom out a bit, to show the location better
                    }
                    else {
                        selectClick.getFeatures().remove(features[i]);
                    }
                    break;
                }
            }
        }

        $scope.$on('results.select.all', function(event, selected) {
            selectAll(selected);
        });

        function selectAll(selected){
            if(resultsLayer){
                while(selectClick.getFeatures().getLength() > 0){
                    selectClick.getFeatures().pop();
                }
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

        /** END OF RESULTS LAYER **/

        /** BASKET LAYER **/
        var basketLayerFeatures = MapService.basketLayerFeatures;
        var basketLayer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: basketLayerFeatures
            }),
            style: resultStyle
        });

        $scope.$on('upload.basket', function(event, basketFiles) {
            $scope.map.removeLayer(resultsLayer);
            basketLayerFeatures.clear();
            if(basketFiles && basketFiles.length > 0){
                for(var i = 0; i < basketFiles.length; i++){
                    var item = basketFiles[i];
                    if(item.geometry){
                        var lonlatPoints = [];
                        for(var k = 0; k < item.geometry.length; k++){
                           for(var m = 0; m < item.geometry[k].length; m++){
                              var p = angular.copy(item.geometry[k][m]);
                              p.reverse(); //We get the coordinates as LAT,LON, but ol3 needs LON,LAT - so reverse is needed. // TODO still needed?
                              lonlatPoints.push(p);
                           }
                        }
                        var pol = new ol.geom['Polygon']( [lonlatPoints] ).transform(EPSG_4326, EPSG_3857); //TODO type
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

        // WMS layer to show products on map
        var productLayers = [];
        $scope.$on('show.products', function(event, jobId, products) {
            for(var i = 0; i < productLayers.length; i++){
                $scope.map.removeLayer(productLayers[i]);
            }
            productLayers = [];
            for(var j = 0; j < products.length; j++){
                if(products[j].attributes.fname){
                    var name = products[j].attributes.fname;
                    if(name.indexOf('.') > -1){
                        name = name.substring(0, name.indexOf('.'));
                    }
                    var source = new ol.source.ImageWMS({
                        url: ftepProperties.WMS_URL + '/' + jobId + '/wms',
                        params: {
                            layers: jobId + ':' + name,
                            service: 'WMS',
                            VERSION: '1.1.0',
                            FORMAT: 'image/png'
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
        });

        window.onresize = function() {
            $scope.map.updateSize();
        };


    }]);
});
