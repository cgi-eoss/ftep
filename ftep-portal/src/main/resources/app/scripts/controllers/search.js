/**
 * @ngdoc function
 * @name ftepApp.controller:SearchCtrl
 * @description
 * # SearchCtrl
 * Controller of the ftepApp
 */
define(['../ftepmodules', 'clipboard'], function (ftepmodules, clipboard) {
    'use strict';

    ftepmodules.controller('SearchCtrl', ['$scope', '$rootScope', '$http', 'CommonService', 'BasketService', 'GeoService', 'JobService', 'ProductService', 'ProjectService', '$sce',
                                 function ($scope, $rootScope, $http, CommonService, BasketService, GeoService, JobService, ProductService, ProjectService, $sce) {
        this.awesomeThings = [
          'HTML5 Boilerplate',
          'AngularJS',
          'Karma'
        ];

    /** PROJECTS **/
    $scope.projects = [{"type":"project","id":"0","attributes":{"name":"DEFAULT_PROJECT","description":"Default project"}}];
    $scope.activeProject = $scope.projects[0];

    $scope.setActiveProject = function(project){
        $scope.activeProject = project;
    }

    ProjectService.getProjects().then(function(data) {
        $scope.projects.push.apply($scope.projects, data);
    });

    $scope.removeProject = function(project){
        ProjectService.removeProject(project).then(function(data){
            var i = $scope.projects.indexOf(project);
            $scope.projects.splice(i, 1);
        });
    }

    $scope.$on('add.project', function(event, data) {
        $scope.projects.push(data);
    });

    $scope.updateProject = function(enterClicked, project){
        if(enterClicked){
            ProjectService.updateProject(project).then(function(){
                projectCache[project.id] = undefined;
            });
        }
        return !enterClicked;
    }

    var projectCache = {};
    $scope.cacheProject = function(project){
        if(projectCache[project.id] === undefined){
            projectCache[project.id] = angular.copy(project);
        }
    }

    $scope.getProjectCache = function(project){
        var result;
        if(projectCache[project.id] != undefined){
            result = angular.copy(projectCache[project.id]);
            projectCache[project.id] = undefined;
        }
        return result;
    }

    /** END OF PROJECTS **/


    /** SEARCH **/

    $scope.missions = [{name: "sentinel-1A", id: 0}, {name: "sentinel-1B", id: 1}, {name: "sentinel-2A", id: 2}, {name: "sentinel-2B", id: 3}]; //TODO
    $scope.polarisations = [{label: "HH", id: 0}, {label: "HV", id: 1}, {label: "VV", id: 2}, {label: "VH", id: 3}, {label: "HH+HV", id: 4}, {label: "VV+VH", id: 5}];

    function setup(){
        $scope.searchParameters = GeoService.parameters;
        $scope.dataSources = GeoService.dataSources;
        $scope.selectedSource = GeoService.selectedSource;

        if($scope.searchParameters.startTime == undefined || $scope.searchParameters.endTime == undefined){
            var weekago = new Date();
            weekago.setDate(weekago.getDate() - 7);
            $scope.searchParameters.startTime = weekago;
            $scope.searchParameters.endTime = new Date();
        }

        if($scope.searchParameters.mission == undefined){
            $scope.searchParameters.mission = $scope.missions[0];
        }
        else{
            $scope.missions[$scope.searchParameters.mission.id] = $scope.searchParameters.mission;
        }
    }

    setup();

    $scope.dateOptions = {
      formatYear: 'yy',
      minDate: new Date(1970, 1, 1),
      startingDay: 1
    };

    $scope.isCalendarOpened = {
            startTime : false,
            endTime: false
    }

    $scope.openStartCalendar = function() {
        $scope.isCalendarOpened.startTime = true;
    };

    $scope.openEndCalendar = function() {
        $scope.isCalendarOpened.endTime = true;
    };

    $scope.updateSlider = function() {
        $rootScope.$broadcast('update.timeslider', {start: $scope.searchParameters.startTime, end: $scope.searchParameters.endTime});
    }

    $scope.isDateInvalid = function(){
        return $scope.searchParameters.startTime > $scope.searchParameters.endTime;
    };

	$scope.search = function() {
	    GeoService.getGeoResults().then(function(data) {
             $rootScope.$broadcast('update.geoResults', data);
        })
        .catch(function(fallback) {
            GeoService.spinner.loading = false;
        });
	};

	$scope.$on('polygon.drawn', function(event, polygon) {
	    if(polygon){
	        $scope.searchParameters.polygon = polygon;
        }
	    else{
	        delete $scope.searchParameters.polygon;
	    }
	});

    $scope.$on('update.timeRange', function(event, range) {
        if(range.start > range.end){
            var end = range.start;
            range.start = range.end;
            range.end = end;
        }
        $scope.searchParameters.startTime = new Date(range.start);
        $scope.searchParameters.endTime = new Date(range.end);
    });

    /** END OF SEARCH **/


	/** DATABASKETS **/

    $scope.dbPaging = {dbCurrentPage: 1, dbPageSize: 5, dbTotal: 0};

	$scope.databaskets = [];
	var collectedFiles = {};

	$scope.fetchDbPage = function(page){
	    $scope.dbPaging.dbCurrentPage = page;
	    BasketService.getDatabaskets(page, $scope.dbPaging.dbPageSize).then(function(result) {
            $scope.databaskets= result.data;
            $scope.dbPaging.dbTotal = result.meta.total[0];
            collectFiles(result.included);
	    });
	}
	$scope.fetchDbPage($scope.dbPaging.dbCurrentPage);

	function collectFiles(files){
	    collectedFiles = {};
	    for(var i = 0; i < files.length; i++){
	        collectedFiles[files[i].id] = files[i];
	    }
	}

    $scope.$on('add.basket', function(event, basket) {
        $scope.fetchDbPage($scope.dbPaging.dbCurrentPage);
    });

    $scope.$on('refresh.databaskets', function(event, result) {
        $scope.databaskets= result.data;
        $scope.dbPaging.dbTotal = result.meta.total[0];
    });

    $scope.removeDatabasket = function(event, basket) {
        CommonService.confirm(event, 'Are you sure you want to delete databasket ' + basket.attributes.name + "?").then(function(confirmed){
            if(confirmed == false){
                return;
            }
            BasketService.removeBasket(basket).then( function(data){
                $rootScope.$broadcast('delete.databasket', basket);
                if($scope.databaskets.length == 0){
                    $scope.dbPaging.dbCurrentPage = $scope.dbPaging.dbCurrentPage -1;
                }
                $scope.fetchDbPage(pgNr);
            });
        });
    }

    /* Show databasket details */
    $scope.showDatabasket = function(basket) {
        BasketService.getItems(basket).then(function(result){
            $rootScope.$broadcast('update.databasket', basket, result.files);
        });
    }

    /* Show databasket items on map */
    $scope.dbLoaded = {id: undefined};

    $scope.loadBasket = function(basket) {
        var basketFiles = [];
        $scope.dbLoaded.id = basket.id;
        if(basket.relationships.files && basket.relationships.files.data.length > 0){
            for(var i = 0; i < basket.relationships.files.data.length; i++){
                var file = collectedFiles[basket.relationships.files.data[i].id];
                basketFiles.push(file);
            }
        }
        $rootScope.$broadcast('upload.basket', basketFiles);
    }

    /* Hide databasket items on map */
    $scope.unloadBasket = function(basket) {
        $rootScope.$broadcast('unload.basket');
        $scope.dbLoaded.id = undefined;
    }

    $scope.updateDatabasket = function(enterClicked, basket){
        if(enterClicked){
            console.log('Update basket: ', basket);
            BasketService.updateBasket(basket).then(function(){
                basketCache[basket.id] = undefined;
            });
        }
        return !enterClicked;
    }

    var basketCache = {};
    $scope.cacheBasket = function(basket){
        if(basketCache[basket.id] === undefined){
            basketCache[basket.id] = angular.copy(basket);
        }
    }

    $scope.getBasketCache = function(basket){
        var result;
        if(basketCache[basket.id] != undefined){
            result = angular.copy(basketCache[basket.id]);
            basketCache[basket.id] = undefined;
        }
        return result;
    }

    $scope.cloneDatabasket = function(event, basket){
        BasketService.getItems(basket).then(function(result){
            $scope.createDatabasketDialog(event, result.files);
        });
    }

    $scope.getBasketDragItems = function(basket){
        var str = "";
        var firstIsDone = false;
        if(basket.relationships.files && basket.relationships.files.data.length > 0){
            for(var i = 0; i < basket.relationships.files.data.length; i++){
                var file = collectedFiles[basket.relationships.files.data[i].id];
                if(file.attributes.properties && file.attributes.properties.details.file && str.indexOf(file.attributes.properties.details.file.path) < 0){
                    str = str.concat(',', file.attributes.properties.details.file.path);
                }
            }
        }
        return str.substr(1);
    }

	/** END OF DATABASKETS **/


	/** JOBS **/

    $scope.jobStatuses = [{name: "Succeeded", value: true}, {name: "Failed", value: true}, {name: "Running", value: true}];
    $scope.jobGroup = {};

    $scope.getColor = function(status){
        return CommonService.getColor(status);
    }

    $scope.jobs = [];
    $scope.jobServices = [];

    function loadJobs() {
        JobService.getJobs().then(function(result) {
                $scope.jobs= result.data;
                $scope.jobServices = result.included;
                groupJobs();
            });
    };
    loadJobs();

    $scope.$on('refresh.jobs', function(event, result) {
        $scope.jobs= result.data;
        $scope.jobServices = result.included;
        groupJobs();
    });

    // Group jobs by their service id
    $scope.groupedJobs = {};
    function groupJobs(){
        $scope.groupedJobs = {};
        if($scope.jobs){
            for(var i = 0; i < $scope.jobs.length; i++){
                var job = $scope.jobs[i];
                var category = $scope.groupedJobs[job.relationships.service.data[0].id];
                if(!category){
                    $scope.groupedJobs[job.relationships.service.data[0].id] = [];
                }
                if($scope.jobGroup[job.relationships.service.data[0].id] == undefined){
                    $scope.jobGroup[job.relationships.service.data[0].id] = {opened: false};
                }
                $scope.groupedJobs[job.relationships.service.data[0].id].push(job);
            }
        }
    }
    groupJobs();

    $scope.isJobGroupOpened = function(serviceId){
        return $scope.jobGroup[serviceId].opened;
    }

    $scope.toggleJobGroup = function(serviceId){
        $scope.jobGroup[serviceId].opened = !$scope.jobGroup[serviceId].opened;
    }

    $scope.filteredJobStatuses = [];
    $scope.filterJobs = function(){
        //TODO another query?
        $scope.filteredJobStatuses = [];
        for(var i = 0; i <$scope.jobStatuses.length; i++ ){
            if($scope.jobStatuses[i].value === true){
                $scope.filteredJobStatuses.push($scope.jobStatuses[i].name);
            }
        }
    }
    $scope.filterJobs();

    $scope.selectJob = function(job){
        $rootScope.$broadcast('select.job', job);
    }

    $scope.removeJob = function(event, job) {
        CommonService.confirm(event, 'Are you sure you want to delete job ' + job.id + "?").then(function(confirmed){
            if(confirmed == false){
                return;
            }

            JobService.removeJob(job).then(function() {
                console.log('remove ', job);
                $rootScope.$broadcast('delete.job', job);
                var i = $scope.jobs.indexOf(job);
                $scope.jobs.splice(i, 1);
                groupJobs();
                console.log($scope.groupedJobs[job.relationships.service.data[0].id]);
                if($scope.groupedJobs[job.relationships.service.data[0].id] == undefined){
                    console.log('last job in the group, remove group');
                    for(var serviceIndex = 0; serviceIndex < $scope.jobServices.length; serviceIndex++){
                        if($scope.jobServices[serviceIndex].id == job.relationships.service.data[0].id){
                            $scope.jobServices.splice(serviceIndex, 1);
                            break;
                        }
                    }
                }
            });
        });
    }

    $scope.hasGuiEndPoint = function(endPoint){
        if(endPoint && endPoint.includes("http")){
            return true;
        }
        return false;
    }

    /** END OF JOBS **/


	/** SERVICES **/
    $scope.services = [];
    ProductService.getServices().then(function(data){
        $scope.services= data;
    });

    $scope.serviceTypes = [{type: "processor", label: "Processors", value: true}, {type: 'application', label: "GUI Applications", value: true}];
    $scope.filteredServiceTypes = [];
    $scope.filterServices = function(){
        //TODO another query?
        $scope.filteredServiceTypes = [];
        for(var i = 0; i <$scope.serviceTypes.length; i++ ){
            if($scope.serviceTypes[i].value === true){
                $scope.filteredServiceTypes.push($scope.serviceTypes[i].type);
            }
        }
    }
    $scope.filterServices();

    $scope.serviceSearch = { searchText: ''};
    $scope.serviceQuickSearch = function(item){
        if(item.attributes.name.toLowerCase().indexOf($scope.serviceSearch.searchText.toLowerCase()) > -1
                || item.attributes.description.toLowerCase().indexOf($scope.serviceSearch.searchText.toLowerCase()) > -1){
            return true;
        }
        return false;
    }

    $scope.selectService = function(service){
        $rootScope.$broadcast('update.selectedService', service);
    }

    $scope.getShortDesc = function(desc){
        if(desc && desc.length > 41){
            return desc.substring(0, 41).concat('..');
        }
        return desc;
    }

    /** END OF SERVICES **/


    /* Pop-ups*/
    var popover = {};

    $scope.getServicePopover = function(service){

        var html = '<p class="raiting">' + $scope.getRating(service.attributes.rating)  +  '</p>' +
        '<div class="metadata"><div class="row">' +
            '<div class="col-sm-4">Name:</div>' +
              '<div class="col-sm-8">' + service.attributes.name + '</div>' +
            '</div>' +
        '<div class="row">' +
          '<div class="col-sm-4">Description:</div>' +
          '<div class="col-sm-8">' + service.attributes.description + '</div>' +
        '</div>' +
        '<div class="row">' +
          '<div class="col-sm-4">Type:</div>' +
          '<div class="col-sm-8">' + service.attributes.kind + '</div>' +
        '</div>'+
        '<div class="row">' +
           '<div class="col-sm-4">License:</div>' +
           '<div class="col-sm-8">' + service.attributes.license + '</div>' +
         '</div>'+
        '</div>';
        return popover[html] || (popover[html] = $sce.trustAsHtml(html));
    }

    $scope.getJobPopover = function(job) {
        var html = '<div class="row">' +
              '<div class="col-sm-4">Status:</div>' +
                '<div class="col-sm-8">' + job.attributes.status + '</div>' +
              '</div>' +
              '<div class="row">' +
                  '<div class="col-sm-12">Inputs:</div>' +
              '</div>' +
              '<div class="row">' +
                '<ul class="job-popover-list">' + getLiItems(job.attributes.inputs) + '</ul>' +
              '</div>' +
              '<div class="row">' +
                '<div class="col-sm-4">Outputs:</div>' +
              '</div>' +
              '<div class="row">' +
                '<ul class="job-popover-list">' + getLiItems(job.attributes.outputs) + '</ul>' +
              '</div>';
        return popover[html] || (popover[html] = $sce.trustAsHtml(html));
    }

    function getLiItems(object){
        var result =  '';
        if(object instanceof Object === true && Object.keys(object).length > 0){
            for(var key in object){
                result = result + '<li>' +
                '<div class="row">' +
                    '<div class="col-md-2">' + key +
                    '</div>' +
                    '<div class="col-md-10 wrap-text">' + object[key] +
                    '</div>' +
                '</div>' +
             '</li>';
            }
        }
        else{
            result = '<li>-</li>';
        }
        return result;
    }

    $scope.getRating = function(rating){
        var stars = '';
        for(var i = 0; i < 5; i++){
            if(rating > i){
                stars = stars + '<img src="images/star_filled.png" class="raiting-star"/>';
            }
            else{
                stars = stars + '<img src="images/star_lined.png" class="raiting-star"/>'
            }
        }
        return stars;
    }
    /* End of Pop-ups */

     }])
    .run(["$templateCache", function($templateCache) {
          /** This is to make accordion item open/close clicking on the header area and not only on the link itself **/
          $templateCache.put("customized-accordion-group.html",
            "<div class=\"panel\" ng-class=\"panelClass || 'panel-default'\">\n" +
            "  <div role=\"tab\" id=\"{{::headingId}}\" aria-selected=\"{{isOpen}}\" class=\"panel-heading pointer\" ng-keypress=\"toggleOpen($event)\" ng-click=\"toggleOpen()\" >\n" +
            "    <h4 class=\"panel-title\">\n" +
            "      <span uib-accordion-header ng-class=\"{'text-muted': isDisabled}\">{{heading}}</span>\n" +
            "    </h4>\n" +
            "  </div>\n" +
            "  <div id=\"{{::panelId}}\" aria-labelledby=\"{{::headingId}}\" aria-hidden=\"{{!isOpen}}\" role=\"tabpanel\" class=\"panel-collapse collapse\" uib-collapse=\"!isOpen\">\n" +
            "    <div class=\"panel-body\" ng-transclude></div>\n" +
            "  </div>\n" +
            "</div>\n" +
            "");
    }]);
});