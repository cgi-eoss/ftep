package com.cgi.eoss.ftep.orchestrator.service.gui;

import com.cgi.eoss.ftep.orchestrator.service.ServiceExecutionException;
import com.cgi.eoss.ftep.orchestrator.service.WorkerFactory;
import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.PortBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobPortLocatorService {

    private final WorkerFactory workerFactory;

    @Autowired
    public JobPortLocatorService(WorkerFactory workerFactory) {
        this.workerFactory = workerFactory;
    }

    public PortBinding getPortBinding(String workerId, Job job, String port) {
        return workerFactory.getWorkerById(workerId)
                .getPortBindings(job).getBindingsList().stream()
                .filter(b -> b.getPortDef().equals(port))
                .findFirst()
                .orElseThrow(() -> new ServiceExecutionException(String.format("Could not find port %s on docker container for job %s", port, job.getId())));
    }

}
