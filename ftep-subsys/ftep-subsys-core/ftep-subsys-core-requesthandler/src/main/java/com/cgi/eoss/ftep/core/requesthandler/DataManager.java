package com.cgi.eoss.ftep.core.requesthandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;

public class DataManager {

	public boolean getData(FtepJob job, HashMap<String, List<String>> inputItems) {

		try {
			
			List<String> i1Map = inputItems.get("i1");
			List<String> i2Map = inputItems.get("i2");
			
			// FIXME : get all the urls 
			URL file1 = new URL(i1Map.get(0));
			URL file2 = new URL(i2Map.get(0));

			InputStream in1 = file1.openStream();
			InputStream in2 = file2.openStream();
			File target = job.getWorkingDir();
			String i1Path = new File(target, "f1.txt").getAbsolutePath();
			String i2Path = new File(target, "f2.txt").getAbsolutePath();

			Files.copy(in1, Paths.get(i1Path), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(in2, Paths.get(i2Path), StandardCopyOption.REPLACE_EXISTING);
			inputItems.put("i1", string2List(i1Path));
			inputItems.put("i2", string2List(i2Path));

			return true;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	private List<String> string2List(String item) {
		List<String> stringList = new ArrayList<String>();
		stringList.add(item);
		return stringList;
	}

}
