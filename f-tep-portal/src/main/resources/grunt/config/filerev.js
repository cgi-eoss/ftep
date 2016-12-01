'use strict';

// Renames files for browser caching purposes
module.exports = {
        dist: {
            src: [
              '<%= ftep.dist %>/scripts/{,*/}*.js',
              '<%= ftep.dist %>/styles/{,*/}*.css',
              '<%= ftep.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
              '<%= ftep.dist %>/styles/fonts/*'
            ]
        }
};
