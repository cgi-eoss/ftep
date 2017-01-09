'use strict';

/* Reads HTML for usemin blocks to enable smart builds that automatically
 * concat, minify and revision files. Creates configurations in memory so
 * additional tasks can operate on them. */
module.exports = {
    html: '<%= ftep.app %>/index.html',
    css: ['<%= ftep.app %>/main.css'],
    options: {
        dest: '<%= ftep.dist %>',
        flow: {
            html: {
                steps: {
                    js: ['concat', 'uglifyjs'],
                    css: ['cssmin']
                },
                post: {}
            }
        }
    }
};
