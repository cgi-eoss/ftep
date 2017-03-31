'use strict';

require.config({

  // JS module paths
  paths: {
    // Angular + extensions
    angular: 'vendor/angular/angular',
    ngAnimate: 'vendor/angular-animate/angular-animate',
    ngAria: 'vendor/angular-aria/angular-aria',
    ngCookies: 'vendor/angular-cookies/angular-cookies',
    dndLists: 'vendor/angular-drag-and-drop-lists/angular-drag-and-drop-lists',
    ngMaterial: 'vendor/angular-material/angular-material',
    ngMessages: 'vendor/angular-messages/angular-messages',
    angularMoment: 'vendor/angular-moment/angular-moment',
    ngOpenlayers: 'vendor/angular-openlayers-directive/angular-openlayers-directive',
    ngPaging: 'vendor/angular-paging/paging',
    ngResource: 'vendor/angular-resource/angular-resource',
    ngRoute: 'vendor/angular-route/angular-route',
    ngSanitize: 'vendor/angular-sanitize/angular-sanitize',
    ngScrollbar: 'vendor/ng-scrollbar/ng-scrollbar',
    ngTouch: 'vendor/angular-touch/angular-touch',
    ngBootstrap: 'vendor/angular-ui-bootstrap/ui-bootstrap-tpls',
    rzModule: 'vendor/angularjs-slider/rzslider',

    // Other vendor modules
    bootstrap: 'vendor/bootstrap/js/bootstrap',
    clipboard: 'vendor/clipboard-js/clipboard',
    domReady: 'vendor/domready/domReady',
    jquery: 'vendor/jquery/jquery',
    moment: 'vendor/moment/min/moment-with-locales',
    ol: 'vendor/openlayers/ol',
    requireLib: 'vendor/requirejs/require',
    traversonAngular: 'vendor/traverson-angular/browser/dist/traverson-angular',
    traversonHal: 'vendor/traverson-hal/browser/dist/traverson-hal',
    ngFileUpload: 'vendor/ng-file-upload/ng-file-upload',

    // ZOO-Client and dependencies
    hogan: 'zoo-client/lib/hogan/hogan-3.0.2',
    xml2json: 'zoo-client/lib/xml2json/xml2json.min',
    queryString: 'zoo-client/lib/query-string/query-string',
    text: 'zoo-client/lib/require-text-2.0.12',
    notify: 'zoo-client/lib/bootstrap-notify',
    wpsPayloads: 'zoo-client/lib/zoo/payloads',
    wpsPayload: 'zoo-client/lib/zoo/wps-payload',
    utils: 'zoo-client/lib/zoo/utils',
    zoo: 'zoo-client/lib/zoo/zoo',
    hgn: 'zoo-client/lib/require-hgn-0.3.0',

    // F-TEP modules
    ftepConfig: 'ftepConfig',
    moduleloader: 'moduleloader',
    app: 'app'
  },

  // modules and their dependent modules
  shim: {
    jquery: {
      exports: ['$', 'jQuery', 'jquery']
    },
    angular: {
      deps: ['jquery'],
      exports: 'angular'
    },
    ngResource: {
      deps: ['angular'],
      exports: 'ngResource'
    },
    ngCookies: {
      deps: ['angular'],
      exports: 'ngCookies'
    },
    ngRoute: {
      deps: ['angular'],
      exports: 'ngRoute'
    },
    ngMaterial: {
      deps: ['angular'],
      exports: 'ngMaterial'
    },
    ngTouch: {
      deps: ['angular']
    },
    ngAnimate: {
      deps: ['angular'],
      exports: 'ngAnimate'
    },
    ngMessages: {
      deps: ['angular'],
      exports: 'ngMessages'
    },
    ngAria: {
      deps: ['angular'],
      exports: 'ngAria'
    },
    dndLists: {
      deps: ['angular'],
      exports: 'dndLists'
    },
    ngPaging: {
      deps: ['angular'],
      exports: 'ngPaging'
    },
    rzModule: {
      deps: ['angular'],
      exports: 'rzModule'
    },
    ngSanitize: {
      deps: ['angular'],
      exports: 'ngSanitize'
    },
    ngBootstrap: {
      deps: ['angular'],
      exports: 'ngBootstrap'
    },
    ngOpenlayers: {
      deps: ['angular'],
      exports: 'ngOpenlayers'
    },
    ol: {
      exports: 'ol'
    },
    bootstrap: {
      deps: ['jquery']
    },
    notify: {
      deps: ['jquery']
    },
    wpsPayloads: {
      deps: ['hogan']
    },
    wpsPayload: {
      deps: ['wpsPayloads'],
      exports: 'wpsPayload'
    },
    hogan: {
      exports: 'Hogan'
    },
    xml2json: {
      exports: 'X2JS'
    },
    queryString: {
      exports: 'queryString'
    },
    zoo: {
      deps: ['queryString', 'xml2json', 'utils']
    },
    clipboard: {
      exports: 'clipboard'
    },
    moment: {
      exports: 'moment'
    },
    angularMoment: {
      deps: ['angular', 'moment'],
      exports: 'angularMoment'
    },
    ngScrollbar: {
      deps: ['angular'],
      exports: 'ngScrollbar'
    },
    traversonAngular: {
      deps: ['angular'],
      exports: ['traversonAngular', 'traverson']
    },
    traversonHal: {
       deps: ['traversonAngular'],
       exports: ['traversonHal']
    },
    ngFileUpload: {
        deps: ['angular'],
        exports: 'ngFileUpload'
    },
    ftepConfig: {
      exports: 'ftepConfig'
    }
  },
  config: {
    moment: {
      noGlobal: true
    }
  }
});
