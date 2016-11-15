'use strict';

describe('Controller: WorkspaceCtrl', function () {

  // load the controller's module
  beforeEach(module('ftepApp'));

  var WorkspaceCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    WorkspaceCtrl = $controller('WorkspaceCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(WorkspaceCtrl.awesomeThings.length).toBe(3);
  });
});
