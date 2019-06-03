/**
 * @ngdoc function
 * @name ftepApp.controller:ServiceCtrl
 * @description
 * # ServiceCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('ServiceCtrl', ['$scope', 'ProductService', 'CommonService', '$mdDialog', function ($scope, ProductService, CommonService, $mdDialog) {

        var editmode = false;

        $scope.serviceParams = ProductService.params.developer;
        $scope.serviceOwnershipFilters = ProductService.serviceOwnershipFilters;
        $scope.serviceTypeFilters = ProductService.serviceTypeFilters;
        $scope.serviceTypes = {
            APPLICATION: { id: 0, name: 'Application', value: 'APPLICATION'},
            PROCESSOR: { id: 0, name: 'Processor', value: 'PROCESSOR'},
            BULK_PROCESSOR: { id: 0, name: 'Bulk Processor', value: 'BULK_PROCESSOR'}
        };

        ProductService.refreshServices('developer');

        $scope.toggleServiceFilter = function(){
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        /* Update Services when polling */
        $scope.$on('poll.services', function (event, data) {
            $scope.serviceParams.services = data;
        });

        $scope.$on("$destroy", function() {
            ProductService.stopPolling();
        });

        /* Paging */
        $scope.getPage = function(url){
            ProductService.getServicesPage('developer', url);
        };

        $scope.filter = function(){
            ProductService.getServicesByFilter('developer');
        };

        function discardChangesMessage(event) {
            if (editmode) {
                var answer = confirm("Are you sure you want to leave this page? Unsaved changes will be discarded.");
                if (!answer) {
                    event.preventDefault();
                    return false;
                } else {
                    editmode = false;
                    return true;
                }
            } else {
                return true;
            }
        }

        $scope.$on('$locationChangeStart', function( event ) {
            discardChangesMessage(event);
        });

        $scope.toggleEditMode = function(state) {
            editmode = state;
        };

        $scope.createService = function($event) {
            function CreateServiceController($scope, $mdDialog) {

                $scope.createService = function () {
                    ProductService.createService($scope.newItem.name, $scope.newItem.description, $scope.newItem.title).then(function (newService) {
                        ProductService.refreshServices('developer', 'Create', newService);
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }

            CreateServiceController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: CreateServiceController,
                templateUrl: 'views/developer/templates/createservice.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        $scope.selectService = function(service, event) {
            if(discardChangesMessage(event)) {
                ProductService.getService(service).then(function(detailedService) {
                    $scope.serviceParams.displayRight = true;
                    $scope.serviceParams.selectedService = detailedService;
                    $scope.isWorkspaceLoading = false;
                    ProductService.refreshSelectedService('developer');
                    $scope.serviceParams.activeArea = $scope.serviceParams.constants.tabs.files;
                });
            }
        };

        $scope.getServiceValidationMessage = function(formInvalid, templateRequiresValidation) {
            if(formInvalid) {
                return 'Invalid form value';
            } else if (templateRequiresValidation) {
                return 'Please validate the \'Simple Input Definitions\' template and ensure it is valid';
            } else {
                return 'Save Service';
            }
        };

        $scope.saveService = function(){
            ProductService.saveService($scope.serviceParams.selectedService).then(function(service){
                editmode = false;
                // Clear selected services in other pages to avoid caching conflicts
                for (var page in ProductService.params) {
                    if (ProductService.params[page] && page !== 'developer') {
                        ProductService.params[page].selectedService = undefined;
                    }
                }
                ProductService.refreshServices('developer');
                (function(ev) {
                    $mdDialog.show(
                        $mdDialog.alert()
                            .clickOutsideToClose(true)
                            .title('Service Saved')
                            .textContent($scope.serviceParams.selectedService.name + ' service has been saved successfully!')
                            .ariaLabel('Service Success Dialog')
                            .ok('OK')
                            .targetEvent(ev)
                    );
                })();
            });
        };

        $scope.removeService = function(event, service){
            CommonService.confirm(event, 'Are you sure you want to delete this service: "' + service.name + '"?').then(function (confirmed){
                if(confirmed === false){
                    return;
                }
                ProductService.removeService(service).then(function(){
                    ProductService.refreshServices('developer', 'Remove', service);
                });
            });
        };

    }]);

});
