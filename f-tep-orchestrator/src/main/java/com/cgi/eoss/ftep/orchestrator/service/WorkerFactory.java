package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.WorkerLocatorExpression;
import com.cgi.eoss.ftep.persistence.service.WorkerLocatorExpressionDataService;
import com.cgi.eoss.ftep.rpc.Worker;
import com.cgi.eoss.ftep.rpc.WorkersList;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.expression.ExpressionParser;

import java.util.Optional;

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
        WorkerLocatorExpression expression = getWorkerLocatorExpression(jobConfig);
        LOG.debug("Locating worker for jobConfig {} by expression: {}", jobConfig.getId(), expression);
        String env = expressionParser.parseExpression(expression.getExpression()).getValue(jobConfig).toString();

        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .filter(si -> si.getMetadata().get("workerEnv").equals(env))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find registered worker for environment: " + env));

        LOG.info("Located {} worker: {}:{}", env, worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return FtepWorkerGrpc.newBlockingStub(managedChannel);
    }

    public WorkersList listWorkers() {
        WorkersList.Builder result = WorkersList.newBuilder();

        discoveryClient.getInstances(workerServiceId).stream()
                .map(si -> Worker.newBuilder()
                        .setHost(si.getHost())
                        .setPort(Integer.parseInt(si.getMetadata().get("grpcPort")))
                        .setEnvironment(si.getMetadata().get("workerEnv"))
                        .build())
                .forEach(result::addWorkers);

        return result.build();
    }

    private WorkerLocatorExpression getWorkerLocatorExpression(JobConfig jobConfig) {
        return Optional.ofNullable(workerLocatorExpressionDataService.getByService(jobConfig.getService())).orElse(defaultWorkerLocatorExpression);
    }

}
