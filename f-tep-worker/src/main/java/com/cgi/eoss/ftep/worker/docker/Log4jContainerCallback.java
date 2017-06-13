package com.cgi.eoss.ftep.worker.docker;

import com.cgi.eoss.ftep.logging.Logging;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import shadow.dockerjava.com.github.dockerjava.api.model.Frame;
import shadow.dockerjava.com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.List;
import java.util.Map;

/**
 * <p>Container logging callback utilising the Log4j 2 Thread Context.</p>
 * <p>The thread context is propagated from the point this object is instantiated.</p>
 */
public class Log4jContainerCallback extends LogContainerResultCallback {

    // Use a specific Logger name, since we don't want to flood the application log with container messages
    private static final Logger LOG = LogManager.getLogger("FTEP_DOCKER_LOGGER");
    private final Map<String, String> contextMap;
    private final List<String> contextMessages;

    public Log4jContainerCallback() {
        ThreadContext.push("In-Docker");
        this.contextMap = ThreadContext.getImmutableContext();
        this.contextMessages = ThreadContext.getImmutableStack().asList();
    }

    @Override
    public void onNext(Frame frame) {
        try (CloseableThreadContext.Instance ctc = getCurrentThreadContext()) {
            // TODO Investigate if there is useful info in the frame.getStreamType
            LOG.info(new String(frame.getPayload()).trim());
        }
    }

    private CloseableThreadContext.Instance getCurrentThreadContext() {
        // TODO Can the thread context be propagated better?
        CloseableThreadContext.Instance instance = CloseableThreadContext.push(Iterables.getLast(contextMessages));
        Lists.reverse(Lists.newArrayList(Iterables.limit(contextMessages, contextMessages.size() - 1)))
                .forEach(instance::push);
        contextMap.forEach(instance::put);
        instance.put(Logging.USER_LOG_MESSAGE_FIELD, "1");
        return instance;
    }

}
