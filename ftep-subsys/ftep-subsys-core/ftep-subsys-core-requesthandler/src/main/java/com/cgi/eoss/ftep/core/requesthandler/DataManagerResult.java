package com.cgi.eoss.ftep.core.requesthandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataManagerResult {

  // Java bean with getters and setters

  private HashMap<String, List<String>> updatedInputItems = new HashMap<>();
  
  private List<String> inputFiles = new ArrayList<>();

  private DataDownloadStatus downloadStatus;

  private String message;

  public HashMap<String, List<String>> getUpdatedInputItems() {
    return updatedInputItems;
  }

  public void setUpdatedInputItems(HashMap<String, List<String>> updatedInputItems) {
    this.updatedInputItems = updatedInputItems;
  }

  public DataDownloadStatus getDownloadStatus() {
    return downloadStatus;
  }

  public void setDownloadStatus(DataDownloadStatus downloadStatus) {
    this.downloadStatus = downloadStatus;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<String> getInputFiles() {
    return inputFiles;
  }

  public void setInputFiles(List<String> inputFiles) {
    this.inputFiles = inputFiles;
  }

}


