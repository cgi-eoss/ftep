package com.cgi.eoss.ftep.core.wpswrapper;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;

public abstract class AbstractWrapperProc {

	private static final Logger LOG = Logger.getLogger(AbstractWrapperProc.class);
	private String dockerImageName;

	public AbstractWrapperProc(String dockerImgName) {
		if (null != dockerImgName && !dockerImgName.isEmpty()) {
			dockerImageName = dockerImgName;
		} else {
			LOG.error("Missing Docker Image name");
		}
	}

	RequestHandler getRequestHandler(HashMap<String, HashMap<String, String>> conf,
			HashMap<String, HashMap<String, Object>> inputs, HashMap<String, HashMap<String, String>> outputs) {
		RequestHandler requestHandler = new RequestHandler(conf, inputs, outputs);
		return requestHandler;
	}

	boolean isSufficientCoinsAvailable() {
		// TODO Auto-generated method stub
		return true;
	}

	boolean isValidUser() {
		// TODO Auto-generated method stub
		return true;
	}

	boolean getInputParameter(String string) {
		// TODO Auto-generated method stub
		return false;
	}

	public String getDockerImageName() {
		return dockerImageName;
	}

}
