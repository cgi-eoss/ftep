/**
 * @ngdoc service
 * @name ftepApp.CommunityService
 * @description
 * # CommunityService
 * Service in the ftepApp.
 */
'use strict';
define(['../ftepmodules'], function (ftepmodules) {

    ftepmodules.service('CommunityService', ['ftepProperties', 'MessageService', '$http', '$q',  function (ftepProperties, MessageService, $http, $q) {

        var rootUri = ftepProperties.URLv2;

        this.permissionTypes = {
                READ: "READ",
                EDIT: "WRITE",
                ADMIN: "ADMIN"
        };

        function getItemName(ace){
            var name = (ace.name ? ace.name : ( ace.filename ? ace.filename : ace.id));
            if(ace.type === 'job'){
                name = "Job ID: " + ace.id;
            }
            return name;
        }

        this.getObjectGroups = function(item, type) {
            return $q(function(resolve, reject) {
                $http({
                    method: 'GET',
                    url: rootUri + '/acls/' + type + '/' + item.id,
                }).
                then(function (response) {
                    resolve(response.data.permissions);
                }).
                catch(function (e) {
                    MessageService.addError('Could not get ' + item.name + ' shared groups', e);
                    reject();
                });
            });
        };

        this.shareObject = function(ace, groups) {

            return $q(function(resolve, reject) {

                var newObject = {
                    "group": {
                        "id": ace.group.id,
                        "name": ace.group.name
                    },
                    "permission": ace.permission
                };
                var groupArray = [];
                var itemName = getItemName(ace);
                var itemType = ace.type.charAt(0).toUpperCase() + ace.type.slice(1);

                for (var i in groups) {
                    if (groups[i].group.id !== ace.group.id) {
                        groupArray.push(groups[i]);
                    }
                }

                groupArray.push(newObject);

                var aclsObject = {
                    "entityId": ace.id,
                    "permissions": groupArray
                };

                $http({
                    method: 'POST',
                    url: rootUri + '/acls/' + ace.type + '/' + ace.id,
                    data: aclsObject,
                }).
                then(function (response) {
                    resolve(response);
                    MessageService.addInfo(itemType + ' shared', (itemName).concat(' shared to ').concat(ace.group.name));
                }).
                catch(function (e) {
                    MessageService.addError('Could not share ' + itemName, e);
                    reject();
                });

            });

        };

        this.updateObjectGroups = function(item, type, groups) {
            return $q(function(resolve, reject) {

                var groupArray = [];
                var itemName = getItemName(ace);
                var itemType = type.charAt(0).toUpperCase() + type.slice(1);

                for (var i in groups) {
                    groupArray.push(groups[i]);
                }

                var aclsObject = {
                    "entityId": item.id,
                    "permissions": groupArray
                };

                $http({
                    method: 'POST',
                    url: rootUri + '/acls/' + type + '/' + item.id,
                    data: aclsObject,
                }).
                then(function (response) {
                    resolve(response);
                    MessageService.addInfo(itemType + ' updated', (itemName).concat(' has been saved.'));
                }).
                catch(function (e) {
                    MessageService.addError('Could not save ' + itemName, e);
                    reject();
                });

            });
        };

        this.removeAceGroup = function(item, type, group, groups) {
            return $q(function(resolve, reject) {

                var groupArray = [];
                var itemName = getItemName(item);
                var itemType = type.charAt(0).toUpperCase() + type.slice(1);

                for (var i in groups) {
                    if (groups[i].group.id !== group.id) {
                        groupArray.push(groups[i]);
                    }
                }

                var aclsObject = {
                    "entityId": item.id,
                    "permissions": groupArray
                };

                $http({
                    method: 'POST',
                    url: rootUri + '/acls/' + type + '/' + item.id,
                    data: aclsObject,
                }).
                then(function (response) {
                    MessageService.addInfo(itemType + ' removed from group', (itemName).concat(' removed from ').concat(group.name));
                    resolve(response);
                }).
                catch(function (e) {
                    MessageService.addError('Could not remove ' + itemName + ' from group', e);
                    reject();
                });

            });
        };

    }]);
});

