package com.cgi.eoss.ftep.orchestrator.service.gui;

import com.cgi.eoss.ftep.rpc.Job;

public interface GuiUrlService {

    String getBackendEndpoint(String workerId, Job job, String port);

    /**
     * @return A URL suitable for accessing a Job container's port running on the given worker.
     */
    String buildGuiUrl(String workerId, Job job, String port);

    default void update() {
        // do nothing
    }
}
