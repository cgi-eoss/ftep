package com.cgi.eoss.ftep.core.requesthandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.IntStream;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.zoo.project.ZooConstants;

import com.cgi.eoss.ftep.core.data.manager.core.DataManager;
import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult;
import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult.DataDownloadStatus;
import com.cgi.eoss.ftep.core.requesthandler.beans.FileProtocols;
import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.requesthandler.beans.JobStatus;
import com.cgi.eoss.ftep.core.utils.DBRestApiManager;
import com.cgi.eoss.ftep.core.utils.FtepConstants;
import com.cgi.eoss.ftep.core.utils.beans.InsertResult;
import com.cgi.eoss.ftep.core.utils.rest.resources.ResourceJob;
import com.google.gson.Gson;

public class RequestHandler {
  private static final Logger LOG = Logger.getLogger(RequestHandler.class);

  private static boolean isLoggerConfigured = false;

  private DataManager dataManager;
  private ClusterManager clusterManager;
  private ZooConfigHandler zooConfigHandler;
  private File log4jPropFile;
  private HashMap<String, List<String>> inputItems = new HashMap<>();
  private HashMap<String, List<String>> inputFilesMap = new HashMap<>();
  private HashMap<String, List<String>> inputParams = new HashMap<>();
  private HashMap<String, String> downloadConfMap = new HashMap<>();

  private HashMap<String, HashMap<String, String>> zooConfMap = new HashMap<>();
  private HashMap<String, HashMap<String, Object>> wpsInputsMap = new HashMap<>();
  private HashMap<String, HashMap<String, String>> wpsOutputsMap = new HashMap<>();

  public HashMap<String, List<String>> getInputItems() {
    return inputItems;
  }

  public RequestHandler(HashMap<String, HashMap<String, String>> _conf,
      HashMap<String, HashMap<String, Object>> _inputs,
      HashMap<String, HashMap<String, String>> _outputs) {
    wpsOutputsMap = _outputs;
    zooConfMap = _conf;
    wpsInputsMap = _inputs;

    init();
  }

  private void init() {
    zooConfigHandler = new ZooConfigHandler(zooConfMap);
    if (!isLoggerConfigured) {
      configureLogger();
      isLoggerConfigured = true;
    }
    buildDownloadConfigMap();
    buildWpsInputsMap();
    dataManager = new DataManager();
    clusterManager = new ClusterManager();
  }

  private void buildDownloadConfigMap() {
    LOG.debug("Building download configuration parameters map");
    downloadConfMap.put(ZooConstants.ZOO_FTEP_DOWNLOAD_TOOL_PATH_PARAM,
        zooConfigHandler.getDownloadToolPath());
    downloadConfMap.put(ZooConstants.ZOO_FTEP_DATA_DOWNLOAD_DIR_PARAM,
        zooConfigHandler.getDataDownloadDir().getAbsolutePath());
    downloadConfMap.put(ZooConstants.ZOO_MAIN_CACHE_DIR_PARAM,
        zooConfigHandler.getCacheDir().getAbsolutePath());
  }

  public FtepJob createJob() {
    FtepJob ftepJob = new FtepJob();
    ftepJob.setJobID(zooConfigHandler.getJobID());
    ftepJob.setStatus(JobStatus.CREATED);
    createWorkingDir(ftepJob);
    createWpsPropertyFile(ftepJob);
    return ftepJob;
  }

  private void createWpsPropertyFile(FtepJob ftepJob) {
    try {
      File workingDir = ftepJob.getWorkingDir();
      File wpsPropertyfile = new File(workingDir, FtepConstants.WPS_PROP_FILE);
      Properties properties = new Properties();
      for (Entry<String, List<String>> entry : inputParams.entrySet()) {
        String value = entry.getValue().toString();
        // remove the square brackets '[' and ']' from the value before writing to the property file
        properties.setProperty(entry.getKey(), value.substring(1, value.length() - 1));
      }
      FileOutputStream fileOut = new FileOutputStream(wpsPropertyfile);
      properties.store(fileOut,
          "Properties created from WPS Execute Request for Job: " + ftepJob.getJobID());
    } catch (FileNotFoundException e) {
      LOG.error(e.getStackTrace());
    } catch (IOException e) {
      LOG.error(e.getStackTrace());
    }
  }

