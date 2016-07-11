package com.cgi.eoss.ftep.core.data.manager.core;

import java.util.HashMap;
import java.util.ArrayList;

// Java bean with getters and setters
public class DataManagerResult {
    public enum DataDownloadStatus {
        COMPLETE, PARTIAL, NONE
    }

    private HashMap<String, ArrayList<String>> updatedInputItems = new HashMap<>();
    private DataDownloadStatus downloadStatus;
    private String message;

    public HashMap<String, ArrayList<String>> getUpdatedInputItems() {
        return updatedInputItems;
    }

    public void setUpdatedInputItems(HashMap<String, ArrayList<String>> pUpdatedInputItems) {
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
