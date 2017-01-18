package com.cgi.eoss.ftep.orchestrator.worker;

/**
 * <p>Provides access to F-TEP worker nodes.</p>
 */
public interface WorkerService {
    /**
     * @return A default Worker, with services configured according to the WorkerService implementation.
     */
    Worker getWorker();

    /**
     * @param host Hostname or IP address of the target worker.
     * @return A Worker with all clients (e.g. docker) configured to use the given host.
     */
    Worker getWorker(String host);

}
