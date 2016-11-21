'use strict';

module.exports = {
    dist: {
        options: {
            collapseWhitespace: true,
            conservativeCollapse: true,
            collapseBooleanAttributes: true,
            removeCommentsFromCDATA: true
        },
        files: [{
            expand: true,
            cwd: '<%= ftep.dist %>',
            src: ['**/*.html'],
            dest: '<%= ftep.dist %>'
        }]
    }
};
