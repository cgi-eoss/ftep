package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.rpc.worker.ContainerExit;
import com.cgi.eoss.ftep.rpc.worker.DockerImageBuildEvent;
import com.cgi.eoss.ftep.rpc.worker.JobError;
import com.cgi.eoss.ftep.rpc.worker.JobEvent;
import com.google.common.util.concurrent.Striped;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;

@Log4j2
@Service
public class FtepOrchestratorDispatcher {

    private final Striped<Lock> imageBuildUpdateLocks;
    private final Striped<Lock> jobUpdateLocks;
    private final FtepQueueService queueService;
    private final FtepOrchestratorService ftepOrchestratorService;

    @Autowired
    public FtepOrchestratorDispatcher(Striped<Lock> imageBuildUpdateLocks, Striped<Lock> jobUpdateLocks, FtepQueueService queueService, FtepOrchestratorService ftepOrchestratorService) {
        this.imageBuildUpdateLocks = imageBuildUpdateLocks;
        this.jobUpdateLocks = jobUpdateLocks;
        this.queueService = queueService;
        this.ftepOrchestratorService = ftepOrchestratorService;
    }

    @JmsListener(destination = FtepQueueService.dockerImageBuildUpdatesQueueName)
    public void receiveDockerImageBuildUpdate(@Payload ObjectMessage objectMessage, @Header("serviceName") String serviceName, @Header("buildFingerprint") String buildFingerprint) {
        Lock lock = imageBuildUpdateLocks.get(serviceName);
        LOG.trace("Getting exclusive lock to update Docker image build status: {}", serviceName);
        lock.lock();
        try {
            Serializable update = objectMessage.getObject();
            if (update instanceof DockerImageBuildEvent) {
                ftepOrchestratorService.updateBuildStatus(serviceName, buildFingerprint, (DockerImageBuildEvent) update);
            } else {
                LOG.warn("Unrecognised Docker image build event type {} (serviceName: {}, buildFingerprint: {})", objectMessage, serviceName, buildFingerprint);
            }
        } catch (JMSException e) {
            LOG.warn("Failed to get message payload from Docker image build updates queue (serviceName: {}, buildFingerprint: {})", serviceName, buildFingerprint, e);
        } finally {
            LOG.trace("Unlocking after processing Docker image build update: {}", serviceName);
            lock.unlock();
        }
    }

    @JmsListener(destination = FtepQueueService.jobUpdatesQueueName)
    public void receiveJobUpdate(@Payload ObjectMessage objectMessage, @Header("workerId") String workerId, @Header("zooId") String jobId, @Header("jobId") String internalJobId) {
        Lock lock = jobUpdateLocks.get(jobId);
        LOG.trace("Getting exclusive lock to process job event: {}", jobId);
        lock.lock();
        try {
            Serializable update = objectMessage.getObject();
            if (update instanceof JobEvent) {
                ftepOrchestratorService.processJobEvent(workerId, jobId, internalJobId, ((JobEvent) update));
            } else if (update instanceof JobError) {
                ftepOrchestratorService.processJobError(workerId, jobId, internalJobId, ((JobError) update));
            } else if (update instanceof ContainerExit) {
                ftepOrchestratorService.processContainerExit(workerId, jobId, internalJobId, ((ContainerExit) update));
            } else {
                ftepOrchestratorService.processJobError(workerId, jobId, internalJobId, new ServiceExecutionException("Unrecognised Job update type " + objectMessage));
            }
        } catch (JMSException e) {
            ftepOrchestratorService.processJobError(workerId, jobId, internalJobId, e);
        } finally {
            LOG.trace("Unlocking after processing job event: {}", jobId);
            lock.unlock();
        }
    }

}
