package com.cgi.eoss.core.demo.processor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

public class FileJoiner {

	public static void main(String[] args) {

		FileJoiner fileJoiner = new FileJoiner();
		fileJoiner.start(args);

	}

	private void start(String[] args) {
		try {

			// args: filepath for input1, filepath for input2, filepath for
			// output, delay in secs

			if (args.length < 4) {
				System.err.println("ERROR: Minimum 2 arguments are required");
				return;
			}

			// File file1 = new File(
			// "E:\\99_Installations\\01_Gdrive\\03_Work\\03_GitAtCgi\\f-tep\\ftep-subsys\\ftep-subsys-core\\ftep-subsys-core-demoprocessor\\src\\test\\resources",
			// "file1.txt");

			// File file2 = new File(
			// "E:\\99_Installations\\01_Gdrive\\03_Work\\03_GitAtCgi\\f-tep\\ftep-subsys\\ftep-subsys-core\\ftep-subsys-core-demoprocessor\\src\\test\\resources",
			// "file2.txt");

			File file1 = new File(args[0]);
			File file2 = new File(args[1]);

			// File to write
			// File file3 = new File(
			// "E:\\99_Installations\\01_Gdrive\\03_Work\\03_GitAtCgi\\f-tep\\ftep-subsys\\ftep-subsys-core\\ftep-subsys-core-demoprocessor\\src\\test\\resources",
			// "file3.txt");
			File file3 = new File(args[2]);

			// Read the file as string
			String file1Str;
			Charset encoding = null;

			file1Str = FileUtils.readFileToString(file1, encoding);
			String file2Str = FileUtils.readFileToString(file2, encoding);

			// Write the file
			FileUtils.write(file3, file1Str, encoding);
			FileUtils.write(file3, "\n >>>>>>>> Appending next file \n", encoding, true);
			FileUtils.write(file3, file2Str, encoding, true); // true for append

			// introduce delay to simulate processing time
			int delayInSecs = Integer.parseInt(args[3]);
			Thread.sleep(TimeUnit.SECONDS.toMillis(delayInSecs));

		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}

	}

}
