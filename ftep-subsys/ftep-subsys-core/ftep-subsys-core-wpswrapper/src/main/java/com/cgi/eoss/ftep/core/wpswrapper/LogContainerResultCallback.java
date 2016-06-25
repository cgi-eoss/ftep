package com.cgi.eoss.ftep.core.wpswrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;

public class LogContainerResultCallback extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogContainerResultCallback.class);

  @Override
  public void onNext(Frame item) {
    LOGGER.debug(item.toString());
    
  }






}
