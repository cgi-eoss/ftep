package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepJob;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface JobDataService extends
        FtepEntityDataService<FtepJob> {

    List<FtepJob> findByOwner(FtepUser user);

    List<FtepJob> findByService(FtepService service);

    List<FtepJob> findByOwnerAndService(FtepUser user, FtepService service);

    /**
     * <p>Create a new FtepJob and save it in the database. This will <em>not</em> update an existing job with the same
     * ID, and will throw a constraint violation exception if attempted.</p>
     *
     * @param jobId The unique WPS-set identifier of the job to create.
     * @param userId The username of the user who triggered the job.
     * @param serviceId The service being launched by the job.
     * @return The created job instance.
     * @throws org.springframework.dao.DataIntegrityViolationException If the job ID already exists.
     */
    FtepJob buildNew(String jobId, String userId, String serviceId);

}
