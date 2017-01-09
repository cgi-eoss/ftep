package com.cgi.eoss.ftep.core.requesthandler.beans;

public class TableFtepJob {

    private String jobID;

    private String inputs;

    private String outputs;

    private String guiEndpoint;

    private String userID;

    public String getJobID() {
        return jobID;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    public String getInputs() {
        return inputs;
    }

    public void setInputs(String inputs) {
        this.inputs = inputs;
    }

    public String getOutputs() {
        return outputs;
    }

    public void setOutputs(String outputs) {
        this.outputs = outputs;
    }

    public String getGuiEndpoint() {
        return guiEndpoint;
    }

    public void setGuiEndpoint(String guiEndpoint) {
        this.guiEndpoint = guiEndpoint;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }


}
