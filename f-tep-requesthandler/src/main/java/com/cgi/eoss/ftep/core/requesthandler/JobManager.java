package com.cgi.eoss.ftep.core.requesthandler;

import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.requesthandler.beans.JobStatus;

import java.util.UUID;

public class JobManager {

    public JobManager() {
        // TODO Auto-generated constructor stub
    }


    private String generateUniqueIdentifier() {
        String[] ids = UUID.randomUUID().toString().split("-");
        return ids[0];
    }


    public FtepJob createJob() {
        FtepJob ftepJob = new FtepJob();
        ftepJob.setJobID(generateUniqueIdentifier());
        ftepJob.setStatus(JobStatus.CREATED);
        return ftepJob;
    }

}
