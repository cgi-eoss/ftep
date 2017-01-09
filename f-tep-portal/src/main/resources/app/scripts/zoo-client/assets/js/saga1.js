/**
* Author : Gérald Fenoy
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
'use strict';

requirejs.config({
    baseUrl: 'assets',
    paths: {
        text: 'js/lib/require-text-2.0.12',
        hgn: 'js/lib/require-hgn-0.3.0',

	ol: 'js/lib/openlayers/ol',

        jquery: 'js/lib/jquery/jquery-2.1.1.min',
        bootstrap: 'js/lib/bootstrap-3.1.1-dist/js/bootstrap.min',
        notify: 'js/lib/bootstrap-notify',
        treeview: 'js/lib/treeview',
	contextmenu: 'js/lib/bootstrap-contextmenu',
	slider: 'js/lib/bootstrap-slider',

        hogan: 'js/lib/hogan/hogan-3.0.2',
        xml2json: 'js/lib/xml2json/xml2json.min',
        queryString: 'js/lib/query-string/query-string',
        wpsPayloads: 'js/lib/zoo/payloads',
        wpsPayload: 'js/lib/zoo/wps-payload',
        utils: 'js/lib/zoo/utils',
        zoo: 'js/lib/zoo/zoo',
        
        domReady: 'js/lib/domReady',
        app: 'js/saga-app1',
            
    },
    shim: {
        bootstrap: {
            deps: ['jquery'],
        },
        notify: {
            deps: ['jquery'],
        },
        wpsPayloads: {
	        deps: ['hogan'],
	    },
        wpsPayload: {
	    deps: ['wpsPayloads'],
            exports: 'wpsPayload',
        },
        hogan: {
            exports: 'Hogan',
        },
        xml2json: {
          exports: "X2JS",
        },
        queryString: {
            exports: 'queryString',
        },
        ol: {
            exports: 'ol',
        },
	app: {
	    deps: ['ol']
	}
    },
    
});

requirejs.config({ 
    config: {
        app: {
            //url: 'http://zoo-project.org/cgi-bin/zoo_loader1.cgi',
            url: '/wps',
            delay: 3000,
        }
    } 
});

require(['domReady', 'app'], function(domReady, app) {

    domReady(function() {
        app.initialize();
    });
    window.cgalProcessing=app.cgalProcessing;
    window.getDescription=app.getDescription;
    window.app=app;
});
