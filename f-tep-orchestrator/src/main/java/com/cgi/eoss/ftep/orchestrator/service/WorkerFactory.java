package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

/**
 * <p>Service providing access to F-TEP Worker nodes based on environment requests.</p>
 */
@Service
@Log4j2
public class WorkerFactory {

    private final DiscoveryClient discoveryClient;
    private final String workerServiceId;

    @Autowired
    public WorkerFactory(DiscoveryClient discoveryClient,
                         @Value("${ftep.orchestrator.worker.eurekaServiceId:f-tep worker}") String workerServiceId) {
        this.discoveryClient = discoveryClient;
        this.workerServiceId = workerServiceId;
    }

    /**
     * @return A Worker appropriate for the requested environment.
     */
    public FtepWorkerGrpc.FtepWorkerBlockingStub getWorker(WorkerEnvironment env) {
        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .filter(si -> si.getMetadata().get("workerEnv").equals(env.toString()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find registered worker for environment: " + env));

        LOG.info("Located {} worker: {}:{}", env, worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return FtepWorkerGrpc.newBlockingStub(managedChannel);
    }

}
