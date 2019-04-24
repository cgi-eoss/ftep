/**
 * @ngdoc function
 * @name ftepApp.controller:DeveloperCtrl
 * @description
 * # DeveloperCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('DeveloperCtrl', ['$scope', 'TabService', 'MessageService', 'ProductService', '$rootScope', function ($scope, TabService, MessageService, ProductService, $rootScope) {

        $scope.serviceParams = ProductService.params.developer;
        $scope.developerSideNavs = TabService.getDeveloperSideNavs();
        $scope.navInfo = TabService.navInfo.developer;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.toggleBottomView = function(){
            $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
        };

        function showSidebarArea() {
            $scope.navInfo.sideViewVisible = true;
        }

        $scope.hideSidebarArea = function () {
            $scope.navInfo.activeSideNav = undefined;
            $scope.navInfo.sideViewVisible = false;
        };

        $scope.toggleSidebar = function (tab) {
            if($scope.navInfo.activeSideNav === tab) {
                $scope.hideSidebarArea();
            } else {
                $scope.navInfo.activeSideNav = tab;
                showSidebarArea();
            }
        };

        $scope.hideContent = true;
        var navbar, sidenav, services;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'services':
                    services = true;
                    break;
            }

            if (navbar && sidenav && services) {
                $scope.hideContent = false;
            }
        };

        // Adds the 'Simple Input Definitions' tab to the service, enabling Easy Mode
        $scope.addEasyMode = function() {
            $scope.serviceParams.activeArea = $scope.serviceParams.constants.tabs.easyMode;
            $scope.serviceParams.selectedService.easyModeServiceDescriptor.id = $scope.serviceParams.selectedService.name;

            // Build template from serviceDescriptor inputs
            var template = { inputs: {}, searchParameters: [] };
            for (var input in $scope.serviceParams.selectedService.serviceDescriptor.dataInputs) {
                template.inputs[$scope.serviceParams.selectedService.serviceDescriptor.dataInputs[input].id] = ['"{{inputs.' + $scope.serviceParams.selectedService.serviceDescriptor.dataInputs[input].id + '.[0]}}"'];
            }

            // Set template and inputs for the easy mode
            $scope.serviceParams.selectedService.easyModeParameterTemplate = JSON.stringify(template,null,2).replace(/\\"/g,'');
            $scope.serviceParams.selectedService.easyModeServiceDescriptor.dataInputs = $scope.serviceParams.selectedService.serviceDescriptor.dataInputs;

            // Broadcast template update message to trigger codemirror to refresh
            $rootScope.$broadcast('developer.definitions.editor.update', template);
        };

    }]);

});
