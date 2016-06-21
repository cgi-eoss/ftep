package com.cgi.eoss.ftep.core.requesthandler.beans;

import java.io.File;

public class FtepJob {

	private String jobID;
	private File workingDir;
	private File inputDir;
	private File outputDir;
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

	public File getInputDir() {
		return inputDir;
	}

	public void setInputDir(File inputDir) {
		this.inputDir = inputDir;
	}

	public File getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

}
