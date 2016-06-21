package com.cgi.eoss.ftep.core.requesthandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.requesthandler.beans.JobStatus;
import com.cgi.eoss.ftep.core.requesthandler.utils.FtepConstants;

public class RequestHandler {
	private static final Logger LOG = Logger.getLogger(RequestHandler.class);

	private static boolean isLoggerConfigured = false;

	private DataManager dataManager;
	private ClusterManager clusterManager;
	private ZooConfigHandler zooConfigHandler;
	private File log4jPropFile;
	private HashMap<String, List<String>> inputItems = new HashMap<String, List<String>>();
	private HashMap<String, HashMap<String, String>> zooConfMap;
	private HashMap<String, HashMap<String, Object>> wpsInputsMap;
	private HashMap<String, HashMap<String, String>> wpsOutputsMap;

	public HashMap<String, List<String>> getInputItems() {
		return inputItems;
	}

	public RequestHandler(HashMap<String, HashMap<String, String>> _conf,
			HashMap<String, HashMap<String, Object>> _inputs, HashMap<String, HashMap<String, String>> _outputs) {
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
		inputItems = buildInputsValueMap();
		dataManager = new DataManager();
		clusterManager = new ClusterManager();
	}

	public FtepJob createJob() {
		FtepJob ftepJob = new FtepJob();
		ftepJob.setJobID(zooConfigHandler.getJobID());
		ftepJob.setStatus(JobStatus.CREATED);
		createWorkingDir(ftepJob);
		return ftepJob;
	}

	private void createWorkingDir(FtepJob job) {

		try {
			String dirName = job.getJobID();
			File workingDir = zooConfigHandler.getWorkingDirParent();
			File jobWorkingDir = createDirectory(workingDir.getAbsolutePath(), FtepConstants.JOB_DIR_PREFIX + dirName);
			File inputDir = createDirectory(jobWorkingDir.getAbsolutePath(), FtepConstants.JOB_INPUT_DIR);
			File outputDir = createDirectory(jobWorkingDir.getAbsolutePath(), FtepConstants.JOB_OUTPUT_DIR);

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
				LOG.info("Creating " + dirName + " directory at " + path);
			} else {
				LOG.error(dirName + " directory cannot be created at " + path);
			}
		}

		return dirInCache;
	}

	public List<String> fetchInputData(FtepJob job) {
		List<String> inputFiles = new ArrayList<String>();

		if (dataManager.getData(job, inputItems)) {
			LOG.info("Data fetched successfully from web");
			return dataManager.getInputFileIdentifiers();
		} else {
			LOG.info("Data transfer failed");
		}

		return inputFiles;
	}

	public void configureLogger() {

		log4jPropFile = zooConfigHandler.getLog4jPropFile();

		if (log4jPropFile.exists()) {
			PropertyConfigurator.configure(log4jPropFile.getAbsolutePath());
		} else {
			BasicConfigurator.configure();
		}
	}

	private HashMap<String, List<String>> buildInputsValueMap() {
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

		LOG.info("WPS Execute Request Input Items");
		for (Entry<String, List<String>> e : inputItems.entrySet()) {
			LOG.info(e.getKey() + " :::: " + e.getValue());

		}
		return inputItems;

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

}
