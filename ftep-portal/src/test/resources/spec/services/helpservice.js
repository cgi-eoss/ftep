'use strict';

describe('Service: helpservice', function () {

  // load the service's module
  beforeEach(module('ftepApp'));

  // instantiate service
  var helpservice;
  beforeEach(inject(function (_helpservice_) {
    helpservice = _helpservice_;
  }));

  it('should do something', function () {
    expect(!!helpservice).toBe(true);
  });

});
