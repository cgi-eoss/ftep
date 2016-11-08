'use strict';

describe('Controller: IndexCtrl', function () {

  // load the controller's module
  beforeEach(module('ftepApp'));

  var IndexCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    IndexCtrl = $controller('IndexCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(IndexCtrl.awesomeThings.length).toBe(3);
  });
});
