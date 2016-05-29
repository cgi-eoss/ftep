package com.cgi.eoss.ftep.core.requesthandler;

import java.io.File;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class ZooConfigHandler {

	private HashMap<String, String> ftepZooConf;
	private File workingDirParent;
	private File log4jPropFile;
	private static final Logger LOG = Logger.getLogger(ZooConfigHandler.class);

	public ZooConfigHandler(HashMap<String, HashMap<String, String>> zooConf) {
		ftepZooConf = zooConf.get("ftep");
	}

	public File getWorkingDirParent() {
		String workingDirLocation = ftepZooConf.get(FtepConf.WORKDIR_LOCATION);
		if (null != workingDirLocation) {
			workingDirParent = new File(workingDirLocation);
		} else {
			LOG.error("Missing property " + FtepConf.WORKDIR_LOCATION + " in the F-TEP configuration");
		}
		return workingDirParent;
	}

	public File getLog4jPropFile() {

		String log4jPropertyFile = ftepZooConf.get(FtepConf.LOG4J_PROPERTY_FILE);
		if (null != log4jPropertyFile) {
			log4jPropFile = new File(log4jPropertyFile);
		} else {
			LOG.error("Missing property " + FtepConf.LOG4J_PROPERTY_FILE + " in the F-TEP configuration");
		}

		return log4jPropFile;
	}

}
