/**
 * Created by dashi on 03-08-2016.
 */

  'use strict';

define(['../ftepmodules', 'traversonHal'], function (ftepmodules, TraversonJsonHalAdapter) {

    ftepmodules.service('GroupService', [ 'ftepProperties', '$q', 'MessageService', 'traverson', function (ftepProperties, $q, MessageService, traverson) {

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = ftepProperties.URLv2;
        var groupsAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            groups: [],
            selectedGroup: undefined,
            searchText: '',
            displayGroupFilters: false
        };
        /** END OF PRESERVE USER SELECTIONS **/

        this.getGroups = function () {
            var deferred = $q.defer();
            groupsAPI.from(rootUri + '/groups/')
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document._embedded.groups);
            }, function (error) {
                MessageService.addError ('Could not get Groups', error);
                deferred.reject();
            });
            return deferred.promise;
        };

        this.createGroup = function(name, desc) {
            return $q(function(resolve, reject) {
                var group = {name: name, description: (desc ? desc : '')};
                groupsAPI.from(rootUri + '/groups/')
                         .newRequest()
                         .post(group)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Group created', 'New group '.concat(name).concat(' created.'));
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError ('Failed to Create Group', error);
                    reject();
                });
            });
        };

        this.removeGroup = function(group) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('Group deleted', 'Group '.concat(group.name).concat(' deleted.'));
                        resolve(group);
                    } else {
                        MessageService.addError ('Failed to Remove Group', document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError ('Failed to Remove Group', error);
                    reject();
                });
            });
        };

        this.updateGroup = function (group) {
            var newgroup = {name: group.name, description: group.description};
            return $q(function(resolve, reject) {
                groupsAPI.from(rootUri + '/groups/' + group.id)
                         .newRequest()
                         .patch(newgroup)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('Group successfully updated', 'Group ' + group.name + ' successfully updated.' );
                    resolve(JSON.parse(document.data));
                }, function (error) {
                    MessageService.addError('Failed to update Group', error);
                    reject();
                });
            });
        };

    return this;
  }]);
});
