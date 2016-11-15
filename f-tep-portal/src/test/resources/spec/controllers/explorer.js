'use strict';

describe('Controller: ExplorerCtrl', function () {

  // load the controller's module
  beforeEach(module('ftepApp'));

  var ExplorerCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    ExplorerCtrl = $controller('ExplorerCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(ExplorerCtrl.awesomeThings.length).toBe(3);
  });
});
