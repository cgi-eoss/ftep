/**
 * @ngdoc service
 * @name ftepApp.CommonService
 * @description
 * # CommonService
 * Service in the ftepApp.
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.service('CommonService', [ '$rootScope', 'ftepProperties', '$mdDialog', '$q', '$injector',
                                           function($rootScope, ftepProperties, $mdDialog, $q, $injector) {

                this.getColor = function(status){
                    if("Succeeded" === status || "approved" === status){
                        return "background: #dff0d8; border: 2px solid #d0e9c6; color: #3c763d";
                    } else if("Failed" === status || "Error" === status){
                        return "background: #f2dede; border: 2px solid #ebcccc; color: #a94442";
                    } else if("Running" === status || "Info" === status){
                        return "background: #d9edf7; border: 2px solid #bcdff1; color: #31708f";
                    } else if("Warning" === status){
                        return "background: #fcf8e3; border: 2px solid #faf2cc; color: #8a6d3b";
                    }
                };

                this.getOutputLink = function(link){
                    return  ftepProperties.URL_PREFIX + link;
                };

                this.confirm = function(event, message) {
                    var deferred = $q.defer();
                    var confirm = $mdDialog.confirm()
                          .title('Confirmation needed')
                          .textContent(message)
                          .targetEvent(event)
                          .ok('Confirm')
                          .cancel('Cancel');

                    $mdDialog.show(confirm).then(function() {
                        deferred.resolve(true);
                    }, function() {
                        deferred.resolve(false);
                    });
                    return deferred.promise;
                };

                this.createItemDialog = function($event, serviceName, serviceMethod){
                    var deferred = $q.defer();
                    function CreateItemController($scope, $mdDialog) {

                        $scope.item = serviceName.substring(0, serviceName.indexOf('Service'));
                        var service = $injector.get(serviceName);

                        $scope.addItem = function () {
                            service[serviceMethod]($scope.newItem.name, $scope.newItem.description).then(function (createdItem) {
                                deferred.resolve(createdItem);
                            },
                            function(error){
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

                        /* Patch databasket and update databasket list */
                        $scope.updateItem = function () {
                            service[serviceMethod]($scope.tempItem).then(function(data){
                                deferred.resolve(data);
                            },
                            function(error){
                                deferred.reject();
                            });
                            $mdDialog.hide();
                        };

                        $scope.closeDialog = function() {
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

            return this;
      }]);
});
