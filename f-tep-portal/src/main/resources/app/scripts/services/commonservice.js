/**
 * @ngdoc service
 * @name ftepApp.CommonService
 * @description
 * # CommonService
 * Service in the ftepApp.
 */
'use strict';
define(['../ftepmodules'], function (ftepmodules) {

    ftepmodules.service('CommonService', ['$rootScope', 'ftepProperties', '$mdDialog', '$q', '$injector', function ($rootScope, ftepProperties, $mdDialog, $q, $injector) {

        var self = this;

        this.getColor = function (status) {
            if ("COMPLETED" === status || "approved" === status) {
                return "background: #dff0d8; border: 2px solid #d0e9c6; color: #3c763d";
            } else if ("ERROR" === status || "Error" === status) {
                return "background: #f2dede; border: 2px solid #ebcccc; color: #a94442";
            } else if ("RUNNING" === status || "Info" === status) {
                return "background: #d9edf7; border: 2px solid #bcdff1; color: #31708f";
            } else if ("Warning" === status) {
                return "background: #fcf8e3; border: 2px solid #faf2cc; color: #8a6d3b";
            }
        };

        this.confirm = function (event, message) {
            var deferred = $q.defer();
            var dialog = $mdDialog.confirm()
                .title('Confirmation needed')
                .targetEvent(event)
                .ok('Confirm')
                .cancel('Cancel');

            if (message.indexOf('\n') > -1) {
                dialog.htmlContent(getHtml(message));
            }
            else {
                dialog.textContent(message);
            }

            $mdDialog.show(dialog).then(function () {
                deferred.resolve(true);
            }, function () {
                deferred.resolve(false);
            });
            return deferred.promise;
        };

        this.infoBulletin = function (event, message) {
            var dialog = $mdDialog.alert()
                .title('Info')
                .targetEvent(event)
                .ok('Ok');

            if (message.indexOf('\n') > -1) {
                dialog.htmlContent(getHtml(message));
            }
            else {
                dialog.textContent(message);
            }

            $mdDialog.show(dialog);
        };

        /* Split the message into separate paragraphs if it includes end of line. */
        function getHtml(message) {
            var paragraphs = message.split('\n');
            var html = '';
            for (var i = 0; i < paragraphs.length; i++) {
                html += '<p>' + paragraphs[i] + '</p>';
            }
            return html;
        }

        this.createItemDialog = function ($event, serviceName, serviceMethod) {
            var deferred = $q.defer();

            function CreateItemController($scope, $mdDialog) {

                $scope.item = serviceName.substring(0, serviceName.indexOf('Service'));
                var service = $injector.get(serviceName);

                $scope.addItem = function () {
                    service[serviceMethod]($scope.newItem.name, $scope.newItem.description).then(function (createdItem) {
                            deferred.resolve(createdItem);
                        },
                        function (error) {
                            deferred.reject();
                        });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    deferred.reject();
                    $mdDialog.hide();
                };
            }

            CreateItemController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: CreateItemController,
                templateUrl: 'views/common/templates/createitem.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });

            return deferred.promise;
        };

        this.editItemDialog = function ($event, item, serviceName, serviceMethod) {
            var deferred = $q.defer();

            function EditController($scope, $mdDialog) {

                /* Save temporary changes */
                $scope.tempItem = angular.copy(item);
                $scope.itemName = item.name;
                var service = $injector.get(serviceName);

                /* Patch databasket and update item list */
                $scope.updateItem = function () {
                    service[serviceMethod]($scope.tempItem).then(function (data) {
                            deferred.resolve(data);
                        },
                        function (error) {
                            deferred.reject();
                        });
                    $mdDialog.hide();
                };

                /* Patch databasket and update databasket list */
                $scope.deleteItem = function (event, itemName) {
                    if (!itemName) {
                        itemName = "";
                    }
                    self.confirm(event, 'Are you sure you want to delete this item: "' + itemName + '"?').then(function (confirmed) {
                        if (confirmed === false) {
                            return;
                        }
                        var deleteMethod = serviceMethod.replace("update", "remove");
                        service[deleteMethod](item).then(function (data) {
                                deferred.resolve(data);
                            },
                            function (error) {
                                deferred.reject();
                            });
                        $mdDialog.hide();
                    });
                };

                $scope.closeDialog = function () {
                    deferred.reject();
                    $mdDialog.hide();
                };
            }

            EditController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: EditController,
                templateUrl: 'views/common/templates/edititem.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
            return deferred.promise;
        };

        this.shareObjectDialog = function ($event, item, type, groups, serviceName, serviceMethod, page) {
            function ShareObjectController($scope, $mdDialog, GroupService, CommunityService) {

                var service = $injector.get(serviceName);
                $scope.permissions = CommunityService.permissionTypes;
                $scope.ace = item;
                $scope.ace.type = type;
                $scope.ace.permission = $scope.permissions.READ;
                $scope.groups = [];

                GroupService.getGroups().then(function (data) {
                    $scope.groups = data;
                });

                $scope.shareObject = function (item) {
                    CommunityService.shareObject($scope.ace, groups).then(function (data) {
                        service[serviceMethod](page);
                    });

                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }

            ShareObjectController.$inject = ['$scope', '$mdDialog', 'GroupService', 'CommunityService'];
            $mdDialog.show({
                controller: ShareObjectController,
                templateUrl: 'views/common/templates/shareitem.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {}
            });
        };

        return this;

    }]);
});
