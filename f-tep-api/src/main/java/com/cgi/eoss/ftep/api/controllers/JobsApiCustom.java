package com.cgi.eoss.ftep.api.controllers;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.User;

public interface JobsApiCustom {

    Page<Job> findByIdContainsAndStatus(@Param("filter") String filter, @Param("status") Collection<Status> statuses,
            Pageable pageable);

    Page<Job> findByIdContainsAndStatusAndOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses,
            @Param("owner") User user, Pageable pageable);

    Page<Job> findByIdContainsAndStatusAndNotOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses,
            @Param("owner") User user, Pageable pageable);
}
