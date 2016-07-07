package com.cgi.eoss.ftep.core.requesthandler.beans;

public class InsertResult {
  private boolean status = false;
  private String restResourceId;

  public boolean isStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public String getRestResourceId() {
    return restResourceId;
  }

  public void setRestResourceId(String restResourceId) {
    this.restResourceId = restResourceId;
  }


}
