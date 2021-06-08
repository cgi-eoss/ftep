package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.batch.service.JobExpansionService;
import com.cgi.eoss.ftep.costing.CostingService;
import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.rpc.BuildServiceParams;
import com.cgi.eoss.ftep.rpc.BuildServiceResponse;
import com.cgi.eoss.ftep.rpc.CancelJobParams;
import com.cgi.eoss.ftep.rpc.CancelJobResponse;
import com.cgi.eoss.ftep.rpc.FtepJobLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.JobParam;
import com.cgi.eoss.ftep.rpc.StopServiceParams;
import com.cgi.eoss.ftep.rpc.StopServiceResponse;
import com.cgi.eoss.ftep.rpc.SubmitJobResponse;
import com.cgi.eoss.ftep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.google.common.util.concurrent.Striped;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import static java.util.stream.Collectors.toMap;

/**
 * <p>Primary entry point for WPS services to launch in F-TEP.</p>
 * <p>Provides access to F-TEP data services and job distribution capability.</p>
 */
@Service
@Log4j2
@GRpcService
public class FtepJobLauncher extends FtepJobLauncherGrpc.FtepJobLauncherImplBase {

    private final Striped<Lock> imageBuildUpdateLocks;
    private final Striped<Lock> jobUpdateLocks;
    private final WorkerFactory workerFactory;
    private final JobDataService jobDataService;
    private final FtepFileRegistrar ftepFileRegistrar;
    private final CostingService costingService;
    private final FtepQueueService ftepQueueService;
    private final ServiceDataService serviceDataService;
    private final JobExpansionService jobExpansionService;

    @Autowired
    public FtepJobLauncher(Striped<Lock> imageBuildUpdateLocks,
                           Striped<Lock> jobUpdateLocks,
                           WorkerFactory workerFactory,
                           JobDataService jobDataService,
                           FtepFileRegistrar ftepFileRegistrar,
                           CostingService costingService,
                           FtepQueueService ftepQueueService,
                           ServiceDataService serviceDataService,
                           JobExpansionService jobExpansionService) {
        this.imageBuildUpdateLocks = imageBuildUpdateLocks;
        this.jobUpdateLocks = jobUpdateLocks;
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.ftepFileRegistrar = ftepFileRegistrar;
        this.costingService = costingService;
        this.ftepQueueService = ftepQueueService;
        this.serviceDataService = serviceDataService;
        this.jobExpansionService = jobExpansionService;
    }

