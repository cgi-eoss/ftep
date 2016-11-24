var jobs=angular.module('jobs',[]);

jQuery(document).ready(function(){
	angular.bootstrap(document.getElementById('jobs-app'), ['jobs']);
});
jobs.controller('jobscontroller',function($scope, $http){
	$scope.foobar='All jobbbbssss123-4e';
	
	$http.get('/api/v1.0/jobs').
	  success(function(data, status, headers, config) {
		    // this callback will be called asynchronously
		    // when the response is available
		  //$scope.jobs=data.data;
		  $scope.jobs=(function(){
			return data;
		  })()
		  
	  }).
	  error(function(data, status, headers, config) {
		    // called asynchronously if an error occurs
		    // or server returns response with an error status.
		  alert("Got an error HERE");
	  });
})
