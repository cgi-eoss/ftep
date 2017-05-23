/**
 * @ngdoc function
 * @name ftepApp.controller:HelpdeskCtrl
 * @description
 * # HelpdeskCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('HelpdeskCtrl', ['ftepProperties', '$scope', '$http', 'ProductService', 'TabService', function (ftepProperties, $scope, $http, ProductService, TabService) {

        $scope.applications = ['Monteverdi', 'QGIS', 'SNAP'];
        $scope.processors = ['LandCoverS1', 'LandCoverS2', 'S1Biomass', 'VegetationIndicies', 'ForestChangeS2'];
        $scope.ftepUrl = ftepProperties.FTEP_URL;

        $scope.videos = [
            {
                url: $scope.ftepUrl + '/manual/search_create_databasket.mp4',
                description: 'Perform a Search and create/manage databasket',
                image: 'images/helpdesk/search.jpg'
            }, {
                url: $scope.ftepUrl + '/manual/run_sentinel2_ndvi.mp4',
                description: 'Run an NDVI service',
                image: 'images/helpdesk/service_NDVI.jpg'
            }, {
                url: $scope.ftepUrl + '/manual/open_ndvi_snap.mp4',
                description: 'Open a product using Sentinel2 Toolbox',
                image: 'images/helpdesk/NDVI_product_toolbox.jpg'
            }, {
                url: $scope.ftepUrl + 'manual/vegindex_snap.mp4',
                description: 'Create a VegetaionIndex and open result in Sentinel2 Toolbox',
                image: 'images/helpdesk/vegind_product_toolbox.jpg'
            }
        ];

        $scope.hideContent = true;
        var tutorials;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'tutorials':
                    tutorials = true;
                    break;
            }

            if (tutorials) {
                $scope.hideContent = false;
            }
        };

    }]);
});