  private void createWorkingDir(FtepJob job) {
    LOG.debug("Creating directories (inDir, outDir) for job " + job.getJobID());
    try {
      String dirName = job.getJobID();
      File workingDir = zooConfigHandler.getDataDownloadDir();
      File jobWorkingDir =
          createDirectory(workingDir.getAbsolutePath(), FtepConstants.JOB_DIR_PREFIX + dirName);
      File inputDir = createDirectory(jobWorkingDir.getAbsolutePath(), FtepConstants.JOB_INPUT_DIR);
      File outputDir =
          createDirectory(jobWorkingDir.getAbsolutePath(), FtepConstants.JOB_OUTPUT_DIR);

      job.setWorkingDir(jobWorkingDir);
      job.setInputDir(inputDir);
      job.setOutputDir(outputDir);

    } catch (Exception ex) {
      LOG.error("Exception in job directory creation", ex);
    }
  }

  private File createDirectory(String path, String dirName) {

    File dirInCache = new File(path, dirName);
    if (!dirInCache.exists()) {
      if (dirInCache.mkdir()) {
        LOG.debug("Creating " + dirName + " directory at " + path);
      } else {
        LOG.error(dirName + " directory cannot be created at " + path);
      }
    }

    return dirInCache;
  }

  public DataManagerResult fetchInputData(FtepJob job) {

//    addEscapeChar(inputFilesMap);
    DataManagerResult dataManagerResult =
        dataManager.getData(downloadConfMap, job.getInputDir().getAbsolutePath(), inputFilesMap);
    DataDownloadStatus downloadStatus = dataManagerResult.getDownloadStatus();
    if (downloadStatus.equals(DataDownloadStatus.COMPLETE)) {
      LOG.info("Data fetch is successful");
    } else if (downloadStatus.equals(DataDownloadStatus.PARTIAL)) {
      LOG.warn("Not all input data can be fetched");
    } else if (downloadStatus.equals(DataDownloadStatus.NONE)) {
      LOG.error("Data fetch failed");
    }
    return dataManagerResult;
  }

  private void addEscapeChar(HashMap<String, List<String>> _inputFilesMap) {
    for (Entry<String, List<String>> e : _inputFilesMap.entrySet()) {
      List<String> urls = e.getValue();
      List<String> updatedUrls = new ArrayList<>();
      for (String url : urls) {
        if (url.contains("$")) {
          url = url.replace("$", "\\$");
        }
        updatedUrls.add(url);
      }
      _inputFilesMap.put(e.getKey(), updatedUrls);
    }
  }

  public void configureLogger() {

    log4jPropFile = zooConfigHandler.getLog4jPropFile();

    if (log4jPropFile.exists()) {
      PropertyConfigurator.configure(log4jPropFile.getAbsolutePath());
    } else {
      BasicConfigurator.configure();
    }
  }

  private void buildWpsInputsMap() {
    for (Entry<String, HashMap<String, Object>> entry : wpsInputsMap.entrySet()) {
      HashMap<String, Object> valueObj = entry.getValue();
      List<String> value = new ArrayList<String>();
      boolean isArray = valueObj.containsKey("isArray");
      if (isArray) {
        value = (ArrayList<String>) valueObj.get("value");
      } else {
        String valueString = (String) valueObj.get("value");
        value.add(valueString);
      }
      inputItems.put(entry.getKey(), value);
    }

    LOG.info("Inputs for WPS Execute Request " + getJobId());
    for (Entry<String, List<String>> e : inputItems.entrySet()) {
      String key = e.getKey();
      List<String> valueList = e.getValue();
      String firstValue = valueList.get(0);
      if (isValueRefersFile(firstValue)) {
        inputFilesMap.put(key, valueList);
      } else {
        inputParams.put(key, valueList);
      }
      LOG.debug("inputFiles :::: " + inputFilesMap);
      LOG.debug("inputParams :::: " + inputParams);
    }
  }