    /**
     * Transforms the gRPC request into ObjectMessages and initiates their submission into the Queue.
     */
    @Override
    public void submitJob(FtepServiceParams request, StreamObserver<SubmitJobResponse> responseObserver) {
        Lock lock = jobUpdateLocks.get(request.getJobId());
        LOG.trace("Getting exclusive lock to submit job: {}", request.getJobId());
        lock.lock();
        try {
            LOG.trace("Processing job submission: {}", request.getJobId());
            Optional<Job> baseJob = Optional.empty();
            try (CloseableThreadContext.Instance ctc = CloseableThreadContext.push("F-TEP Service Orchestrator")
                    .put("userId", request.getUserId()).put("serviceId", request.getServiceId()).put("zooId", request.getJobId())) {
                List<JobSpec> jobSpecs = jobExpansionService.expandJobParamsFromRequest(request);

                if (jobSpecs.size() == 1) {
                    baseJob = Optional.of(jobDataService.getById(jobSpecs.get(0).getJob().getIntJobId()));
                } else {
                    baseJob = Optional.of(jobDataService.getById(jobSpecs.get(0).getJob().getIntJobId()).getParentJob());
                }

                responseObserver.onNext(SubmitJobResponse.newBuilder().setJob(GrpcUtil.toRpcJob(baseJob.get())).build());

                Map<JobSpec, Job> persistentJobs = jobSpecs.stream()
                        .collect(toMap(
                                jobSpec -> jobSpec,
                                jobSpec -> jobDataService.getById(jobSpec.getJob().getIntJobId())
                        ));

                long estimatedCost = persistentJobs.values().stream()
                        .mapToLong(job -> costingService.estimateJobCost(job.getConfig(), true))
                        .sum();
                if (estimatedCost > baseJob.get().getOwner().getWallet().getBalance()) {
                    throw new ServiceExecutionException("Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
                }

                int priorityIdx = 0;
                for (JobSpec jobSpec : jobSpecs) {
                    Job job = persistentJobs.get(jobSpec);

                    // Creates FtepFile records for each input, and validates user access
                    ftepFileRegistrar.registerAndCheckInputs(job);

                    //TODO: Check that the user can use the geoserver spec
                    if (!checkAccessToOutputCollection(job.getOwner(), jobSpec.getInputsList())) {
                        LOG.error("User {} does not have read access to all requested output collections", request.getUserId());
                        throw new ServiceExecutionException("User does not have read access to all requested output collections");
                    }

                    costingService.chargeForJob(job.getOwner().getWallet(), job);

                    Map<String, Object> messageHeaders = workerFactory.getMessageHeaders(job);
                    ftepQueueService.sendObject(FtepQueueService.jobQueueName, messageHeaders, jobSpec, getJobPriority(priorityIdx++));

                    Logging.withUserLoggingContext(() -> LOG.info("Submitted job to work queue: {} (UUID {})", job.getId(), job.getExtId()));
                }

                responseObserver.onCompleted();
            } catch (Exception e) {
                baseJob.ifPresent(j -> endJobWithError(j, e));
                LOG.error("Failed to instantiate job. Notifying gRPC client", e);
                responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
            }
        } finally {
            LOG.trace("Unlocking after processing job submission: {}", request.getJobId());
            lock.unlock();
        }
    }

    private boolean checkAccessToOutputCollection(User user, List<JobParam> rpcInputs) {
        // TODO Implement output collections
        return true;
    }

    private int getJobPriority(int messageNumber) {
        if (messageNumber >= 0 && messageNumber < 10) {
            return 6;
        } else if (messageNumber >= 10 && messageNumber < 30) {
            return 5;
        } else if (messageNumber >= 30 && messageNumber < 70) {
            return 4;
        } else if (messageNumber >= 70 && messageNumber < 150) {
            return 3;
        } else if (messageNumber >= 150 && messageNumber < 310) {
            return 2;
        }
        return 1;
    }

    // gRPC interface
    @Override
    public void buildService(BuildServiceParams buildServiceParams, StreamObserver<BuildServiceResponse> responseObserver) {
        Lock lock = imageBuildUpdateLocks.get(buildServiceParams.getServiceName());
        LOG.trace("Getting exclusive lock to start Docker image build: {}", buildServiceParams.getServiceName());
        lock.lock();
        try {
            LOG.trace("Starting Docker image build: {}", buildServiceParams.getServiceName());
            Long serviceId = Long.parseLong(buildServiceParams.getServiceId());
            FtepService service = serviceDataService.getById(serviceId);
            DockerImageConfig dockerImageConfig = DockerImageConfig.newBuilder()
                    .setDockerImage(service.getDockerTag())
                    .setServiceName(service.getName())
                    .setBuildFingerprint(buildServiceParams.getBuildFingerprint())
                    .build();
            ftepQueueService.sendObject(FtepQueueService.dockerImageBuildsQueueName, dockerImageConfig);
            responseObserver.onNext(BuildServiceResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to trigger building a Docker image", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        } finally {
            LOG.trace("Unlocking after starting Docker image build: {}", buildServiceParams.getServiceName());
            lock.unlock();
        }
    }

    private void endJobWithError(Job job, Throwable cause) {
        LOG.error("Error in Job {}", job.getExtId(), cause);
        job.setStatus(Job.Status.ERROR);
        job.setEndTime(LocalDateTime.now(ZoneOffset.UTC));
        jobDataService.save(job);
    }

    // gRPC interface
    @Override
    public void cancelJob(CancelJobParams request, StreamObserver<CancelJobResponse> responseObserver) {
        Lock lock = jobUpdateLocks.get(request.getJob().getId());
        LOG.trace("Getting exclusive lock to cancel job: {}", request.getJob().getId());
        lock.lock();
        try {
            LOG.trace("Processing job cancellation: {}", request.getJob().getId());
            com.cgi.eoss.ftep.rpc.Job rpcJob = request.getJob();
            Job job = jobDataService.getById(rpcJob.getIntJobId());
            Set<Job> subJobs = job.getSubJobs();
            if (subJobs.size() > 0) {
                for (Job subJob : subJobs) {
                    if (subJob.getStatus() != Job.Status.CANCELLED) {
                        cancelJob(subJob);
                    }
                }
                //TODO Check if this implies parent is completed
            } else if (job.getStatus() != Job.Status.CANCELLED) {
                cancelJob(job);
            }
            responseObserver.onNext(CancelJobResponse.newBuilder().build());
            responseObserver.onCompleted();
        } finally {
            LOG.trace("Unlocking after processing job cancellation: {}", request.getJob().getId());
            lock.unlock();
        }
    }

    private void cancelJob(Job job) {
        LOG.info("Cancelling job with id {}", job.getId());
        JobSpec queuedJobSpec = (JobSpec) ftepQueueService.receiveObject(FtepQueueService.jobQueueName, "jobId = " + job.getId());
        if (queuedJobSpec != null) {
            LOG.info("Refunding user for job id {}", job.getId());
            job.setStatus(Status.CANCELLED);
            jobDataService.save(job);
        }
    }

    // gRPC interface
    @Override
    public void stopJob(StopServiceParams request, StreamObserver<StopServiceResponse> responseObserver) {
        Lock lock = jobUpdateLocks.get(request.getJob().getId());
        lock.lock();
        try {
            com.cgi.eoss.ftep.rpc.Job rpcJob = request.getJob();
            try {
                FtepWorkerGrpc.FtepWorkerBlockingStub worker = workerFactory.getWorker(jobDataService.getById(rpcJob.getIntJobId()).getConfig());
                LOG.info("Stop requested for job {}", rpcJob.getId());
                worker.stopContainer(rpcJob);
                LOG.info("Successfully stopped job {}", rpcJob.getId());
                responseObserver.onNext(StopServiceResponse.newBuilder().build());
                responseObserver.onCompleted();
            } catch (IllegalStateException e) {
                LOG.warn("Could not find F-TEP worker for job {}; marking as closed in the DB anyway", rpcJob.getId());
                Job job = jobDataService.getById(rpcJob.getIntJobId());
                job.setStatus(Status.CANCELLED);
                jobDataService.save(job);
                responseObserver.onNext(StopServiceResponse.newBuilder().build());
                responseObserver.onCompleted();
            } catch (StatusRuntimeException e) {
                if (e.getStatus() == io.grpc.Status.FAILED_PRECONDITION) {
                    LOG.warn("F-TEP worker could not locate container for job {}; marking as closed in the DB anyway", rpcJob.getId());
                    Job job = jobDataService.getById(rpcJob.getIntJobId());
                    job.setStatus(Status.CANCELLED);
                    jobDataService.save(job);
                    responseObserver.onNext(StopServiceResponse.newBuilder().build());
                    responseObserver.onCompleted();
                } else {
                    LOG.error("Failed to stop job {} - message {}; notifying gRPC client", rpcJob.getId(), e.getMessage());
                    responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
                }
            } catch (NumberFormatException e) {
                LOG.error("Failed to stop job {} - message {}; notifying gRPC client", rpcJob.getId(), e.getMessage());
                responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
            }
        } finally {
            lock.unlock();
        }
    }

}
