package com.cgi.eoss.ftep.core.wpswrapper.utils;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogContainerResultCallback extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {

    @Override
    public void onNext(Frame item) {
        LOG.debug(item.toString());
    }

}
