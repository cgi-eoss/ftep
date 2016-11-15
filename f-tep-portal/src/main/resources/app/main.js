require.config({

    // aliases and paths of modules
    paths: {
        angular: '../bower_components/angular/angular',
        ngResource: '../bower_components/angular-resource/angular-resource',
        ngCookies: '../bower_components/angular-cookies/angular-cookies',
        ngAnimate: '../bower_components/angular-animate/angular-animate',
        ngRoute: '../bower_components/angular-route/angular-route',
        ngSanitize: '../bower_components/angular-sanitize/angular-sanitize',
        ngTouch: '../bower_components/angular-touch/angular-touch',
        ngAria: '../bower_components/angular-aria/angular-aria',
        ngMessages: '../bower_components/angular-messages/angular-messages',
        ngMaterial: '../bower_components/angular-material/angular-material',
        ngBootstrap: '../bower_components/angular-ui-bootstrap/ui-bootstrap-tpls',
        rzModule: '../bower_components/angularjs-slider/rzslider',
        ngOpenlayers: '../bower_components/angular-openlayers-directive/angular-openlayers-directive',
        dndLists: '../bower_components/angular-drag-and-drop-lists/angular-drag-and-drop-lists',
        ngPaging: '../bower_components/angular-paging/paging',
        clipboard: '../bower_components/clipboard-js/clipboard',

        hogan: 'zoo-client/lib/hogan/hogan-3.0.2',
        xml2json: 'zoo-client/lib/xml2json/xml2json.min',
        queryString: 'zoo-client/lib/query-string/query-string',
        text: 'zoo-client/lib/require-text-2.0.12',
        notify: 'zoo-client/lib/bootstrap-notify',
        wpsPayloads: 'zoo-client/lib/zoo/payloads',
        wpsPayload: 'zoo-client/lib/zoo/wps-payload',
        utils: 'zoo-client/lib/zoo/utils',
        zoo: 'zoo-client/lib/zoo/zoo',
        domReady: 'zoo-client/lib/domReady',
        hgn: 'zoo-client/lib/require-hgn-0.3.0',

        ol: '../bower_components/openlayers/ol',
        jquery: '../bower_components/jquery/jquery',
        bootstrap: '../bower_components/bootstrap/js/bootstrap',
        ftepConfig: 'scripts/ftepConfig',
        app: 'scripts/app',
    },

    // modules and their dependent modules
    shim: {
        jquery: {
            exports: ['$', 'jQuery', 'jquery'],
        },
        //// angular modules
        angular: {
            deps: ['jquery'],
            exports: 'angular',
        },
        ngResource: {
            deps: ['angular'],
            exports: 'ngResource',
        },
        ngCookies: {
            deps: ['angular'],
            exports: 'ngCookies',
        },
        ngRoute: {
            deps: ['angular'],
            exports: 'ngRoute',
        },
        ngMaterial: {
            deps: ['angular'],
            exports: 'ngMaterial',
        },
        ngTouch: {
            deps: ['angular'],
        },
        ngAnimate: {
            deps: ['angular'],
            exports: 'ngAnimate',
        },
        ngAria: {
            deps: ['angular'],
            exports: 'ngAria',
        },
        dndLists: {
            deps: ['angular'],
            exports: 'dndLists',
        },
        ngPaging: {
            deps: ['angular'],
            exports: 'ngPaging',
        },
        rzModule: {
            deps: ['angular'],
            exports: 'rzModule',
        },
        ngSanitize: {
            deps: ['angular'],
            exports: 'ngSanitize',
        },
        ngBootstrap: {
            deps: ['angular'],
            exports: 'ngBootstrap',
        },
        ngOpenlayers: {
            deps: ['angular'],
            exports: 'ngOpenlayers',
        },
        //// openlayers3
        ol: {
            exports: 'ol',
        },
        //// zoo-client
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
        zoo: {
            deps: ['queryString', 'xml2json', 'utils']
        },
        ////
        clipboard: {
            exports: 'clipboard',
        },
        //// application,
        ftepConfig: {
            exports: 'ftepConfig',
        },
        app: {
            deps: ['angular', 'ol', 'zoo', 'ftepConfig'],
        }
    }
});

require(['domReady', 'app'], function(domReady, app) {
    domReady(function() {
        app.init();
    });
});
