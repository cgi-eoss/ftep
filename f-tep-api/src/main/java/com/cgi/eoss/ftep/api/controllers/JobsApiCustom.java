package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface JobsApiCustom {

    Page<Job> findByFilterOnly(String filter, Collection<Status> statuses, Pageable pageable);
    Page<Job> findByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);
    Page<Job> findByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    Page<Job> findByFilterAndIsNotSubjob(String filter, Collection<Status> statuses, Pageable pageable);
    Page<Job> findByFilterAndIsNotSubjobAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);
    Page<Job> findByFilterAndIsNotSubjobAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    Page<Job> findByFilterAndParent(String filter, Collection<Status> statuses, Long parentId, Pageable pageable);
    Page<Job> findByFilterAndParentAndOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable);
    Page<Job> findByFilterAndParentAndNotOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable);
}
