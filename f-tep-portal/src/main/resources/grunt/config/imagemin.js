'use strict';

module.exports = {
    dist: {
        files: [{
            expand: true,
            cwd: '<%= ftep.app %>/images',
            src: '{,*/}*.{png,jpg,jpeg,gif}',
            dest: '<%= ftep.dist %>/images'
        }]
    }
};
