'use strict';

// Watches files for changes and runs tasks based on the changed files
module.exports = {
    bower: {
        files: ['bower.json'],
        tasks: ['wiredep']
    },
    js: {
        files: ['<%= ftep.app %>/scripts/{,*/}*.js'],
        tasks: ['newer:jshint:all', 'newer:jscs:all'],
        options: {
            livereload: '<%= connect.options.livereload %>'
        }
    },
    jsTest: {
        files: ['test/spec/{,*/}*.js'],
        tasks: ['newer:jshint:test', 'newer:jscs:test', 'karma']
    },
    styles: {
        files: ['<%= ftep.app %>/styles/**/*.*'],
        tasks: ['less', 'newer:postcss', 'concat']
    }
};
