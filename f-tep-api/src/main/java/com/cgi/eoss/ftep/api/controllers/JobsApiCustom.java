package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface JobsApiCustom {

    public Page<Job> findByFilterOnly(String filter, Collection<Status> statuses, Pageable pageable);
    public Page<Job> findByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);
    public Page<Job> findByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    public Page<Job> findByFilterAndIsNotSubjob(String filter, Collection<Status> statuses, Pageable pageable);
    public Page<Job> findByFilterAndIsNotSubjobAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);
    public Page<Job> findByFilterAndIsNotSubjobAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    public Page<Job> findByFilterAndParent(String filter, Collection<Status> statuses, Long parentId, Pageable pageable);
    public Page<Job> findByFilterAndParentAndOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable);
    public Page<Job> findByFilterAndParentAndNotOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable);
}
