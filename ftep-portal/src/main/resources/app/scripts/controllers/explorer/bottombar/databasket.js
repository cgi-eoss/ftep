/**
 * @ngdoc function
 * @name ftepApp.controller:DatabasketCtrl
 * @description
 * # DatabasketCtrl
 * Controller of the ftepApp
 */
define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('DatabasketCtrl', ['$scope', '$rootScope', 'CommonService', 'BasketService',
                                 function ($scope, $rootScope, CommonService, BasketService) {

            $scope.dbPaging = {
                dbCurrentPage: 1,
                dbPageSize: 5,
                dbTotal: 0
            };

            $scope.databaskets = [];

            $scope.databaskets = [
                {
                    "type": "databaskets",
                    "id": "265",
                    "attributes": {
                        "name": "test",
                        "description": ""
                    },
                    "relationships": {
                        "files": {
                            "data": []
                         }
                     }
                },
                {
                    "type": "databaskets",
                    "id": "265",
                    "attributes": {
                        "name": "test",
                        "description": ""
                    },
                    "relationships": {
                        "files": {
                            "data": []
                         }
                     }
                },
                {
                    "type": "databaskets",
                    "id": "265",
                    "attributes": {
                        "name": "test",
                        "description": ""
                    },
                    "relationships": {
                        "files": {
                            "data": []
                         }
                     }
                },
                {
                    "type": "databaskets",
                    "id": "265",
                    "attributes": {
                        "name": "test",
                        "description": ""
                    },
                    "relationships": {
                        "files": {
                            "data": []
                         }
                     }
                }
            ];
            var collectedFiles = {};

            $scope.fetchDbPage = function (page) {
                $scope.dbPaging.dbCurrentPage = page;
                BasketService.getDatabaskets(page, $scope.dbPaging.dbPageSize).then(function (result) {
                    $scope.databaskets = result.data;
                    $scope.dbPaging.dbTotal = result.meta.total[0];
                    collectFiles(result.included);
                });
            };
            $scope.fetchDbPage($scope.dbPaging.dbCurrentPage);

            function collectFiles(files) {
                collectedFiles = {};
                for (var i = 0; i < files.length; i++) {
                    collectedFiles[files[i].id] = files[i];
                }
            }

            $scope.$on('add.basket', function (event, basket) {
                $scope.fetchDbPage($scope.dbPaging.dbCurrentPage);
            });

            $scope.$on('refresh.databaskets', function (event, result) {
                $scope.databaskets = result.data;
                $scope.dbPaging.dbTotal = result.meta.total[0];
            });

            $scope.removeDatabasket = function (event, basket) {
                CommonService.confirm(event, 'Are you sure you want to delete databasket ' + basket.attributes.name + "?").then(function (confirmed) {
                    if (confirmed === false) {
                        return;
                    }
                    BasketService.removeBasket(basket).then(function (data) {
                        $rootScope.$broadcast('delete.databasket', basket);
                        if ($scope.databaskets.length === 0) {
                            $scope.dbPaging.dbCurrentPage = $scope.dbPaging.dbCurrentPage - 1;
                        }
                        $scope.fetchDbPage(pgNr);
                    });
                });
            };

            /* Show databasket details */
            $scope.showDatabasket = function (basket) {
                BasketService.getItems(basket).then(function (result) {
                    $rootScope.$broadcast('update.databasket', basket, result.files);
                });
            };

            /* Show databasket items on map */
            $scope.dbLoaded = {
                id: undefined
            };

            $scope.loadBasket = function (basket) {
                var basketFiles = [];
                $scope.dbLoaded.id = basket.id;
                if (basket.relationships.files && basket.relationships.files.data.length > 0) {
                    for (var i = 0; i < basket.relationships.files.data.length; i++) {
                        var file = collectedFiles[basket.relationships.files.data[i].id];
                        basketFiles.push(file);
                    }
                }
                $rootScope.$broadcast('upload.basket', basketFiles);
            };

            /* Hide databasket items on map */
            $scope.unloadBasket = function (basket) {
                $rootScope.$broadcast('unload.basket');
                $scope.dbLoaded.id = undefined;
            };

            $scope.updateDatabasket = function (enterClicked, basket) {
                if (enterClicked) {
                    console.log('Update basket: ', basket);
                    BasketService.updateBasket(basket).then(function () {
                        basketCache[basket.id] = undefined;
                    });
                }
                return !enterClicked;
            };

            var basketCache = {};
            $scope.cacheBasket = function (basket) {
                if (basketCache[basket.id] === undefined) {
                    basketCache[basket.id] = angular.copy(basket);
                }
            };

            $scope.getBasketCache = function (basket) {
                var result;
                if (basketCache[basket.id] !== undefined) {
                    result = angular.copy(basketCache[basket.id]);
                    basketCache[basket.id] = undefined;
                }
                return result;
            };

            $scope.cloneDatabasket = function (event, basket) {
                BasketService.getItems(basket).then(function (result) {
                    $scope.createDatabasketDialog(event, result.files);
                });
            };

            $scope.getBasketDragItems = function (basket) {
                var str = "";
                var firstIsDone = false;
                if (basket.relationships.files && basket.relationships.files.data.length > 0) {
                    for (var i = 0; i < basket.relationships.files.data.length; i++) {
                        var file = collectedFiles[basket.relationships.files.data[i].id];
                        if (file.attributes.properties && file.attributes.properties.details.file && str.indexOf(file.attributes.properties.details.file.path) < 0) {
                            str = str.concat(',', file.attributes.properties.details.file.path);
                        }
                    }
                }
                return str.substr(1);
            };
    }]);
});
