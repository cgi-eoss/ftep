define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('ServicesCtrl', ['$scope', '$rootScope', 'ProductService', '$sce',
                                 function ($scope, $rootScope, ProductService, $sce) {


            // Service types which services are grouped by
             $scope.serviceTypes = [
                 {
                    type: "processor",
                    label: "Processors"
                }, {
                    type: 'application',
                    label: "GUI Applications"
                }
            ];

            $scope.services = [];
            ProductService.getServices().then(function (data) {
                $scope.services = data;
            });

            $scope.serviceSearch = {
                searchText: ''
            };

            $scope.serviceQuickSearch = function (item) {
                if (item.attributes.name.toLowerCase().indexOf($scope.serviceSearch.searchText.toLowerCase()) > -1 || item.attributes.description.toLowerCase().indexOf($scope.serviceSearch.searchText.toLowerCase()) > -1) {
                    return true;
                }
                return false;
            };

            $scope.selectService = function (service) {
                $rootScope.$broadcast('update.selectedService', service);
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
                            '<div class="col-sm-4">Rating:</div>' +
                            '<div class="col-sm-8">' + $scope.getRating(service.attributes.rating) + '</div>' +
                        '</div>' +
                        '<div class="row">' +
                            '<div class="col-sm-4">Name:</div>' +
                            '<div class="col-sm-8">' + service.attributes.name + '</div>' +
                        '</div>' +
                        '<div class="row">' +
                            '<div class="col-sm-4">Description:</div>' +
                            '<div class="col-sm-8">' + service.attributes.description + '</div>' +
                        '</div>' +
                        '<div class="row">' +
                            '<div class="col-sm-4">Type:</div>' +
                            '<div class="col-sm-8">' + service.attributes.kind + '</div>' +
                        '</div>' +
                        '<div class="row">' +
                            '<div class="col-sm-4">License:</div>' +
                            '<div class="col-sm-8">' + service.attributes.license + '</div>' +
                        '</div>' +
                    '</div>';
                return popover[html] || (popover[html] = $sce.trustAsHtml(html));
            };

            $scope.getRating = function (rating) {
                var stars = '';
                for (var i = 0; i < 5; i++) {
                    if (rating > i) {
                        stars = stars + '<i class="material-icons star-full">star</i>';
                    } else {
                        stars = stars + '<i class="material-icons star-empty">star_border</i>';
                    }
                }
                return stars;
            };

    }]);
});
