<?php
class Job extends stdClass
{
    /**
     * @param string $authorId
     * @param string $firstName
     * @param string $lastName
     *
     * @return Author
     */
    public static function instance($inputdb, $outputdb, $status)
    {
        $job  = new self();
        $job->jid="1";
        $job->inputdb  = $inputdb;
        $job->outputdb = $outputdb;
        $job->status  = $status;
        return $job;
    }
}
