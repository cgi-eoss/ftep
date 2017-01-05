"use strict";

var stream = require("readable-stream");

var Duplex = stream.Duplex;
var Readable = stream.Readable;

var DuplexWrapper = exports.DuplexWrapper = function DuplexWrapper(options, writable, readable) {
  if (readable === undefined) {
    readable = writable;
    writable = options;
    options = {};
  } else {
    options = options || {};
  }

  Duplex.call(this, options);

  if (options.bubbleErrors === undefined) {
    this._bubbleErrors = true;
  } else {
    if (typeof options.bubbleErrors !== "boolean") {
      throw new TypeError(
        String(options.bubbleErrors) +
        " is not a Boolean value. `bubbleErrors` option of duplexer2 must be Boolean (`true` by default)."
      );
    }
    this._bubbleErrors = options.bubbleErrors;
  }
  this._shouldRead = false;

  if (typeof readable.read !== "function") {
    readable = (new Readable()).wrap(readable);
  }

  this._writable = writable;
  this._readable = readable;

  var self = this;

  writable.once("finish", function() {
    self.end();
  });

  this.once("finish", function() {
    writable.end();
  });

  readable.on("readable", function() {
    self._forwardRead();
  });

  readable.once("end", function() {
    return self.push(null);
  });

  if (this._bubbleErrors) {
    writable.on("error", function(err) {
      return self.emit("error", err);
    });

    readable.on("error", function(err) {
      return self.emit("error", err);
    });
  }
};
DuplexWrapper.prototype = Object.create(stream.Duplex.prototype, {constructor: {value: DuplexWrapper}});

DuplexWrapper.prototype._write = function _write(input, encoding, done) {
  this._writable.write(input, encoding, done);
};

DuplexWrapper.prototype._read = function _read() {
  if (this._shouldRead) {
    this.push(null);
    return;
  }

  this._shouldRead = true;
  this._forwardRead();
};

DuplexWrapper.prototype._forwardRead = function _forwardRead() {
  if (!this._shouldRead) {
    return;
  }

  var data;
  var shouldRead = true;

  while ((data = this._readable.read()) !== null) {
    shouldRead = false;
    this.push(data);
  }

  this._shouldRead = shouldRead;
};

module.exports = function duplex2(options, writable, readable) {
  return new DuplexWrapper(options, writable, readable);
};
