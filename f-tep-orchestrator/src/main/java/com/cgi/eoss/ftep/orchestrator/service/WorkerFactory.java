package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.WorkerLocatorExpression;
import com.cgi.eoss.ftep.persistence.service.WorkerLocatorExpressionDataService;
import com.cgi.eoss.ftep.rpc.Worker;
import com.cgi.eoss.ftep.rpc.WorkersList;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc.FtepWorkerBlockingStub;
import com.google.common.collect.ImmutableMap;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.expression.ExpressionParser;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Service providing access to F-TEP Worker nodes based on environment requests.</p>
 */
@Log4j2
public class WorkerFactory {

    private final DiscoveryClient discoveryClient;
    private final String workerServiceId;
    private final ExpressionParser expressionParser;
    private final WorkerLocatorExpressionDataService workerLocatorExpressionDataService;
    private final WorkerLocatorExpression defaultWorkerLocatorExpression;

    private final Map<String, FtepWorkerBlockingStub> workerStubCache = new ConcurrentHashMap<>();

    public WorkerFactory(DiscoveryClient discoveryClient, String workerServiceId, ExpressionParser expressionParser, WorkerLocatorExpressionDataService workerLocatorExpressionDataService, String defaultWorkerExpression) {
        this.discoveryClient = discoveryClient;
        this.workerServiceId = workerServiceId;
        this.expressionParser = expressionParser;
        this.workerLocatorExpressionDataService = workerLocatorExpressionDataService;
        this.defaultWorkerLocatorExpression = WorkerLocatorExpression.builder().expression(defaultWorkerExpression).build();
    }

    /**
     * @return A Worker appropriate for the requested environment.
     */
    public FtepWorkerGrpc.FtepWorkerBlockingStub getWorker(JobConfig jobConfig) {
        String workerId = getWorkerId(jobConfig);

        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .filter(si -> si.getMetadata().get("workerId").equals(workerId))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find registered worker for environment: " + workerId));

        LOG.info("Located {} worker: {}:{}", workerId, worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return FtepWorkerGrpc.newBlockingStub(managedChannel);
    }

    /**
     * @return The worker with the specified id
     */
    private FtepWorkerGrpc.FtepWorkerBlockingStub createStubForWorker(String workerId) {
        LOG.debug("Locating worker with id {}", workerId);
        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .filter(si -> si.getMetadata().get("workerId").equals(workerId))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find worker with id: " + workerId));

        LOG.info("Located {} worker: {}:{}", workerId, worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return FtepWorkerGrpc.newBlockingStub(managedChannel);
    }

    public FtepWorkerBlockingStub getWorkerById(String workerId) {
        FtepWorkerBlockingStub existingWorkerStub = workerStubCache.get(workerId);
        if (existingWorkerStub != null) {
            return existingWorkerStub;
        } else {
            FtepWorkerBlockingStub newWorkerStub = createStubForWorker(workerId);
            workerStubCache.put(workerId, newWorkerStub);
            return newWorkerStub;
        }
    }

    /**
     * @return An existing instance of a worker
     */
    public FtepWorkerGrpc.FtepWorkerBlockingStub getOne() {
        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find a worker"));

        LOG.info("Located worker: {}:{}", worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return FtepWorkerGrpc.newBlockingStub(managedChannel);
    }

    public Map<String, Object> getMessageHeaders(Job job) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        builder.put("jobId", job.getId());

        // Tag the message for picking up by a specific worker - this may be ignored if a worker is not "restricted"
        builder.put("workerId", getWorkerId(job));

        return builder.build();
    }

    public WorkersList listWorkers() {
        WorkersList.Builder result = WorkersList.newBuilder();

        discoveryClient.getInstances(workerServiceId).stream()
                .map(si -> Worker.newBuilder()
                        .setHost(si.getHost())
                        .setPort(Integer.parseInt(si.getMetadata().get("grpcPort")))
                        .setEnvironment(si.getMetadata().get("workerId"))
                        .build())
                .forEach(result::addWorkers);

        return result.build();
    }

    private WorkerLocatorExpression getWorkerLocatorExpression(JobConfig jobConfig) {
        return Optional.ofNullable(workerLocatorExpressionDataService.getByService(jobConfig.getService()))
                .orElse(defaultWorkerLocatorExpression);
    }

    private String getWorkerId(Job job) {
        return getWorkerId(job.getConfig());
    }

    private String getWorkerId(JobConfig jobConfig) {
        String expression = getWorkerLocatorExpression(jobConfig).getExpression();
        LOG.debug("Locating worker for jobConfig {} by expression: {}", jobConfig.getId(), expression);
        return expressionParser.parseExpression(expression).getValue(jobConfig).toString();
    }
}
