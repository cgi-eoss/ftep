'use strict';

module.exports = {
    dist: {
        files: [{
            expand: true,
            cwd: '<%= ftep.app %>/images',
            src: '{,*/}*.svg',
            dest: '<%= ftep.dist %>/images'
        }]
    }
};
