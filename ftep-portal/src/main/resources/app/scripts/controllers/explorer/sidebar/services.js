define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('ServicesCtrl', ['$scope', '$rootScope', 'ProductService', '$sce',
                                 function ($scope, $rootScope, ProductService, $sce) {


            // Set initial selected missions to none
             $scope.serviceTypes = [
                 {
                    type: "processor",
                    label: "Processors",
                    value: true
                }, {
                    type: 'application',
                    label: "GUI Applications",
                    value: true
                }
            ];

            var temp = JSON.parse(JSON.stringify($scope.serviceTypes));

            $scope.selected = temp;

            // Toggle checkbox when selected
            $scope.toggle = function (item, list) {
                var idx = list.indexOf(item);
                if (idx > -1) {
                    list.splice(idx, 1);
                } else {
                    list.push(item);
                }
            };

            // Sets the check attribute
            $scope.exists = function (item, list) {
                return list.indexOf(item) > -1;
            };

            $scope.services = [];
            ProductService.getServices().then(function (data) {
                $scope.services = data;
            });


            $scope.filteredServiceTypes = [];
            $scope.filterServices = function () {
                //TODO another query?
                $scope.filteredServiceTypes = [];
                for (var i = 0; i < $scope.serviceTypes.length; i++) {
                    if ($scope.serviceTypes[i].value === true) {
                        $scope.filteredServiceTypes.push($scope.serviceTypes[i].type);
                    }
                }
            };
            $scope.filterServices();

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
                if (desc && desc.length > 41) {
                    return desc.substring(0, 41).concat('..');
                }
                return desc;
            };

            var popover = {};

            $scope.getServicePopover = function (service) {

                var html = '<p class="raiting">' + $scope.getRating(service.attributes.rating) + '</p>' +
                    '<div class="metadata"><div class="row">' +
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
                        stars = stars + '<img src="images/star_filled.png" class="raiting-star"/>';
                    } else {
                        stars = stars + '<img src="images/star_lined.png" class="raiting-star"/>';
                    }
                }
                return stars;
            };

    }]);
});
