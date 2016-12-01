'use strict';

module.exports = function (grunt) {
    grunt.registerTask('build', [
        'wiredep',
        'less',
        'postcss',
        'concat'
  ]);
};
