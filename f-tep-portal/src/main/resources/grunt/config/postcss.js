'use strict';

// Add vendor prefixed styles
module.exports = {
    options: {
        processors: [
            require('autoprefixer')({browsers: ['last 2 versions']})
        ]
    },
    dist: {
        files: [{
            expand: true,
            cwd: '.tmp/styles',
            src: ['**/*.css'],
            dest: '.tmp/post/styles/'
        }]
    }
};
