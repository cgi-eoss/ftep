'use strict';

// ng-annotate tries to make the code safe for minification automatically
// by using the Angular long form for dependency injection.
module.exports = {
    dist: {
        options: {
            singleQuotes: true
        },
        files: [{
            expand: true,
            src: ['.tmp/concat/scripts/**/*.js']
            //ext: '.annotated.js'
        }]
    }
};
