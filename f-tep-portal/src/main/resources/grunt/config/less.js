 'use strict';

// Watches less files and coverts them to css
 module.exports = {
     options: {
         paths: ["<%= ftep.app %>/styles/less"],
         yuicompress: true
     },
     files: {
         expand: true,
         cwd: '<%= ftep.app %>/styles/less',
         src: ['**/*.less', '!{variable, mixins}*.less'],
         dest: '<%= ftep.app %>/styles',
         ext: '.css'
     },
 };
