package com.cgi.eoss.ftep.core.utils.beans;

public class InsertResult {
  private boolean status = false;
  private String resourceRestEndpoint;
  private String resourceId;


  public boolean isStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getResourceRestEndpoint() {
    return resourceRestEndpoint;
  }

  public void setResourceRestEndpoint(String resourceRestEndpoint) {
    this.resourceRestEndpoint = resourceRestEndpoint;
  }


}
