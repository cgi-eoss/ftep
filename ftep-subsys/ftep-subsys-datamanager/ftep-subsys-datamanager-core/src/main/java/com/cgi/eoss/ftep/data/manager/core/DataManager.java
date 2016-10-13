package com.cgi.eoss.ftep.data.manager.core;

import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult;

import java.util.List;
import java.util.Map;

/**
 */
public interface DataManager {
    DataManagerResult getData(Map<String, String> downloadConfiguration,
                              String destination,
                              Map<String, List<String>> inputUrlListsWithJobID);
}
