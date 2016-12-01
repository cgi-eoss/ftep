'use strict';

module.exports = function (grunt) {
    grunt.registerTask('dist', [
        'clean:dist',           // Clean up files and folders
        'wiredep',              // Wire Bower dependencies into index.html.
        'useminPrepare',        // Reads usemin blocks to enable automatic concat, minify and revision of files.
        'less',                 // Compile LESS files to CSS
        'postcss',              // Post-process CSS with autoprefixer
        'cssmin',               // Compress CSS files
        'imagemin',             // Minify PNG, JPG, and GIFs
        'svgmin',               // Minify SVGs
        'ngtemplates',          // Minify, combine and cache HTML templates (not used?)
        'requirejs',            // Optimise RequireJS project
        'copy:dist',            // Copy files and folders
        'concat',               // Combine files into a single file
        'ngAnnotate',           // Add, remove and rebuild dependency injection annotations
        'uglify',               // Minify JS files using UglifyJS
        'filerev',              // Static asset revisioning through file content hash
        'usemin',               // Replaces references to non-optimized scripts / stylesheets into a set of HTML files
        'htmlmin'               // Minify HTML files
      ]);
};
