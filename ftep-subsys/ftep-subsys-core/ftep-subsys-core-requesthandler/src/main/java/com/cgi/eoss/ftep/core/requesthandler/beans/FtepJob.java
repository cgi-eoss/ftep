package com.cgi.eoss.ftep.core.requesthandler.beans;

import java.io.File;

public class FtepJob {

	private String jobID;
	private File workingDir;
	private JobStatus status;

	public String getJobID() {
		return jobID;
	}

	public void setJobID(String jobID) {
		this.jobID = jobID;
	}

	public File getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

}
