package com.cgi.eoss.ftep.core.wpswrapper.utils;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LogContainerResultCallback extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {

    @Override
    public void onNext(Frame item) {
        LOG.debug(item.toString());
    }

}
