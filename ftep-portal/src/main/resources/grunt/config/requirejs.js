'use strict';

module.exports = {
compile: {
    options: {
        baseUrl: 'app',
        mainConfigFile: 'app/main.js',
        out: '.tmp/concat/scripts/explorer.js',
        name: 'main',
        uglify2: {
            mangle: false
        }
    }
  }
};
