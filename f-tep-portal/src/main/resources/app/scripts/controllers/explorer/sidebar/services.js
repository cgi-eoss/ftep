define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('ServicesCtrl', ['$scope', '$rootScope', '$sce', 'ProductService', 'TabService',
                                 function ($scope, $rootScope, $sce, ProductService, TabService) {

            $scope.serviceParams = ProductService.params.explorer;

            // Service types which services are grouped by
             $scope.serviceTypes = [
                 {
                    type: "PROCESSOR",
                    label: "Processors"
                }, {
                    type: 'APPLICATION',
                    label: "GUI Applications"
                }
            ];

            ProductService.refreshServices('explorer');

            $scope.serviceSearch = {
                searchText: $scope.serviceParams.searchText
            };

            $scope.serviceQuickSearch = function (item) {
                if (item.name.toLowerCase().indexOf($scope.serviceSearch.searchText.toLowerCase()) > -1||
                    (item.description && item.description.toLowerCase().indexOf($scope.serviceSearch.searchText.toLowerCase()) > -1)) {
                    return true;
                }
                return false;
            };

            $scope.selectService = function (service) {
                $rootScope.$broadcast('update.selectedService', service);
                TabService.navInfo.activeSideNav = TabService.getExplorerSideNavs().WORKSPACE;
            };

            $scope.getShortDesc = function (desc) {
                if (desc && desc.length > 60) {
                    return desc.substring(0, 60).concat('..');
                }
                return desc;
            };

            var popover = {};

            $scope.getServicePopover = function (service) {

                var html =
                    '<div class="metadata">' +
                        '<div class="row">' +
                            '<div class="col-sm-4">Name:</div>' +
                            '<div class="col-sm-8">' + service.name + '</div>' +
                        '</div>' +
                        '<div class="row">' +
                            '<div class="col-sm-4">Description:</div>' +
                            '<div class="col-sm-8">' + (service.description ? service.description : '' ) + '</div>' +
                        '</div>' +
                        '<div class="row">' +
                            '<div class="col-sm-4">Type:</div>' +
                            '<div class="col-sm-8">' + service.type + '</div>' +
                        '</div>' +
                        '<div class="row">' +
                            '<div class="col-sm-4">Licence:</div>' +
                            '<div class="col-sm-8">' + service.licence + '</div>' +
                        '</div>' +
                    '</div>';
                return popover[html] || (popover[html] = $sce.trustAsHtml(html));
            };

    }]);
});
