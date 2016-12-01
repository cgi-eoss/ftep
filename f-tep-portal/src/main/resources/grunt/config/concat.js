'use strict';

// Empties folders to start fresh
module.exports = {
    options: {
    },
    dist: {
      src: ['.tmp/styles/**/*.css'],
      dest: '<%= ftep.app %>/main.css',
    }
};
