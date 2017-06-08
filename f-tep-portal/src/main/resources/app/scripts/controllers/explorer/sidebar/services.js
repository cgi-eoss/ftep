/**
 * @ngdoc function
 * @name ftepApp.controller:ServicesCtrl
 * @description
 * # ServicesCtrl
 * Controller of the ftepApp
 */
'use strict';
define(['../../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ServicesCtrl', ['$scope', '$rootScope', '$sce', 'ProductService', 'TabService', function ($scope, $rootScope, $sce, ProductService, TabService) {

            $scope.serviceParams = ProductService.params.explorer;
            $scope.serviceOwnershipFilters = ProductService.serviceOwnershipFilters;
            $scope.serviceTypeFilters = ProductService.serviceTypeFilters;

            ProductService.refreshServices('explorer');

            $scope.getPage = function(url){
                ProductService.getServicesPage('explorer', url);
            };

            $scope.filter = function(){
                ProductService.getServicesByFilter('explorer');
            };

            /* Update Services when polling */
            $scope.$on('poll.services', function (event, data) {
                $scope.serviceParams.services = data;
            });

            $scope.$on("$destroy", function() {
                ProductService.stopPolling();
            });

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
