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

public class RequestHandler {
	private static final Logger LOG = Logger.getLogger(RequestHandler.class);

	private static boolean isLoggerConfigured = false;

	private JobManager jobManager;
	private DataManager dataManager;
	private ClusterManager clusterManager;
	private ZooConfigHandler zooConfigHandler;
	private File log4jPropFile;
	// private HashMap<String, HashMap<String, Object>> wpsInputs;
	private HashMap<String, HashMap<String, String>> wpsOutputs;
	private HashMap<String, List<String>> inputItems = new HashMap<String, List<String>>();

	public HashMap<String, List<String>> getInputItems() {
		return inputItems;
	}

	public RequestHandler(HashMap<String, HashMap<String, String>> conf,
			HashMap<String, HashMap<String, Object>> inputs, HashMap<String, HashMap<String, String>> outputs) {
		wpsOutputs = outputs;
		zooConfigHandler = new ZooConfigHandler(conf);

		init(inputs, conf);
	}

	private void init(HashMap<String, HashMap<String, Object>> inputs, HashMap<String, HashMap<String, String>> conf) {
		if (!isLoggerConfigured) {
			configureLogger();
			isLoggerConfigured = true;
		}
		inputItems = getInputs(inputs);
		jobManager = new JobManager();
		dataManager = new DataManager();
		clusterManager = new ClusterManager();
	}

	public FtepJob createJob() {
		FtepJob job = jobManager.createJob();
		job.setWorkingDir(createWorkingDir(job));
		return job;
	}

	private File createWorkingDir(FtepJob job) {

		try {
			String dirName = job.getJobID();
			File workingDir = zooConfigHandler.getWorkingDirParent();
			File jobWorkingDir = createDirectory(workingDir.getAbsolutePath(), "Job_" + dirName);
			return jobWorkingDir;

		} catch (Exception ex) {
			LOG.error("Exception in job directory creation", ex);
		}
		return null;

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

	public static void main(String[] args) {
		System.out.println("Testing Request Handler");

		String log4jPropertyfileStr = "log4j.properties";
		if (null != log4jPropertyfileStr && !log4jPropertyfileStr.isEmpty()) {
			File log4jPropertyFile = new File(log4jPropertyfileStr);
			PropertyConfigurator.configure(log4jPropertyFile.getAbsolutePath());
		} else {
			BasicConfigurator.configure();
		}

		HashMap<String, String> config = new HashMap<String, String>();
		config.put("workingDirLocation", "E:\\07_StagingArea");

		HashMap<String, HashMap<String, String>> configuration = new HashMap<String, HashMap<String, String>>();
		configuration.put("ftep", config);
		RequestHandler handler = new RequestHandler(configuration, null, null);
		handler.createJob();

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

	private HashMap<String, List<String>> getInputs(HashMap<String, HashMap<String, Object>> wpsInputs) {
		for (Entry<String, HashMap<String, Object>> entry : wpsInputs.entrySet()) {
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

		LOG.info("Input Items");
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
}
