package com.cgi.eoss.ftep.orchestrator;

/**
 * <p>Provides control and access of worker nodes, i.e. machines where F-TEP services may be executed.</p>
 * <p>Injects required local and remote services (e.g. DownloadManager) for workers.</p>
 */
public class ManualWorkerService implements WorkerService {

    private final ServiceInputOutputManager inputOutputManager;

    public ManualWorkerService() {
        // TODO Remove this placeholder constructor for legacy WPS services
        this(null);
    }

    public ManualWorkerService(ServiceInputOutputManager inputOutputManager) {
        this.inputOutputManager = inputOutputManager;
    }

    @Override
    public Worker getWorker() {
        return getBuilder().build();
    }

    @Override
    public Worker getWorker(String host) {
        return getBuilder()
                .dockerHost(host)
                .build();
    }

    private Worker.WorkerBuilder getBuilder() {
        return Worker.builder().inputOutputManager(inputOutputManager);
    }

}
