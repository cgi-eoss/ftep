/**
* @ngdoc function
* @name ftepApp.controller:MapCtrl
* @description
* # MapCtrl
* Controller of the ftepApp
*/
define(['../ftepmodules', 'ol', 'xml2json', 'clipboard'], function (ftepmodules, ol, X2JS, clipboard) {
    'use strict';

    ftepmodules.controller('MapCtrl', [ '$scope', '$rootScope', 'ftepProperties', function($scope, $rootScope, ftepProperties) {
        this.awesomeThings = [
          'HTML5 Boilerplate',
          'AngularJS',
          'Karma'
        ];

        /* Time Selection Slider*/
        var monthNames = [ "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                           "Jul", "Aug", "Sept", "Oct", "Nov", "Dec" ];
        var sliderCeil = new Date();
        var sliderFloor = new Date();
        sliderFloor.setFullYear(sliderFloor.getFullYear()-10);

        $scope.timeRangeSlider = {
                 options: {
                    floor: sliderFloor.getTime(),
                    ceil: sliderCeil.getTime(),
                    translate: function(value) {
                        var date = new Date(value);
                        var str = monthNames[date.getMonth()] + ' ' + date.getFullYear();
                        return str; //TODO agree on time format
                    },
                    getSelectionBarColor: function(value) {
                        return '#94dbef';
                    },
                    getPointerColor: function(value) {
                        return '#94dbef';
                    },
                    onChange: onSliderChange,
                    showTicks: 31104000000,
                    step: 2592000000 //TODO stepArray? step in ms?
                  },
                  minValue: sliderFloor.getTime(),
                  maxValue: sliderCeil.getTime()
        };

        function onSliderChange() {
            $rootScope.$broadcast('update.timeRange', { start: $scope.timeRangeSlider.minValue, end: $scope.timeRangeSlider.maxValue });
        }

        $scope.$on('update.timeslider', function(event, time) {
            if(time.start.getTime() < $scope.timeRangeSlider.options.floor) {
                $scope.timeRangeSlider.options.floor = time.start.getTime();
            }
            if(time.end.getTime() > $scope.timeRangeSlider.options.ceil) {
                $scope.timeRangeSlider.options.ceil = time.end.getTime();
            }
        });

        /* End of Time Selection Slider*/

        var SHAPE = {
                NONE: {},
                POLYGON : {type: 'Polygon', img: 'images/polygon.png', location: 'polygon-selection'},
                BOX : {type: 'LineString', img: 'images/box.png', location: 'box-selection'}
        };

        $scope.drawType = SHAPE.NONE;

        var draw; // global so we can remove it later
        var features = new ol.Collection();

        function addInteraction() {
            if(searchAreaLayer){
                searchAreaLayer.getSource().clear();
            }
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
                  features: features,
                  type: ($scope.drawType.type) /** @type {ol.geom.GeometryType} */ ,
                  geometryFunction: geometryFunction,
                  maxPoints: maxPoints
                });

                draw.on('drawend', function (event) {
                    $scope.drawType = SHAPE.NONE
                    $scope.map.removeInteraction(draw);

                    var extent = event.feature.getGeometry().getExtent();
                    var polygonWSEN = ol.extent.applyTransform(extent, ol.proj.getTransform("EPSG:3857", "EPSG:4326"));
                    $rootScope.$broadcast('polygon.drawn', polygonWSEN);
                    clipboard.copy(polygonWSEN.toString());
                });

                $scope.map.addInteraction(draw);
            }
        }

        addInteraction();

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
            element.appendChild(button);

            ol.control.Control.call(this, {
              element: element,
              target: options.target
            });
        };
        ol.inherits($scope.draw, ol.control.Control);

        var defaultStyle = {
                'Point': new ol.style.Style({
                  image: new ol.style.Circle({
                    fill: new ol.style.Fill({
                      color: 'rgba(255,255,0,0.5)'
                    }),
                    radius: 5,
                    stroke: new ol.style.Stroke({
                      color: '#ff0',
                      width: 1
                    })
                  })
                }),
                'LineString': new ol.style.Style({
                  stroke: new ol.style.Stroke({
                    color: '#f00',
                    width: 3
                  })
                }),
                'Polygon': new ol.style.Style({
                  fill: new ol.style.Fill({
                    color: 'rgba(0,255,255,0.5)'
                  }),
                  stroke: new ol.style.Stroke({
                    color: '#E9FF5D',
                    width: 1
                  })
                }),
                'MultiPoint': new ol.style.Style({
                  image: new ol.style.Circle({
                    fill: new ol.style.Fill({
                      color: 'rgba(255,0,255,0.5)'
                    }),
                    radius: 5,
                    stroke: new ol.style.Stroke({
                      color: '#f0f',
                      width: 1
                    })
                  })
                }),
                'MultiLineString': new ol.style.Style({
                  stroke: new ol.style.Stroke({
                    color: '#0f0',
                    width: 3
                  })
                }),
                'MultiPolygon': new ol.style.Style({
                  fill: new ol.style.Fill({
                    color: 'rgba(0,0,255,0.5)'
                  }),
                  stroke: new ol.style.Stroke({
                    color: '#00f',
                    width: 1
                  })
                })
        };

        var styleFunction = function(feature, resolution) {
            var featureStyleFunction = feature.getStyleFunction();
            if (featureStyleFunction) {
              return featureStyleFunction.call(feature, resolution);
            } else {
              return defaultStyle[feature.getGeometry().getType()];
            }
        };

        var dragAndDropInteraction = new ol.interaction.DragAndDrop({
            formatConstructors: [
                ol.format.KML
                //TODO other formats?
            ]
        });

        var layerOSM = new ol.layer.Tile({
            name: 'Test_OSM',
            source: new ol.source.OSM()
        })

        var layerMQ = new ol.layer.Tile({
            name: 'Test_MQ',
            source: new ol.source.MapQuest({
                layer: 'sat'
            })
        });

        var layerMapBox = new ol.layer.Tile({
            source: new ol.source.XYZ({
                tileSize: [512, 512],
                url: ftepProperties.MAPBOX_URL
            })
        });

        $scope.setMapType = function(newType) {
            if(newType == 'OSM') {
                $scope.map.removeLayer(layerMapBox);
                $scope.map.addLayer(layerOSM);
            } else if (newType == 'MB') {
                $scope.map.removeLayer(layerOSM);
                $scope.map.addLayer(layerMapBox);
            }
        }

        var layers = [layerMapBox];

        var mousePositionControl = new ol.control.MousePosition({
            coordinateFormat: ol.coordinate.createStringXY(4),
            projection: 'EPSG:4326',
            undefinedHTML: '&nbsp;'
          });

        $scope.map = new ol.Map({
            interactions: ol.interaction.defaults().extend([dragAndDropInteraction]),
            controls: ol.control.defaults({
                  attributionOptions: ({
                  collapsible: false
                })
            }).extend([
                new $scope.draw(SHAPE.POLYGON), new $scope.draw(SHAPE.BOX), mousePositionControl
            ]),
            target: 'map',
            layers: layers,
            view: new ol.View({
              center: ol.proj.fromLonLat([0, 51.28]),
              zoom: 4
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

        var searchAreaLayer = new ol.layer.Vector({
          source: new ol.source.Vector({features: features}),
          style: searchBoxStyle
        });
        $scope.map.addLayer(searchAreaLayer);

        var modify = new ol.interaction.Modify({
            features: features,
            // the SHIFT key must be pressed to delete vertices, so
            // that new vertices can be drawn at the same position
            // of existing vertices
            deleteCondition: function(event) {
              return ol.events.condition.shiftKeyOnly(event) &&
                  ol.events.condition.singleClick(event);
            }
          });
        $scope.map.addInteraction(modify);

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
              var polyExtent = ol.proj.transformExtent(polygonWSEN, "EPSG:4326", "EPSG:3857");
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
                        style: new ol.style.Style({
                            fill: new ol.style.Fill({
                                color: 'rgba(241, 0, 0, 0.3)'
                              }),
                            stroke: new ol.style.Stroke({
                                color: '#ed0707',
                                width: 2
                            })
                        })
                    })
                });
                $scope.map.addLayer(droppedFileLayer);
                console.log(vectorSource.getExtent());
                $scope.map.getView().fit(vectorSource.getExtent(), /** @type {ol.Size} */ ($scope.map.getSize()));
                var polygonWSEN = ol.extent.applyTransform(vectorSource.getExtent(), ol.proj.getTransform("EPSG:3857", "EPSG:4326"));
                $rootScope.$broadcast('polygon.drawn', polygonWSEN);
            }
        });

       var resultStyle = new ol.style.Style({
           fill: new ol.style.Fill({
               color: 'rgba(255, 255, 255, 0.4)'
           }),
           stroke: new ol.style.Stroke({
               color: '#ffcc33',
               width: 1
           })
       });

       var selectedStyles = {
               'Point': new ol.style.Style({
                 image: new ol.style.Circle({
                   fill: new ol.style.Fill({
                     color: 'rgba(255,255,0,0.2)'
                   }),
                   radius: 5,
                   stroke: new ol.style.Stroke({
                     color: '#ffcc33',
                     width: 1
                   })
                 })
               }),
               'LineString': new ol.style.Style({
                 stroke: new ol.style.Stroke({
                   color: '#E54127',
                   width: 3
                 })
               }),
               'Polygon': new ol.style.Style({
                 fill: new ol.style.Fill({
                   color: 'rgba(0,255,255,0.5)'
                 }),
                 stroke: new ol.style.Stroke({
                   color: '#EEFF49',
                   width: 1
                 })
               }),
               'MultiPoint': new ol.style.Style({
                 image: new ol.style.Circle({
                   fill: new ol.style.Fill({
                     color: 'rgba(255,0,255,0.5)'
                   }),
                   radius: 5,
                   stroke: new ol.style.Stroke({
                     color: '#EEFF49',
                     width: 1
                   })
                 })
               }),
               'MultiLineString': new ol.style.Style({
                 stroke: new ol.style.Stroke({
                   color: '#0f0',
                   width: 3
                 })
               }),
               'MultiPolygon': new ol.style.Style({
                 fill: new ol.style.Fill({
                   color: 'rgba(0,0,255,0.5)'
                 }),
                 stroke: new ol.style.Stroke({
                   color: '#EEFF49',
                   width: 1
                 })
               })
       };

        /* Display results on map */
       var resultsLayer;
       function addResultLayer(items) {

           var vectorSource = new ol.source.Vector({
               features: items
           });

           resultsLayer = new ol.layer.Vector({
               source: vectorSource,
               style: resultStyle
           });

           $scope.map.addLayer(resultsLayer);
        };

        $scope.$on('update.geoResults', function(event, results) {
            $scope.map.removeLayer(resultsLayer);
            var featureItems = [];
            for(var folderNr = 0; folderNr < results.length; folderNr++){
                var isLONLAT = true;
                if(results[folderNr].datasource == 'CEDA2'){
                    isLONLAT = false;
                }
                if(results[folderNr].results && results[folderNr].results.entities){
                    for(var i = 0; i < results[folderNr].results.entities.length; i++){
                        var item = results[folderNr].results.entities[i];
                        console.log('Geometry type: ', item.geo.type);
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
                        var pol = new ol.geom[item.geo.type]( [lonlatPoints] ).transform('EPSG:4326', 'EPSG:3857');

                        var resultItem =  new ol.Feature({
                            geometry: pol,
                            data: item
                        });

                        featureItems.push(resultItem);
                    }
                }
            }

            if(featureItems.length > 0){
                addResultLayer(featureItems);
                $scope.map.getView().fit(resultsLayer.getSource().getExtent(), $scope.map.getSize());
            }
        });

        var basketLayer;
        function addBasketLayer(items) {

            var vectorSource = new ol.source.Vector({
                features: items
            });

            basketLayer = new ol.layer.Vector({
                source: vectorSource,
                style: resultStyle
            });

            $scope.map.addLayer(basketLayer);
         };

        $scope.$on('upload.basket', function(event, basketFiles) {
            $scope.map.removeLayer(basketLayer);
            $scope.map.removeLayer(resultsLayer);
            var featureItems = [];
            for(var i = 0; i < basketFiles.length; i++){
                var item = basketFiles[i];
                if(item.attributes.properties && item.attributes.properties.geo){
                    if(item.attributes.properties.geo.type == 'polygon'){
                       item.attributes.properties.geo.type = 'Polygon';
                    }
                    var lonlatPoints = [];
                    for(var k = 0; k < item.attributes.properties.geo.coordinates.length; k++){
                       for(var m = 0; m < item.attributes.properties.geo.coordinates[k].length; m++){
                          var p = angular.copy(item.attributes.properties.geo.coordinates[k][m]);
                          p.reverse(); //We get the coordinates as LAT,LON, but ol3 needs LON,LAT - so reverse is needed.
                          lonlatPoints.push(p);
                       }
                    }
                    var pol = new ol.geom[item.attributes.properties.geo.type]( [lonlatPoints] ).transform('EPSG:4326', 'EPSG:3857');
                    var resultItem =  new ol.Feature({
                         geometry: pol,
                         data: item
                    });
                    featureItems.push(resultItem);
                }
            }
            if(featureItems.length > 0){
                addBasketLayer(featureItems);
                $scope.map.getView().fit(basketLayer.getSource().getExtent(), $scope.map.getSize());
            }
        });

        $scope.$on('unload.basket', function(event) {
            $scope.map.removeLayer(basketLayer);
            if(resultsLayer){
                $scope.map.addLayer(resultsLayer);
            }
        });

        var selectClick = new ol.interaction.Select({
            condition: ol.events.condition.click,
            toggleCondition: ol.events.condition.shiftKeyOnly,
            style: selectedStyles['Polygon']
        });
        $scope.map.addInteraction(selectClick);

        selectClick.on('select', function(evt) {
            if(evt.selected.length > 0){
                $rootScope.$broadcast('map.item.toggled', evt.selected[0].get('data'));
            }
            if(evt.deselected.length > 0){
                $rootScope.$broadcast('map.item.toggled', evt.deselected[0].get('data'));
            }
        });

        $scope.$on('results.item.selected', function(event, item, selected) {
            selectItem(item, selected);
        });

        function selectItem(item, selected){
            var style;
            if(selected){
                style = selectedStyles[item.geo.type];
            }
            else {
                style = resultStyle;
            }
            var features = resultsLayer.getSource().getFeatures();
            for(var i = 0; i < features.length; i++){
                if((item.identifier && item.identifier == features[i].get('data').identifier)){
                    features[i].setStyle(style);
                    if(selected){
                        $scope.map.getView().fit(features[i].getGeometry().getExtent(), $scope.map.getSize());
                    }
                    break;
                }
            }
        }

        $scope.$on('results.select.all', function(event, selected) {
            selectAll(selected);
        });

        function selectAll(selected){
            var features = resultsLayer.getSource().getFeatures();
            for(var i = 0; i < features.length; i++){
                if(selected){
                    features[i].setStyle(selectedStyles[features[i].getGeometry().getType()]);
                }
                else{
                    features[i].setStyle(resultStyle);
                }
            }
        }

        $scope.$on('results.invert', function(event, items) {
            selectAll(false);

            for(var i = 0; i < items.length; i++){
                selectItem(items[i], true);
            }
        });

        $scope.clearMap = function(){
            if(droppedFileLayer){
                $scope.map.removeLayer(droppedFileLayer);
            }
            $scope.drawType = SHAPE.NONE;
            $scope.map.removeInteraction(draw);
            addInteraction();
            $rootScope.$broadcast('polygon.drawn', undefined);
        };

        $scope.$on('map.resize', function(event, productUrl) {
            $scope.map.updateSize();
        });

        //WMS layer to show products on map
        var productLayers = [];
        $scope.$on('show.products', function(event, jobId, products) {
            for(var i = 0; i < productLayers.length; i++){
                $scope.map.removeLayer(productLayers[i]);
            }
            productLayers = [];
            for(var i = 0; i < products.length; i++){
                if(products[i].attributes.fname){
                    var name = products[i].attributes.fname;
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
                        projection: 'EPSG:4326'
                    });
                    var productLayer = new ol.layer.Image({
                        source: source
                    });
                    productLayers.push(productLayer);
                    $scope.map.addLayer(productLayer);
                }
            }
        });

    }]);
});