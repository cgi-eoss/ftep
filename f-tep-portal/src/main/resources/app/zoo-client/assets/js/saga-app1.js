/**
* Author: GÃ©rald Fenoy
*
* Copyright (c) 2015 GeoLabs SARL
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*
* This work was supported by a grant from the European Union's 7th Framework Programme (2007-2013)
* provided for the project PublicaMundi (GA no. 609608).
*/

require(['bootstrap', 'notify']);

define([
    'module', 'jquery', 'zoo', 'xml2json','ol', 'hgn!tpl/describe_process_form1'
], function(module, $, Zoo, X2JS,ol,tpl_describeProcess) {
    
 /*   
    var zoo1 = new Zoo({
        url: "/wps",
        delay: module.config().delay,
    });

    zoo1.getCapabilities({
        type: 'POST',
        success: function(data){
            console.log(data["Capabilities"]["ProcessOfferings"]["Process"]);
        }
    });
*/



    var zoo = new Zoo({
        url: module.config().url,
        delay: module.config().delay,
    });
    
    var currentLID=0;
    var mymodal = $('#myModal');
    var mynotify = $('.top-right');
    var points   = "sf:bugsites";
    var lines    = "";
    var polygons = "sf:restricted";
    var raster = "sf:sfdem";
    var mapUrl = "http://zoo-project.org:8080/geoserver/wms";
    var mapWFSUrl = "http://zoo-project.org:8080/geoserver/wfs";
    var mapWCSUrl = "http://zoo-project.org:8080/geoserver/wcs";
    var map;
    var styles=[];
    var basicLayers=[];
    

    function notify(text, type) {
        mynotify.notify({
            message: { text: text },
            type: type,
        }).show();
    }
    
    var initialize = function() {
    $(function(){
/*
        map = new ol.Map({
        layers: new ol.Collection(),
        target: 'map',
        view: new ol.View({
//        center: [-11557933,5533207],
//         zoom:12 
        })
    });
*/
    var layers = [
  new ol.layer.Tile({
    source: new ol.source.TileWMS({
      // url: 'http://demo.boundlessgeo.com/geoserver/wms',
      url: '/wps-gui-proxy/geoserver/wms',
      params: {
        'LAYERS': 'ne:NE1_HR_LC_SR_W_DR'
      }
    })
  })
];

    var source = new ol.source.Vector();

var vector = new ol.layer.Vector({
  source: source,
  style: new ol.style.Style({
    fill: new ol.style.Fill({
      color: 'rgba(255, 255, 255, 0.2)'
    }),
    stroke: new ol.style.Stroke({
      color: '#ffcc33',
      width: 2
    }),
    image: new ol.style.Circle({
      radius: 7,
      fill: new ol.style.Fill({
        color: '#ffcc33'
      })
    })
  })
});




      layers = [ new ol.layer.Tile({ source: new ol.source.OSM() }) , vector ]  ;

      var map = new ol.Map({
        controls: ol.control.defaults().extend([
          new ol.control.ScaleLine({
            units: 'degrees'
          })
        ]),
        layers: layers,
        target: 'map',
        controls: ol.control.defaults({
            attributionOptions: ({
                collapsible: false
            })
        }),
        view: new ol.View({
          center: [0, 0],
          zoom: 2
        })
      });

    var style1 = new ol.style.Style({
            image: new ol.style.Circle({
        radius: 5,
        fill: new ol.style.Fill({
            color: 'rgba(254,252,234,1)'
        }),
        stroke: new ol.style.Stroke({
            color: 'rgba(102,62,5,1)'
        })
        })
    });

    layerLS=new ol.layer.Tile({
        opacity: 0.9,
        source: new ol.source.OSM()
    });




     map.addLayer(layerLS);
     basicLayers.push(layerLS); 






     $(window).on("resize", applyMargins);


     applyMargins();




         $("#wpscaller").click( function() {
            console.log("click");
             var params={
                 type: 'POST',
                 success: function(data){
                     console.log("XXX");
                     console.log( data );
                     


                     for(var i in data.Capabilities.ProcessOfferings.Process){
                         console.log(data.Capabilities.ProcessOfferings.Process[i])
                         console.log(data.Capabilities.ProcessOfferings.Process[i].Identifier.__text)
                         /*
          <ul class="list-group">
          <li class="list-group-item layerItem"><input name="name" type="checkbox" checked="checked"  disabled="disabled" /> Elevation</li>
          */
                         $("#wps-list").append(
                             $("<li>").attr("class","list-group-item layerItem").append(
                                 $("<a>").attr('href',"").append(
                                     $("<span>").append(
                                         data.Capabilities.ProcessOfferings.Process[i].Identifier.__text
                                     )
                                 ).click( function(event){ 
                                     event.preventDefault();
                                     console.log(event.target.childNodes.item(0) );
                                     getDescription(event.target.childNodes.item(0).data );
                                 })
                             )
                         );
                     }
                 }
             };
             zoo.getCapabilities( params );
             // $("input[id$='WKT']")
/*
     * myZooObject.getCapabilities({
     *      *     type: 'POST',
     *           *     success: function(data){
     *                *         console.log(data["Capabilities"]["ProcessOfferings"]["Process"]);
     *                     *     }
     *                          * });
     *
 */


         });
	 $("#serviceIdentifier").change(function(){
	     getDescription($(this).val());
	 });
	 getDescription($("#serviceIdentifier").val());
    });
    }
function applyMargins() {
        var leftToggler = $(".mini-submenu-left");
        if (leftToggler.is(":visible")) {
          $("#map .ol-zoom")
            .css("margin-left", 0)
            .removeClass("zoom-top-opened-sidebar")
            .addClass("zoom-top-collapsed");
        } else {
          $("#map .ol-zoom")
            .css("margin-left", $(".sidebar-left").width())
            .removeClass("zoom-top-opened-sidebar")
            .removeClass("zoom-top-collapsed");
        }
      }


    function getDescription(identifier){
	$("#layers,#layers_display").find(".panel-body").css({"height":($(window).height()/3)+"px"});
	$("#layers,#layers_display").find(".panel-body").css({"max-height":($(window).height()/3)+"px"});
	zoo.describeProcess({
	    identifier: identifier,
	    type: "POST",
	    success: function(data) {
		data["Identifier1"]=data.ProcessDescriptions.ProcessDescription.Identifier.__text.replace(/\./g,"__");
		data.ProcessDescriptions.ProcessDescription.Identifier1=data.ProcessDescriptions.ProcessDescription.Identifier.__text.replace(/\./g,"__");
		for(var i in data.ProcessDescriptions.ProcessDescription.DataInputs.Input){
		    if(data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i]._minOccurs=="0")
			data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i].optional=true;
		    else
			data.ProcessDescriptions.ProcessDescription.DataInputs.Input[i].optional=false;
		}
		var details =  tpl_describeProcess(data);
		$(".main-row").find(".panel-body").first().html(details);
		$("#layers").find('.panel-body').first().find("input[type=hidden]").each(function(){
			console.log($(this).prev().text());
		    if($(this).val()==""){
			if($(this).prev().text()=="Points"){
			    $(this).val(points);
			}
			else{
			    if($(this).prev().text()=="Elevation" || $(this).prev().text()=="Grid")
			        $(this).val(raster);
			    else
			        $(this).val(polygons);
			}
			    
		    }
		});
		$("#layers").find('.panel-body').first().find("p").first().each(function(){
		    var dup=$(this).text();
		    console.log(dup);
		    $(this).html(dup);
		});
		$("#btn-wps-execute").click(function(){
		    launchProcessing(identifier);
		});
	    },
	    error: function(data) {
		notify('DescribeProcess failed', 'danger');
	    }
	});
    }

    var filename="http://geolabs.fr/dl/Landsat8Extract1.tif";
    function launchProcessing(aProcess) {
	notify('Running '+aProcess+' service','info');
	var iparams=[];
	$("#layers").find(".pm-popup").find("input[type=text],select").each(function(){
	    var lname=$(this).attr("id").replace(/wps_i_/g,"");
	    console.log(lname);
	    if($(this).is(":visible") && lname!=$(this).attr("id"))
		iparams.push({
		    identifier: lname,
		    value: $(this).val(),
		    dataType: "string"
		});
	});
	$("#layers").find(".pm-popup").find("input[type=hidden]").each(function(){
	    var lname=$(this).attr("id").replace(/wps_i_/g,"");
	    console.log(lname);
	    if($(this).parent().is(":visible") && lname!=$(this).attr("id")){
	    	if($(this).val()==raster)
		iparams.push({
		    identifier: lname,
		    href: mapWCSUrl+"?service=WCS&version=2.0.0&request=GetCoverage&CoverageId="+$(this).val(),
		    mimeType: "image/tiff"
		});
		else
		iparams.push({
		    identifier: lname,
		    href: mapWFSUrl+"?service=WFS&version=1.0.0&request=GetFeature&srsName=EPSG:4326&typename="+$(this).val(),
		    mimeType: "text/xml"
		});
            }
	});
	var oparams=[];
	$("#layers").find(".pm-popup").find("select").each(function(){
	    var lname=$(this).attr("id").replace(/format_wps_o_/g,"");
	    console.log(lname);
	    if($(this).is(":visible") && lname!=$(this).attr("id"))
		oparams.push({
		    identifier: lname,
		    mimeType: $(this).val(),
		    asReference: "true"
		});
	});
	console.log(iparams);
	console.log(oparams);
	var progress=$("#progress-process");
	zoo.execute({
	    identifier: aProcess,
	    dataInputs: iparams,
	    dataOutputs: oparams,
	    type: 'POST',
            storeExecuteResponse: true,
            status: true,
            success: function(data, launched) {
		console.log("**** SUCCESS ****");
		console.log(launched);
		notify("Execute asynchrone launched: "+launched.sid, 'info');

		// Polling status
		zoo.watch(launched.sid, {
                    onPercentCompleted: function(data) {
			console.log("**** PercentCompleted ****");
			console.log(data);
			
			progress.css('width', (data.percentCompleted)+'%');
			progress.text(data.text+' : '+(data.percentCompleted)+'%');
			progress.attr("aria-valuenow",data.percentCompleted);
			$("#infoMessage").html(data.text+' : '+(data.percentCompleted)+'%');
                    },
                    onProcessSucceeded: function(data) {
			console.log("**** ProcessSucceeded ****");
			//console.log(data);
			
			progress.css('width', (100)+'%');
			progress.text(data.text+' : '+(100)+'%');
			progress.removeClass("progress-bar-info").addClass("progress-bar-success");
			progress.attr("aria-valuenow",100);
			$("#infoMessage").html("");

			notify(aProcess+' service run successfully','success');
			console.log(data);//.ExecuteResponse.ProcessOutputs.Output.Reference._href);
			var ldata=data.result.ExecuteResponse.ProcessOutputs.Output;
			var cnt=0;
			if(!$.isArray(ldata))
			    ldata=[data.result.ExecuteResponse.ProcessOutputs.Output];
			for(var a=0;a<ldata.length;a++){
			    console.log(ldata[a]);
			    var lmapUrl=ldata[a].Reference._href.split('&')[0];
			    console.log(lmapUrl);
			    var content=$("#addLayer_template")[0].innerHTML.replace(/lname/g,ldata[a].Identifier.toString());
			    if(lmapUrl.replace(/json/g,"")!=lmapUrl){

				var sourceVector;

				loadFeatures = function(response) {
				    formatWFS = new ol.format.GeoJSON(),
				    sourceVector.addFeatures(formatWFS.readFeatures(response,{
					dataProjection: ol.proj.get('EPSG:4326'),
					featureProjection: ol.proj.get('EPSG:3857')
				    }));
				};

				sourceVector = new ol.source.Vector({
				});

				if(styles[cnt]!=null)
				    layerVector = new ol.layer.Vector({
					visible: true,
					style: style,
					source: sourceVector
				    });
				else
				    layerVector = new ol.layer.Vector({
					visible: true,
					source: sourceVector
				    });
			
				cnt++;
				console.log(this.localID);
				$.ajax(lmapUrl,{
				    type: 'GET',
				    data: null,
				}).done(loadFeatures);

				map.addLayer(layerVector);
		    
				content=content.replace(/ldata/g,"#");
				content=content.replace(/lcheck/g,'<input id="layerd'+currentLID+'" name="name" type="checkbox" checked="checked" />');
				currentLID+=1;
			    }
			    else if(lmapUrl.replace(/map/g,"")!=lmapUrl){


				layer=new ol.layer.Tile({
	         		    opacity: 0.85,
				    visible: true,
				    source: new ol.source.TileWMS({
					url: lmapUrl,
					params: {'LAYERS': ldata[a].Identifier.toString(), 'TILED': true},
					serverType: 'mapserver'
				    })
				});
				map.addLayer(layer);

				content=content.replace(/ldata/g,"#");
				content=content.replace(/lcheck/g,'<input id="layerd'+currentLID+'" name="name" type="checkbox" checked="checked" />');
				currentLID+=1;
			    }else{
				content=content.replace(/ldata/g,ldata[a].Reference._href);
				content=content.replace(/lcheck/g,'<input name="name" type="checkbox" checked="checked" disabled="disabled" />');
			    }
			    console.log(content);
			    $("#layers_display").find(".list-group").first().append(content);
			    $("#layers_display").find(".layerItem").find("input[type=checkbox]").each(function(){
				$(this).off("change");
				$(this).on("change",function(){
				    var lid=parseInt($(this).attr("id").replace(/layerd/g,""));
				    map.getLayers().item(basicLayers.length+lid).setVisible($(this).is(":checked"));
				});
			    });
			    $("#layers_display").find(".removeLayer").each(function(){
				$(this).off("click");
				$(this).on("click",function(){
				    if(!$(this).prev().prev().is(":disabled")){
					var lid=parseInt($(this).prev().prev().attr("id").replace(/layerd/g,""));
					map.getLayers().item(basicLayers.length+lid).setVisible(false);
				    }
				    $(this).parent().remove();
				});
			    });
			}
                    },
                    onError: function(data) {
			console.log("**** onError ****");
			console.log(data);
			notify("Execute asynchrone failed", 'danger');
                    },
		});

            },
            error: function(data) {
		        notify('Execute failed:' +data.ExceptionReport.Exception.ExceptionText, 'danger');
            }
        });
    }




      function isConstrained() {
        return $(".sidebar").width() == $(window).width();
      }



    // Return public methods
    return {
        initialize: initialize,
        getDescription: getDescription,
	cgalProcessing: launchProcessing
    };


});

