'use strict';

// Automatically inject Bower components into the app
module.exports = {
    app: {
        src: ['<%= ftep.app %>/index.html'],
        ignorePath: /\.\.\//
    },
    test: {
        devDependencies: true,
        src: '<%= karma.unit.configFile %>',
        ignorePath: /\.\.\//,
        fileTypes: {
            js: {
                block: /(([\s\t]*)\/{2}\s*?bower:\s*?(\S*))(\n|\r|.)*?(\/{2}\s*endbower)/gi,
                detect: {
                    js: /'(.*\.js)'/gi
                },
                replace: {
                    js: '\'{{filePath}}\','
                }
            }
        }
    }
};
