package com.cgi.eoss.core.demo.processor;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

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
				System.err.println("ERROR: Minimum 4 arguments are required");
				return;
			}


			File inputFile1 = new File(args[0]);
			File inputFile2 = new File(args[1]);
			File outputConcatenatedFile = new File(args[2]);

			// Read the file as string
			String file1Str;
			Charset encoding = null;

			file1Str = FileUtils.readFileToString(inputFile1, encoding);
			String file2Str = FileUtils.readFileToString(inputFile2, encoding);

			// Write the file
			FileUtils.write(outputConcatenatedFile, file1Str, encoding);
			FileUtils.write(outputConcatenatedFile, "\n >>>>>>>> Appending next file \n", encoding, true);
			FileUtils.write(outputConcatenatedFile, file2Str, encoding, true); // true for append

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
