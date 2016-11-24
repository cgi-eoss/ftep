module.exports=function(grunt){
	"use strict";
	
	grunt.initConfig({
		pkg: grunt.file.readJSON('package.json'),
		watch: { 
			scripts: {
				files: ['./js/**/*.js', '!./js/jobs.gen.js'],
				// tasks: ['jshint','concat:jobs' ]
				tasks: ['concat:jobs' ]
			}
		},
		concat: { 
			jobs: { 
				src: ['./js/jobs/**/*.js', '!./js/jobs/jobs.gen.js'],
				dest: './js/jobs.gen.js'
			}
		},
		jshint: {
			options: {
				jshintrc: '.jshintrc'
			},
			all: [
			    'js/**/*.js',
			    '!js/**/*.gen.js'
			]
		},
	});
	
	grunt.loadNpmTasks('grunt-contrib-watch');
	// grunt.loadNpmTasks('grunt-contrib-jshint');
	grunt.loadNpmTasks('grunt-contrib-concat');
	
	grunt.registerTask('default', 'watch');
}
