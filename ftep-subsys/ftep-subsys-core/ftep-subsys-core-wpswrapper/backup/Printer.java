package com.cgi.eoss.ftep.core.wpswrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Printer {

	private static final Logger LOG = Logger.getLogger(Printer.class);

	private HashMap<String, HashMap<String, String>> wpsConf;
	private HashMap<String, HashMap<String, Object>> wpsInputs;
	private HashMap<String, HashMap<String, String>> wpsOutputs;
	private HashMap<String, List<String>> inputItems = new HashMap<String, List<String>>();
	private List<String> outputItems = new ArrayList<String>();

	public Printer(HashMap<String, HashMap<String, String>> conf, HashMap<String, HashMap<String, Object>> inputs,
			HashMap<String, HashMap<String, String>> outputs) {

		wpsConf = conf;
		wpsInputs = inputs;
		wpsOutputs = outputs;
	}



	public HashMap<String, List<String>> getInputs() {
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

}
