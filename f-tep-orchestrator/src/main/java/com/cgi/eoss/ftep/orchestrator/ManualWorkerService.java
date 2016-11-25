package com.cgi.eoss.ftep.orchestrator;

/**
 * <p>Provides control and access of worker nodes, i.e. machines where F-TEP services may be executed.</p>
 */
public class ManualWorkerService implements WorkerService {

    private static final String DEFAULT_DOCKER_HOST = "localhost";

    @Override
    public Worker getWorker() {
        return Worker.builder().build();
    }

    @Override
    public Worker getWorker(String host) {
        return Worker.builder()
                .dockerHost(host)
                .build();
    }
}
