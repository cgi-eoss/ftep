'use strict';

describe('Controller: HelpdeskCtrl', function () {

  // load the controller's module
  beforeEach(module('ftepApp'));

  var HelpdeskCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    HelpdeskCtrl = $controller('HelpdeskCtrl', {
      $scope: scope
      // place here mocked dependencies
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(HelpdeskCtrl.awesomeThings.length).toBe(3);
  });
});
