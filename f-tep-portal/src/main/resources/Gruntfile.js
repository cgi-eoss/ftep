'use strict';

module.exports = function (grunt) {

    var path = require('path');

    // Time how long tasks take. Can help when optimizing build times
    require('time-grunt')(grunt);

    grunt.loadNpmTasks('grunt-usemin');
    grunt.loadNpmTasks('grunt-angular-templates');

    require('load-grunt-config')(grunt, {
        configPath: path.join(process.cwd(), 'grunt/config'),
        jitGrunt: {
            customTasksDir: 'grunt/tasks',
        },
        data: {
            ftep: {
                app: require('./bower.json').appPath || 'app',
                dist: 'dist'
            }
        }
    });
};
