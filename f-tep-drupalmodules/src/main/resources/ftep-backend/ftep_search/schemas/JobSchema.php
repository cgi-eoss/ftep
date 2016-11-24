<?php
use \Neomerx\JsonApi\Schema\SchemaProvider;

class JobSchema extends SchemaProvider{
    protected $resourceType = 'jobs';
    // OPTIONAL (by default constructed as '/' . $resourceType . '/')
    //     protected $selfSubUrl   = '/sites';
    public function getId($job) {
        /** @var Site $site */
        return $job->jid;
    }
    public function getAttributes($job) {
        /** @var Site $site */
        return [
            'inputdb' => $job->inputdb,
            'outputdb' => $job->outputdb,
            'status' => $job->status,
        ];
    }

    public function getRelationships($job, $isPrimary, array $includeList) {
        /** @var Site $site */
        return [
            //'user' => [self::DATA => $job->user],
        ];
    }

}
