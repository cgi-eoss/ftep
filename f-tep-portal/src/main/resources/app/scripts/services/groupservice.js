/**
 * Created by dashi on 03-08-2016.
 */
define(['../ftepmodules'], function (ftepmodules) {
  'use strict';

  ftepmodules.service('GroupService', [ '$rootScope', '$http', 'ftepProperties', '$q', 'MessageService',
                                        function ($rootScope, $http, ftepProperties, $q, MessageService) {

    $http.defaults.headers.post['Content-Type'] = 'application/json';
    $http.defaults.withCredentials = true;

    //GET Method to get default Groups
    this.getGroups = function(){
      var deferred = $q.defer();
      $http.get( ftepProperties.URL + '/groups').then(function(response) {
        deferred.resolve(response.data.data);
      })
      .catch(function(e){
          MessageService.addError(
              'Could not get Groups'
          );
          deferred.reject();
      });
      return deferred.promise;
    };

    //POST Method to create new Groups
    this.createGroup = function(name, desc){
      return $q(function(resolve, reject) {
        //added var group which takes in attributes name and description
        var group = {type: 'groups', attributes:{name: name, description: (desc ? desc : '')}};
        $http({
          method: 'POST',
          url: ftepProperties.URL + '/groups',
          data: '{"data": ' + JSON.stringify(group) + '}',
        }).
        then(function(response) {
          resolve(response.data.data);
          MessageService.addInfo('Group created', 'New group '.concat(name).concat(' created'));
        }).
        catch(function(e) {
          if(e.status == 409){
            MessageService.addError(
                'Could not create group',
                'Conflicts with an already existing one'
            );
          }
          reject();
        });
      });
    };

    //DELETE Method to remove group
    this.removeGroup = function(group){
      return $q(function(resolve, reject) {
        //next two lines are repeated again-why?
        $http.defaults.headers.post['Content-Type'] = 'application/json';
        $http.defaults.withCredentials = true;
        $http({
          method: 'DELETE',
          url: ftepProperties.URL + '/groups/' + group.id
        }).
        then(function(response) {
          resolve(group);
          MessageService.addInfo('Group deleted', 'Group '.concat(group.attributes.name).concat(' deleted'));
        }).
        catch(function(e) {
            MessageService.addError(
                'Failed to remove Group'
            );
            console.log(e);
            reject();
        });
      });
    };

    //PATCH Method to edit an existing group
    this.updateGroup = function(group){
        return $q(function(resolve, reject) {

            delete group.attributes.id;
            $http({
                method: 'PATCH',
                url: ftepProperties.URL + '/groups/' + group.id,
                data: '{"data": ' + JSON.stringify(group) + '}',
            }).
            then(function(response) {
                resolve(group);
                MessageService.addInfo('Group updated', 'Group '.concat(group.attributes.name).concat(' updated'));
            }).
            catch(function(e) {
                MessageService.addError(
                    'Failed to update Group'
                );
                console.log(e);
                reject();
            });
        });
    };

    return this;
  }]);
});
