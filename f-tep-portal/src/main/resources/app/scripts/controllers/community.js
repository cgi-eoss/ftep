/**
 * @ngdoc function
 * @name ftepApp.controller:CommunityCtrl
 * @description
 * # CommunityCtrl
 * Controller of the ftepApp
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('CommunityCtrl', ['$scope', '$http', 'ftepProperties', 'GroupService', '$q', '$rootScope', 'CommonService', '$mdDialog',
                              function ($scope, $http, ftepProperties, GroupService, $q, $rootScope, CommonService, $mdDialog) {


            // Get exisitng groups using GET from groupservice.js
            $scope.groups = [];
            GroupService.getGroups().then(function (data) {
                $scope.groups = data;
                console.log("Groups:" + data);
            });

            //adding group using POST from groupservice.js

            /* Create Group Modal */
            $scope.newGroup = {
                name: undefined,
                description: undefined
            };
            $scope.createGroupDialog = function ($event) {
                var parentEl = angular.element(document.body);
                $mdDialog.show({
                    parent: parentEl,
                    targetEvent: $event,
                    template: '<md-dialog id="group-dialog" aria-label="Create group dialog">' +
                        '    <h4>Add Group</h4>' +
                        '  <md-dialog-content>' +
                        '    <div class="dialog-content-area">' +
                        '        <md-input-container class="md-block" flex-gt-sm>' +
                        '           <label>Name</label>' +
                        '           <input ng-model="newGroup.name" type="text"></input>' +
                        '       </md-input-container>' +
                        '       <md-input-container class="md-block" flex-gt-sm>' +
                        '           <label>Description</label>' +
                        '           <textarea ng-model="newGroup.description"></textarea>' +
                        '       </md-input-container>' +
                        '    </div>' +
                        '  </md-dialog-content>' +
                        '  <md-dialog-actions>' +
                        '    <md-button ng-click="addGroup()" ng-disabled="!newGroup.name" class="md-primary">Create</md-button>' +
                        '    <md-button ng-click="closeDialog()" class="md-primary">Cancel</md-button>' +
                        '  </md-dialog-actions>' +
                        '</md-dialog>',
                    controller: DialogController,
                    locals: {
                        items: $scope.items
                    }
                });

                function DialogController($scope, $mdDialog, GroupService) {
                    $scope.closeDialog = function () {
                        $mdDialog.hide();
                    };
                    $scope.addGroup = function () {
                        GroupService.createGroup($scope.newGroup.name, $scope.newGroup.description).then(function (data) {
                            $rootScope.$broadcast('add.group', data);
                        });
                        $mdDialog.hide();
                    };
                }
            };
            $scope.$on('add.group', function (event, data) {
                $scope.groups.push(data);
            });

            //Removing Groups using DELETE from groupservice.js
            $scope.removeGroup = function (group) {
                GroupService.removeGroup(group).then(function (data) {
                    var i = $scope.groups.indexOf(group);
                    $scope.groups.splice(i, 1);
                });
            };
            //////////////////////edit this when Member attribute is working in the JSON
            /*
            //Removing a single member from a GROUP
            $scope.removeMember = function(pid, cid) {
                pid.splice(pid.indexOf(cid), 1);
                //$scope.data[pid].relationships.user.data.splice(cid, 1);
            };
            */
            /////////////////////////////////////////////////////////////////////////////////

            //Update Groups using UPDATE from groupservice.js

            $scope.showGroup = function (group) {
                $rootScope.$broadcast('update.group', group, []);
            };

            $scope.loadGroup = function (group) {
                $rootScope.$broadcast('upload.group', group);
            };

            $scope.updateGroup = function (enterClicked, group) {
                if (enterClicked) {
                    console.log('Update Group:', group);
                    GroupService.updateGroup(group).then(function () {
                        groupCache[group.id] = undefined;
                    });
                }
                return !enterClicked;
            };

            var groupCache = {};
            $scope.cacheGroup = function (group) {
                if (groupCache[group.id] === undefined) {
                    groupCache[group.id] = angular.copy(group);
                }
            };

            $scope.getGroupCache = function (group) {
                var result;
                if (groupCache[group.id] !== undefined) {
                    result = angular.copy(groupCache[group.id]);
                    groupCache[group.id] = undefined;
                }
                return result;
            };

      }]);

});