  private boolean isValueRefersFile(String firstValue) {
    for (FileProtocols val : FileProtocols.values()) {
      if (firstValue.trim().toUpperCase().startsWith(val.toString())) {
        return true;
      }
    }
    return false;
  }

  public String getWorkVmIpAddr() {
    return zooConfigHandler.getWorkerVM();
  }

  public int estimateExecutionCost() {
    // TODO Auto-generated method stub
    return 0;
  }

  public String getUserId() {

    return zooConfigHandler.getUserID();
  }

  public void setMessage(String message) {

    zooConfigHandler.setMessage(message);
  }

  public <T> T getInputParamValue(String paramName, Class<T> type) {
    List<String> values = inputItems.get(paramName);
    String value = null;
    if (null != values) {
      if (values.size() > 1) {
        value = values.toString();
      } else {
        value = values.get(0);
      }
      return type.cast(value);
    }

    return null;

  }

  public int findFreePortOn(String workerVmIpAddr) {

    int[] ports = IntStream
        .rangeClosed(FtepConstants.GUI_APP_MIN_PORT, FtepConstants.GUI_APP_MAX_PORT).toArray();
    for (int port : ports) {
      try {
        Socket s = new Socket(workerVmIpAddr, port);
        s.close();
      } catch (ConnectException e) {
        return port;
      } catch (IOException e) {
        if (e.getMessage().contains("refused"))
          return port;
      }
    }
    // if the program gets here, no port in the range was found
    LOG.error("Could not find a free TCP/IP port to start the application");
    return -1;
  }

  public String getJobId() {
    return zooConfigHandler.getJobID();
  }


  public InsertResult insertJob(String inputsAsJson, String outputsAsJson, String guiEndPoint) {
    ResourceJob resourceJob = new ResourceJob();
    resourceJob.setJobId(getJobId());
    resourceJob.setInputs(inputsAsJson);
    resourceJob.setOutputs(outputsAsJson);
    resourceJob.setGuiEndpoint(guiEndPoint);
    resourceJob.setUserId(getUserId());
    resourceJob.setServiceName(zooConfigHandler.getServiceName());
    LOG.debug("Job Resource created for :" + getJobId());
    return insertIntoJobTable(resourceJob);
  }

  private InsertResult insertIntoJobTable(ResourceJob resourceJob) {
    DBRestApiManager dataBaseMgr = DBRestApiManager.DB_API_CONNECTOR_INSTANCE;
    InsertResult insertResult = new InsertResult();
    insertResult = dataBaseMgr.insertJobRecord(resourceJob);
    if (insertResult.isStatus()) {
      LOG.debug(resourceJob.getJobId() + " Job is successfully inserted in the database");
      return insertResult;
    }
    LOG.error("Unable to insert Job record in the database");
    return insertResult;
  }

  public String toJson(Object processInputs) {
    Gson gson = new Gson();
    return gson.toJson(processInputs);
  }

  public void updateJobOutput(InsertResult resourceEndpoint, String outputsAsJson) {
    ResourceJob resourceJob = new ResourceJob();
    resourceJob.setOutputs(outputsAsJson);
    resourceJob.setId(resourceEndpoint.getResourceId());
    updateJobTable(resourceJob, resourceEndpoint.getResourceRestEndpoint());
  }

  private boolean updateJobTable(ResourceJob resourceJob, String resourceEndpoint) {
    DBRestApiManager dataBaseMgr = DBRestApiManager.DB_API_CONNECTOR_INSTANCE;
    if (dataBaseMgr.updateOutputsInJobRecord(resourceJob, resourceEndpoint)) {
      LOG.debug(getJobId() + " Job is successfully updated in the database");
      return true;
    }
    LOG.error("Unable to update Job record in the database for " + getJobId());
    return false;
  }
}


