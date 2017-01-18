package com.cgi.eoss.ftep.orchestrator.worker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>Service providing access to F-TEP Worker nodes based on environment requests.</p>
 */
@Service
public class WorkerFactory {

    private final ManualWorkerService manualWorkerService;

    @Autowired
    public WorkerFactory(ManualWorkerService manualWorkerService) {
        this.manualWorkerService = manualWorkerService;
    }

    /**
     * @return A Worker appropriate for the requested environment.
     */
    public Worker getWorker(WorkerEnvironment env) {
        switch (env) {
            case LOCAL:
                return manualWorkerService.getWorker();
            default:
                throw new UnsupportedOperationException("Unable to launch worker for environment: " + env);
        }
    }

    /**
     * @param host Hostname or IP address of the target worker.
     * @return A Worker with all clients (e.g. docker) configured to use the given host.
     */
    public Worker getWorker(String host) {
        return manualWorkerService.getWorker(host);
    }

}
