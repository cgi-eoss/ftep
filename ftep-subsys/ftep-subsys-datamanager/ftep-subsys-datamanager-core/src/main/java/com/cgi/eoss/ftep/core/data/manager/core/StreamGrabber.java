package com.cgi.eoss.ftep.core.data.manager.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class StreamGrabber extends Thread {
  private static final Logger LOG = Logger.getLogger(StreamGrabber.class);

  InputStream is;
  private List<String> lines = new ArrayList<>();

  StreamGrabber(InputStream is) {
    this.is = is;
  }

  public void run() {
    try {
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      while ((line = br.readLine()) != null)
        lines.add(line);
    } catch (IOException ioe) {
      LOG.error(ioe);
    }
  }

  public List<String> getLines() {
    return lines;
  }

}
