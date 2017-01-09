<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>wps-js example</title>
<link rel="shortcut icon" href="favicon.ico" />
</head>
<body>
	<div id="wrapper">
		<div>
			<h1>wps-js example</h1>
			<p>wps-js is an <a href="https://github.com/52North/wps-js/" title="wps-js on GitHub">open source client library</a> for connecting Javascript client to <a href="http://www.opengeospatial.org/standards/wps" title="WPS @ Open Geospatial Consortium">OGC Web Processing</a> services. This page demonstrates how you can use it. More documentation is available in the <a href="https://wiki.52north.org/bin/view/Geoprocessing/Wps-js" title="wps-js wiki page">52&deg;North Wiki</a> and on <a href="https://github.com/52North/wps-js/blob/master/README.md" title="wps-js readme">GitHub</a>.</p>
		</div>
		<hl>
		<div>
			<select id="wps">
				<option>Select a WPS</option>
				<option>https://192.171.139.83/wps</option>
			</select>
		</div>
		<div>
			<h3>
				Capabilities <a href="#" id="wpscaller">Show Capabilities</a>
			</h3>
			<div class="wps-container">
				<div id="capabilitiesByClick"></div>
			</div>
		</div>
		<div>
			<h3>Execute Process</h3>
			<div class="wps-container">
				<select id="processes"><option>Select a process</option></select>
				<span id="processDescriptionLink"></span>
				<div class="wps-execute-container" id="wps-execute-container"></div>
			</div>
			<p></p>
			<div class="wps-container">
			    <div id="auto-update-container"></div>
				<div id="executeProcess"></div>
			</div>
		</div>
	</div>
	<script type="text/javascript">
		var capabilities, // the capabilities, read by Format.WPSCapabilities::read
		process; // the process description from Format.WPSDescribeProcess::read

		jQuery('#wpscaller').click(function() {
			console.log("clicked");

			var sel = document.getElementById("wps");

		        console.log( sel.options[sel.selectedIndex].text );
			console.log( GET_CAPABILITIES_TYPE );

			jQuery('#capabilitiesByClick').wpsCall({
				url : sel.options[sel.selectedIndex].text,
				requestType : GET_CAPABILITIES_TYPE
			});
		});
		jQuery(document).ready(function(){

							jQuery.wpsSetup({
								proxy : {
									url : "/wps_proxy/wps_proxy?url=",
									type : "parameter"
								},
								configuration : {
									url : "https://192.171.139.83/wps"
								}
							});
		});

		// add behavior to html elements
		document.getElementById("processes").onchange = function() {
			var sel = document.getElementById("processes");
			
			describeProcess(sel.options[sel.selectedIndex].text);
		};
		document.getElementById("wps").onchange = function() {
			var sel = document.getElementById("wps");
			
			getCapabilities(sel.options[sel.selectedIndex].text);
		};
		
	</script>

	<div>
		<p class="infotext">wps-js-0.1.2-SNAPSHOT #4bc1467 as of 20151209153238, build at 20160625-1446</p>
	</div>
</body>
</html>
