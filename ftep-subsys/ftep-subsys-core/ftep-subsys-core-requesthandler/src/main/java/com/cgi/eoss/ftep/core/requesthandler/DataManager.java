package com.cgi.eoss.ftep.core.requesthandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;

public class DataManager {
	private static final Logger LOG = Logger.getLogger(DataManager.class);
	private List<String> inputFiles = new ArrayList<String>();

	public List<String> getInputFileIdentifiers() {
		return inputFiles;
	}

	public boolean getData(FtepJob job, HashMap<String, List<String>> inputItems) {
		try {

			for (Entry<String, List<String>> entry : inputItems.entrySet()) {
				List<String> updatedInputPaths = new ArrayList<String>();
				String inputIdentifier = entry.getKey();
				List<String> inputValues = entry.getValue();
				for (String val : inputValues) {
					if (val.startsWith("http://")) {
						URL url = new URL(val);
						InputStream is = url.openStream();
						String path = url.getPath();
						String[] parts = path.split("/");
						String filename = parts[parts.length - 1];
						File target = job.getWorkingDir();
						String inputPath = new File(target, filename).getAbsolutePath();
						Files.copy(is, Paths.get(inputPath), StandardCopyOption.REPLACE_EXISTING);
						updatedInputPaths.add(inputPath);
						inputFiles.add(filename);
					} else if (val.startsWith("copy://")) {
						String inputPath = val.substring(7);

						Path source = Paths.get(inputPath);

						String[] parts = inputPath.split("/");
						String foldername = parts[parts.length - 1];
						File target = job.getInputDir();
						String destination = new File(target, foldername).getAbsolutePath();
						Files.move(source, Paths.get(destination));
//						Files.createSymbolicLink(Paths.get(destination),source);
						updatedInputPaths.add(destination);
						inputFiles.add(foldername);

					} else
						break;
				}
				if (updatedInputPaths.size() > 0) {
					inputItems.put(inputIdentifier, updatedInputPaths);
				}
			}

			LOG.info("Updated input items after fetching data");
			for (Entry<String, List<String>> e : inputItems.entrySet()) {
				LOG.info(e.getKey() + " :::: " + e.getValue());
			}
			LOG.info("Input File Paths are: " + inputFiles);
			return true;
		} catch (MalformedURLException e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
		}

		return false;
	}

}
