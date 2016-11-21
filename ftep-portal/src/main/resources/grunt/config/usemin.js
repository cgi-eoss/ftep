'use strict';

// Performs rewrites based on filerev and the useminPrepare configuration
module.exports = {
    html: ['<%= ftep.dist %>/{,*/}*.html'],
    css: ['<%= ftep.dist %>/styles/{,*/}*.css'],
    js: ['<%= ftep.dist %>/scripts/{,*/}*.js'],
    options: {
        assetsDirs: [
          '<%= ftep.dist %>',
          '<%= ftep.dist %>/images',
          '<%= ftep.dist %>/styles'
        ],
        patterns: {
            js: [[/(images\/[^''""]*\.(png|jpg|jpeg|gif|webp|svg))/g, 'Replacing references to images']]
        }
    }
};
