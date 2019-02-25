package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface JobsApiCustom extends BaseRepositoryApi<Job> {
    public Page<Job> searchByFilterOnly(String filter, Collection<Status> statuses, Pageable pageable);
    public Page<Job> searchByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);
    public Page<Job> searchByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    public Page<Job> searchByFilterAndIsNotSubjob(String filter, Collection<Status> statuses, Pageable pageable);
    public Page<Job> searchByFilterAndIsNotSubjobAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);
    public Page<Job> searchByFilterAndIsNotSubjobAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable);

    public Page<Job> searchByFilterAndParent(String filter, Collection<Status> statuses, Long parentId, Pageable pageable);
    public Page<Job> searchByFilterAndParentAndOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable);
    public Page<Job> searchByFilterAndParentAndNotOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable);
}
