package com.cgi.eoss.ftep.core.requesthandler;

public enum StringLogger {

	INSTANCE;
	private StringBuilder sb = new StringBuilder();

	public StringBuilder getSb() {
		return sb;
	}

}
