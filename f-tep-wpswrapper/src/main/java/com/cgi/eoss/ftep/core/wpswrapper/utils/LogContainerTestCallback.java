package com.cgi.eoss.ftep.core.wpswrapper.utils;

import com.github.dockerjava.api.model.Frame;

import java.util.ArrayList;
import java.util.List;

public class LogContainerTestCallback extends LogContainerResultCallback{
  protected final StringBuffer log = new StringBuffer();

  List<Frame> collectedFrames = new ArrayList<Frame>();

  boolean collectFrames = false;

  public LogContainerTestCallback() {
    this(false);
  }

  public LogContainerTestCallback(boolean collectFrames) {
    this.collectFrames = collectFrames;
  }

  @Override
  public void onNext(Frame frame) {
    if (collectFrames)
      collectedFrames.add(frame);
    log.append(new String(frame.getPayload()));
  }

  @Override
  public String toString() {
    return log.toString();
  }


  public List<Frame> getCollectedFrames() {
    return collectedFrames;
  }
}
