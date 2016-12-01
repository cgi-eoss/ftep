'use strict';

module.exports = {
    dist: {
        options: {
            module: 'ftepApp',
            htmlmin: '<%= htmlmin.dist.options %>'
        },
        cwd: '<%= ftep.app %>',
        src: 'views/{,*/}*.html',
        dest: '<%= ftep.dist %>/scripts/templateCache.js'
    }
};
