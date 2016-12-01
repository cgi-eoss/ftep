'use strict';

module.exports = function(grunt) {
    grunt.registerTask('default', [
        'newer:jshint',
        'newer:jscs',
        'build',
        'test'
      ]);
};
