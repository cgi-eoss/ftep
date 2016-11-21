'use strict';

// Make sure there are no obvious mistakes
module.exports = {
    options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
    },
    all: {
        src: [
          '<%= ftep.app %>/scripts/{,*/}*.js'
        ]
    },
    test: {
        options: {
            jshintrc: 'test/.jshintrc'
        },
        src: ['test/spec/{,*/}*.js']
    }
};
