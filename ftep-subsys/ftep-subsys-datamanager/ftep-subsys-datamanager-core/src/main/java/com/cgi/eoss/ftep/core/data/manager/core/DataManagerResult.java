package com.cgi.eoss.ftep.core.data.manager.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Java bean with getters and setters -- PARAMETERS RENAMED, otherwise left as it was
public class DataManagerResult {
  public enum DataDownloadStatus {
    COMPLETE, PARTIAL, NONE
  }

  private Map<String, List<String>> updatedInputItems = new HashMap<>();
  private DataDownloadStatus downloadStatus;
  private String message;

  public Map<String, List<String>> getUpdatedInputItems() {
    return updatedInputItems;
  }

  public void setUpdatedInputItems(HashMap<String, List<String>> pUpdatedInputItems) {
    updatedInputItems = pUpdatedInputItems;
  }

  public DataDownloadStatus getDownloadStatus() {
    return downloadStatus;
  }

  public void setDownloadStatus(DataDownloadStatus pDownloadStatus) {
    downloadStatus = pDownloadStatus;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String pMessage) {
    message = pMessage;
  }
}
