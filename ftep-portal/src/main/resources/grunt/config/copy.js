'use strict';

// Copies remaining files to places other tasks can use
module.exports = {
    dist: {
        files: [{
            expand: true,
            dot: true,
            cwd: '<%= ftep.app %>',
            dest: '<%= ftep.dist %>',
            src: [
                '*.{ico,png,txt}',
                '**/*.html',
                'images/{,*/}*.{webp}',
                'fonts/{,*/}*.*',
                'zoo-client/*'
            ]
        }, {
            expand: true,
            cwd: '.tmp/images',
            dest: '<%= ftep.dist %>/images',
            src: ['generated/*']
        }]
    }
};
