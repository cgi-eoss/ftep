'use strict';

module.exports = function (grunt) {
    grunt.registerTask('test', [
        'connect:test',
        'karma'
    ]);
};
