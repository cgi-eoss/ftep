/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityManageJobCtrl
 * @description
 * # CommunityManageJobCtrl
 * Controller of the ftepApp
 */

'use strict';

define(['../../../ftepmodules', 'ol', 'clipboard'], function (ftepmodules, ol, clipboard) {

    ftepmodules.controller('CommunityManageSystematicProcessingCtrl', ['CommunityService', 'SystematicService', 'MapService', 'CommonService', 'MessageService', '$scope', '$location', function (CommunityService, SystematicService, MapService, CommonService, MessageService, $scope, $location) {

        /* Get stored Jobs details */
        $scope.systematicParams = SystematicService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "Systematic processing";

        /* Filters */
        $scope.toggleSharingFilters = function () {
            $scope.systematicParams.sharedGroupsDisplayFilters = !$scope.systematicParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.systematicParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshSystematicProcessing = function() {
            SystematicService.refreshSelectedSystematicProcessing('community');
        };

        // Not implemented
        /*$scope.showAreaOnMap = function(aoi) {
            try {
                var polygon = new ol.format.WKT().readGeometry(aoi[0]);
                AoiService.setSearchAoi({
                    geometry: JSON.parse(new ol.format.GeoJSON().writeGeometry(polygon))
                });

                polygon.transform('EPSG:4326', 'EPSG:3857');
                var extent = polygon.getExtent();
                if (extent) {
                    MapService.fitExtent(extent);
                    $location.path('/explorer');
                }
            } catch(e) {
                MessageService.addError('Unable to parse aoi', error);
            }
        }*/

        $scope.copyToClipboard = function(value) {
            clipboard.copy(value);
        }


    }]);
});
