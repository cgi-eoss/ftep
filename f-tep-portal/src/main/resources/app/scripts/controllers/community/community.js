/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityCtrl
 * @description
 * # CommunityCtrl
 * Controller of the ftepApp
 */
'use strict';

define(['../../ftepmodules'], function (ftepmodules) {

    ftepmodules.controller('CommunityCtrl', ['$scope', 'CommunityService', 'GroupService', 'UserService', 'ProjectService', 'BasketService', 'JobService', 'ProductService', 'FileService', 'MessageService', 'TabService', 'CommonService', '$injector', function ($scope, CommunityService, GroupService, UserService, ProjectService, BasketService, JobService, ProductService, FileService, MessageService, TabService, CommonService, $injector) {

        $scope.navInfo = TabService.navInfo.community;

        $scope.groupParams = GroupService.params.community;
        $scope.projectParams = ProjectService.params.community;
        $scope.basketParams = BasketService.params.community;
        $scope.jobParams = JobService.params.community;
        $scope.serviceParams = ProductService.params.community;
        $scope.fileParams = FileService.params.community;

        /* Get current user */
        $scope.user = UserService.params.activeUser;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        /* Sidebar navigation */
        $scope.communityTabs = TabService.getCommunityNavTabs();

        function showSidebarArea() {
            $scope.navInfo.sideViewVisible = true;
        }

        $scope.hideSidebarArea = function () {
            $scope.navInfo.sideViewVisible = false;
        };

        $scope.togglePage = function (tab) {
            if($scope.navInfo.activeSideNav === tab && $scope.navInfo.sideViewVisible) {
                $scope.hideSidebarArea();
            } else if($scope.navInfo.activeSideNav === tab && !$scope.navInfo.sideViewVisible) {
                showSidebarArea();
            } else {
                $scope.navInfo.activeSideNav = tab;
                showSidebarArea();
            }
        };

        /** Bottom bar **/
        $scope.displayTab = function(tab){
            $scope.navInfo.bottomViewVisible = true;
            $scope.navInfo.activeBottomNav = tab;
        };

        $scope.toggleBottomView = function(){
            $scope.navInfo.bottomViewVisible = !$scope.navInfo.bottomViewVisible;
        };

        /* Sharing */

        /* Share Object Modal */
        $scope.ace = {};
        $scope.shareObjectDialog = function($event, item, type, groups, serviceName, serviceMethod) {
            CommonService.shareObjectDialog($event, item, type, groups, serviceName, serviceMethod, 'community');
        };

        $scope.updateGroups = function (item, type, groups, serviceName, serviceMethod) {
            var service = $injector.get(serviceName);
            CommunityService.updateObjectGroups(item, type, groups).then(function (data) {
                service[serviceMethod]('community');
            });
        };

        $scope.removeGroup = function (item, type, group, groups, serviceName, serviceMethod) {
            var service = $injector.get(serviceName);
            CommunityService.removeAceGroup(item, type, group, groups).then(function (data) {
                service[serviceMethod]('community');
            });
        };

        $scope.displaySidebar = function () {
            if(!$scope.groupParams.selectedGroup && $scope.navInfo.activeSideNav === $scope.communityTabs.GROUPS) {
               return true;
            } else if(!$scope.projectParams.selectedProject && $scope.navInfo.activeSideNav === $scope.communityTabs.PROJECTS) {
                return true;
            } else if(!$scope.basketParams.selectedDatabasket && $scope.navInfo.activeSideNav === $scope.communityTabs.DATABASKETS) {
                return true;
            } else if(!$scope.jobParams.selectedJob && $scope.navInfo.activeSideNav === $scope.communityTabs.JOBS) {
                return true;
            } else if(!$scope.serviceParams.selectedService && $scope.navInfo.activeSideNav === $scope.communityTabs.SERVICES) {
                return true;
            } else if(!$scope.fileParams.selectedFile && $scope.navInfo.activeSideNav === $scope.communityTabs.FILES) {
                return true;
            }
            return false;
        };

        $scope.hideContent = true;
        var navbar, sidenav, groups, projects, databaskets, jobs, services, files;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'groups':
                    groups = true;
                    break;
                case 'projects':
                    projects = true;
                    break;
                case 'databaskets':
                    databaskets = true;
                    break;
                case 'jobs':
                    jobs = true;
                    break;
                case 'services':
                    services = true;
                    break;
                case 'files':
                    files = true;
                    break;
            }

            if (navbar && sidenav && (groups || projects || databaskets || jobs || services || files)) {
                $scope.hideContent = false;
            }
        };

    }]);
});
