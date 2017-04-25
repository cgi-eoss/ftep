/**
 * @ngdoc function
 * @name ftepApp.controller:HelpdeskCtrl
 * @description
 * # HelpdeskCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('HelpdeskCtrl', ['$scope', '$http', 'ProductService', 'TabService', function ($scope, $http, ProductService, TabService) {

        $scope.applications = ['MonteVerdiAppV2', 'QGIS', 'Sentinel2ToolboxV2'];
        $scope.processors = ['LandCoverS1', 'LandCoverS2', 'S1Biomass', 'VegetationIndicies'];

        $scope.videos = [
                 {
                     url: 'https://forestry-tep.eo.esa.int/manual/search_create_databasket.mp4',
                     description: 'Perform a Search and create/manage databasket',
                     image: 'images/helpdesk/search.jpg'
                 },
                 {
                     url: 'https://forestry-tep.eo.esa.int/manual/run_sentinel2_ndvi.mp4',
                     description: 'Run an NDVI service',
                     image: 'images/helpdesk/service_NDVI.jpg'
                 },
                 {
                     url: 'https://forestry-tep.eo.esa.int/manual/open_ndvi_snap.mp4',
                     description: 'Open a product using Sentinel2 Toolbox',
                     image: 'images/helpdesk/NDVI_product_toolbox.jpg'
                 },
                 {
                     url: 'https://forestry-tep.eo.esa.int/manual/vegindex_snap.mp4',
                     description: 'Create a VegetaionIndex and open result in Sentinel2 Toolbox',
                     image: 'images/helpdesk/vegind_product_toolbox.jpg'
                 }
        ];

    }]);
});

