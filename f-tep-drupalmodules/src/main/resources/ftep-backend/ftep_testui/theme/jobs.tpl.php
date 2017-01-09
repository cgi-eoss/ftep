<div id="jobs-app" ng-controller="jobscontroller" >
All FTEP123 jobs {{ 1+1 }} <br/>
{{ foobar }}	

<br/>
<pre>
{{ jobs | json : spacing }}
</pre>

<div class="job" ng-repeat="job in jobs">
<h2>{{job.label}}</h2>
</div>
</div>
