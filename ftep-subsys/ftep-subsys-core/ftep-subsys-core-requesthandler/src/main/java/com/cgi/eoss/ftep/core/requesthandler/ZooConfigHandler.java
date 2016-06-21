package com.cgi.eoss.ftep.core.requesthandler;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.zoo.project.ZooConstants;

public class ZooConfigHandler {

	private HashMap<String, String> ftepZooConf;
	private HashMap<String, String> lEnvZooConf;
	private HashMap<String, String> sEnvZooConf;
	private HashMap<String, String> rEnvZooConf;

	private File workingDirParent;
	private File log4jPropFile;
	// TODO remove the workerVM property after the implementation of Resource
	// Provisioner
	private String workerVM;
	private static final Logger LOG = Logger.getLogger(ZooConfigHandler.class);

	public ZooConfigHandler(HashMap<String, HashMap<String, String>> _zooConf) {
		ftepZooConf = _zooConf.get(ZooConstants.ZOO_FTEP_CFG_MAP);
		lEnvZooConf = _zooConf.get(ZooConstants.ZOO_LENV_CFG_MAP);
		sEnvZooConf = _zooConf.get(ZooConstants.ZOO_SENV_CFG_MAP);
		rEnvZooConf = _zooConf.get(ZooConstants.ZOO_RENV_CFG_MAP);
	}

	public File getWorkingDirParent() {
		String workingDirLocation = ftepZooConf.get(ZooConstants.ZOO_FTEP_WORKDIR_LOCATION_PARAM);
		if (null != workingDirLocation) {
			workingDirParent = new File(workingDirLocation);
		} else {
			LOG.error(
					"Missing property " + ZooConstants.ZOO_FTEP_WORKDIR_LOCATION_PARAM + " in the F-TEP configuration");
		}
		return workingDirParent;
	}

	public File getLog4jPropFile() {

		String log4jPropertyFile = ftepZooConf.get(ZooConstants.ZOO_FTEP_LOG4J_FILENAME_PARAM);
		if (null != log4jPropertyFile) {
			log4jPropFile = new File(log4jPropertyFile);
		} else {
			LOG.error("Missing property " + ZooConstants.ZOO_FTEP_LOG4J_FILENAME_PARAM + " in the F-TEP configuration");
		}

		return log4jPropFile;
	}

	public String getWorkerVM() {
		String workerVmIpAddr = ftepZooConf.get(ZooConstants.ZOO_FTEP_WORKER_VM_IP_ADDR_PARAM);
		if (null != workerVmIpAddr) {
			return workerVmIpAddr;
		} else {
			LOG.error("Missing property " + ZooConstants.ZOO_FTEP_WORKER_VM_IP_ADDR_PARAM
					+ " in the F-TEP configuration");
		}
		return "localhost:2376";
	}

	public String getJobID() {
		String zooJobID = lEnvZooConf.get(ZooConstants.ZOO_LENV_USID_PARAM);
		if (null != zooJobID) {
			return zooJobID;
		} else {
			LOG.error("Cannot find WPS server JobID. Creating a new one.");
		}
		return generateUniqueIdentifier();
	}

	public String getUserID() {
		String ssoUserID = rEnvZooConf.get(ZooConstants.ZOO_RENV_SSO_USERID_PARAM);
		if (null != ssoUserID) {
			return ssoUserID;
		} else {
			LOG.error("Cannot find EO SSO User ID");
		}
		return "";
	}

	private String generateUniqueIdentifier() {
		String[] ids = UUID.randomUUID().toString().split("-");
		return ids[0];
	}

}
