package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.costing.CostingService;
import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.FtepServiceDockerBuildInfo;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.internal.OutputFileMetadata;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.model.internal.Pair;
import com.cgi.eoss.ftep.model.internal.RetrievedOutputFile;
import com.cgi.eoss.ftep.model.internal.Shapefile;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.rpc.BuildServiceParams;
import com.cgi.eoss.ftep.rpc.BuildServiceResponse;
import com.cgi.eoss.ftep.rpc.CancelJobParams;
import com.cgi.eoss.ftep.rpc.CancelJobResponse;
import com.cgi.eoss.ftep.rpc.FileStream;
import com.cgi.eoss.ftep.rpc.FileStreamClient;
import com.cgi.eoss.ftep.rpc.FtepJobLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepJobResponse;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.JobParam;
import com.cgi.eoss.ftep.rpc.ListWorkersParams;
import com.cgi.eoss.ftep.rpc.StopServiceParams;
import com.cgi.eoss.ftep.rpc.StopServiceResponse;
import com.cgi.eoss.ftep.rpc.WorkersList;
import com.cgi.eoss.ftep.rpc.worker.ContainerExit;
import com.cgi.eoss.ftep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc.FtepWorkerBlockingStub;
import com.cgi.eoss.ftep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobError;
import com.cgi.eoss.ftep.rpc.worker.JobEvent;
import com.cgi.eoss.ftep.rpc.worker.JobEventType;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.ftep.rpc.worker.OutputFileItem;
import com.cgi.eoss.ftep.rpc.worker.OutputFileList;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.io.MoreFiles;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * <p>Primary entry point for WPS services to launch in F-TEP.</p>
 * <p>Provides access to F-TEP data services and job distribution capability.</p>
 */
@Service
@Log4j2
@GRpcService
public class FtepJobLauncher extends FtepJobLauncherGrpc.FtepJobLauncherImplBase {

    private static final String TIMEOUT_PARAM = "timeout";
    private static final int SINGLE_JOB_PRIORITY = 9;

    private final Map<com.cgi.eoss.ftep.rpc.Job, FtepWorkerGrpc.FtepWorkerBlockingStub> jobWorkers = new HashMap<>();

    private final WorkerFactory workerFactory;
    private final JobDataService jobDataService;
    private final FtepGuiServiceManager guiService;
    private final FtepFileRegistrar ftepFileRegistrar;
    private final CostingService costingService;
    private final FtepSecurityService securityService;
    private final FtepQueueService ftepQueueService;
    private final ServiceDataService serviceDataService;
    private final DatabasketDataService databasketDataService;

    @Value("${ftep.orchestrator.gui.baseUrl:http://ftep}")
    private String baseUrl;

    private final Map<String, StreamObserver<FtepJobResponse>> responseObservers = new HashMap<>();

    @Autowired
    public FtepJobLauncher(WorkerFactory workerFactory, JobDataService jobDataService,
                           FtepGuiServiceManager guiService, FtepFileRegistrar ftepFileRegistrar,
                           CostingService costingService, FtepSecurityService securityService,
                           FtepQueueService ftepQueueService, ServiceDataService serviceDataService,
                           DatabasketDataService databasketDataService) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.guiService = guiService;
        this.ftepFileRegistrar = ftepFileRegistrar;
        this.costingService = costingService;
        this.securityService = securityService;
        this.ftepQueueService = ftepQueueService;
        this.serviceDataService = serviceDataService;
        this.databasketDataService = databasketDataService;
    }

    @Override
    public void launchService(FtepServiceParams request, StreamObserver<FtepJobResponse> responseObserver) {
        submitJob(request, responseObserver);
    }

    /**
     * Transforms the gRPC request into ObjectMessages and initiates their submission into the Queue.
     */
    @Override
    public void submitJob(FtepServiceParams request, StreamObserver<FtepJobResponse> responseObserver) {
        String zooId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();
        String jobConfigLabel = request.getJobConfigLabel();
        String parentId = request.getJobParent();
        List<JobParam> rpcInputs = request.getInputsList();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        Job job = null;
        com.cgi.eoss.ftep.rpc.Job rpcJob;
        try (CloseableThreadContext.Instance ctc = CloseableThreadContext.push("F-TEP Service Orchestrator")
                .put("userId", userId).put("serviceId", serviceId).put("zooId", zooId)) {
            if (!Strings.isNullOrEmpty(parentId)) {
                // This request attaches a subjob to an existing parent
                job = jobDataService.reload(Long.valueOf(parentId));
                FtepService service = job.getConfig().getService();
                if (service.getType() != FtepService.Type.PARALLEL_PROCESSOR) {
                    throw new ServiceExecutionException("Trying to attach a new subjob to a non parallel job");
                }
            } else {
                job = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, inputs);
            }

            rpcJob = GrpcUtil.toRpcJob(job);
            // Post back the job metadata for async responses
            responseObserver.onNext(FtepJobResponse.newBuilder().setJob(rpcJob).build());

            ctc.put("jobId", String.valueOf(job.getId()));
            FtepService service = job.getConfig().getService();
            if (service.getType() == FtepService.Type.PARALLEL_PROCESSOR) {
                Collection<String> parallelInput = inputs.get("parallelInputs");
                List<String> newInputs = explodeParallelInput(parallelInput);
                checkCost(job.getOwner(), job.getConfig(), newInputs);

                // Creates FtepFile records for each input, and validates user access
                ftepFileRegistrar.registerAndCheckInputs(job);

                //TODO: Check that the user can use the geoserver spec
                if (!checkAccessToOutputCollection(job.getOwner(), rpcInputs)) {
                    LOG.error("User {} does not have read access to all requested output collections", userId);
                    throw new ServiceExecutionException("User does not have read access to all requested output collections");
                }

                jobDataService.save(job);
                responseObservers.put(job.getExtId(), responseObserver);

                List<Job> subJobs = createSubJobs(job, userId, service, newInputs, inputs);
                int p = 0;
                for (Job subJob : subJobs) {
                    chargeUser(subJob.getOwner(), subJob);
                    submitJobRequestToQueue(subJob, GrpcUtil.toRpcJob(subJob), GrpcUtil.mapToParams(subJob.getConfig().getInputs()), getJobPriority(p));
                    p++;
                }
            } else {
                checkCost(job.getOwner(), job.getConfig());

                // Creates FtepFile records for each input, and validates user access
                ftepFileRegistrar.registerAndCheckInputs(job);

                //TODO: Check that the user can use the geoserver spec
                if (!checkAccessToOutputCollection(job.getOwner(), rpcInputs)) {
                    LOG.error("User {} does not have read access to all requested output collections", userId);
                    throw new ServiceExecutionException("User does not have read access to all requested output collections");
                }

                chargeUser(job.getOwner(), job);

                responseObservers.put(job.getExtId(), responseObserver);

                submitJobRequestToQueue(job, rpcJob, rpcInputs, SINGLE_JOB_PRIORITY);
            }
        } catch (Exception e) {
            if (job != null) {
                endJobWithError(job, e);
            } else {
                LOG.error("Failed to instantiate job. Notifying gRPC client", e);
                responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    private void submitJobRequestToQueue(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, Iterable<JobParam> rpcInputs, int priority) throws IOException {
        FtepService service = job.getConfig().getService();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        JobSpec.Builder jobSpecBuilder = JobSpec.newBuilder().setService(GrpcUtil.toRpcService(service)).setJob(rpcJob).addAllInputs(rpcInputs);

        if (service.getType() == FtepService.Type.APPLICATION) {
            jobSpecBuilder.addExposedPorts(service.getApplicationPort());
        }

        if (inputs.containsKey(TIMEOUT_PARAM)) {
            int timeout = Integer.parseInt(Iterables.getOnlyElement(inputs.get(TIMEOUT_PARAM)));
            jobSpecBuilder.setHasTimeout(true).setTimeoutValue(timeout);
        }

        JobSpec jobSpec = jobSpecBuilder.build();
        HashMap<String, Object> messageHeaders = new HashMap<>();
        messageHeaders.put("jobId", job.getId());
        ftepQueueService.sendObject(FtepQueueService.jobQueueName, messageHeaders, jobSpec, priority);

        // Gets the actual worker node and stores in the map
        FtepWorkerGrpc.FtepWorkerBlockingStub worker = workerFactory.getWorker(job.getConfig());
        jobWorkers.put(rpcJob, worker);
    }

    // gRPC interface
    @Override
    public void buildService(BuildServiceParams buildServiceParams, StreamObserver<BuildServiceResponse> responseObserver) {
        Long serviceId = Long.parseLong(buildServiceParams.getServiceId());
        FtepService service = serviceDataService.getById(serviceId);
        try {
            FtepWorkerBlockingStub worker = workerFactory.getOne();
            responseObserver.onNext(BuildServiceResponse.newBuilder().build());
            DockerImageConfig dockerImageConfig = DockerImageConfig.newBuilder()
                    .setDockerImage(service.getDockerTag())
                    .setServiceName(service.getName())
                    .build();
            worker.prepareDockerImage(dockerImageConfig);
            service.getDockerBuildInfo().setDockerBuildStatus(FtepServiceDockerBuildInfo.Status.COMPLETED);
            service.getDockerBuildInfo().setLastBuiltFingerprint(buildServiceParams.getBuildFingerprint());
            serviceDataService.save(service);
        } catch (Exception e) {
            service.getDockerBuildInfo().setDockerBuildStatus(FtepServiceDockerBuildInfo.Status.ERROR);
            serviceDataService.save(service);
        }
        responseObserver.onCompleted();
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

    private void checkCost(User user, JobConfig jobConfig) {
        int estimatedCost = costingService.estimateJobCost(jobConfig);
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException("Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
    }

    private void checkCost(User user, JobConfig config, List<String> newInputs) {
        int singleJobCost = costingService.estimateJobCost(config);
        int estimatedCost = newInputs.size() * singleJobCost;
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException("Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
    }

    private void checkCost(User user, JobConfig config, int numberOfJobs) {
        int singleJobCost = costingService.estimateJobCost(config);
        int estimatedCost = numberOfJobs * singleJobCost;
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException("Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
    }

    private void chargeUser(User user, Job job) {
        costingService.chargeForJob(user.getWallet(), job);
    }

    private boolean checkAccessToOutputCollection(User user, List<JobParam> rpcInputs) {
        // TODO Implement output collections
        return true;
    }

    private List<String> explodeParallelInput(Collection<String> inputUris) {
        List<String> results = new ArrayList<>();
        for (String inputUri : inputUris) {
            if (inputUri.startsWith("ftep://databasket")) {
                Databasket dataBasket = getDatabasketFromUri(inputUri);
                results.addAll(dataBasket.getFiles().stream().map(f -> f.getUri().toString()).collect(toList()));
            } else if (inputUri.contains((","))) {
                results.addAll(Arrays.asList(inputUri.split(",")));
            } else {
                results.add(inputUri);
            }
        }
        return results;
    }

    private Databasket getDatabasketFromUri(String uri) {
        Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(uri);
        if (!uriIdMatcher.matches()) {
            throw new ServiceExecutionException("Failed to load databasket for URI: " + uri);
        }
        Long databasketId = Long.parseLong(uriIdMatcher.group(1));
        Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId))
                .orElseThrow(() -> new ServiceExecutionException("Failed to load databasket for ID " + databasketId));
        LOG.debug("Listing databasket contents for id {}", databasketId);
        return databasket;
    }

    private List<Job> createSubJobs(Job parentJob, String userId, FtepService service, List<String> newInputs, Multimap<String, String> inputs) {
        List<Job> childJobs = new ArrayList<>();
        // Create the simpler map of parameters shared by all parallel jobs
        SetMultimap<String, String> sharedParams = MultimapBuilder.hashKeys().hashSetValues().build(inputs);
        sharedParams.removeAll("parallelInputs");
        for (String newInput : newInputs) {
            SetMultimap<String, String> parallelJobParams = MultimapBuilder.hashKeys().hashSetValues().build(sharedParams);
            parallelJobParams.put("input", newInput);
            Job childJob = jobDataService.buildNew(UUID.randomUUID().toString(), userId, service.getName(), parentJob.getConfig().getLabel(), parallelJobParams, parentJob);
            childJob = jobDataService.reload(childJob.getId());
            parentJob.getSubJobs().add(childJob);
            childJobs.add(childJob);
        }
        parentJob.setParent(true);
        jobDataService.save(parentJob);
        return childJobs;
    }

    @JmsListener(destination = FtepQueueService.jobUpdatesQueueName)
    public void receiveJobUpdate(@Payload ObjectMessage objectMessage, @Header("workerId") String workerId, @Header("jobId") String internalJobId) {
        Job job = jobDataService.reload(Long.parseLong(internalJobId));
        // TODO change into Chain of Responsibility type pattern
        Serializable update = null;
        try {
            update = objectMessage.getObject();
        } catch (JMSException e) {
            onJobError(job, e);
        }
        if (update instanceof JobEvent) {
            JobEvent jobEvent = (JobEvent) update;
            JobEventType jobEventType = jobEvent.getJobEventType();
            if (null != jobEventType) switch (jobEventType) {
                case DATA_FETCHING_STARTED:
                    onJobDataFetchingStarted(job, workerId);
                    break;
                case DATA_FETCHING_COMPLETED:
                    LOG.info("Launching docker container for job {}", job.getExtId());
                    break;
                case PROCESSING_STARTED:
                    onJobProcessingStarted(job, workerId);
                    break;
                default:
                    break;
            }
        } else if (update instanceof JobError) {
            JobError jobError = (JobError) update;
            onJobError(job, jobError.getErrorDescription());
        } else if (update instanceof ContainerExit) {
            ContainerExit containerExit = (ContainerExit) update;
            try {
                onContainerExit(job, workerId, containerExit.getJobEnvironment(), containerExit.getExitCode());
            } catch (Exception e) {
                onJobError(job, e);
            }
        }
    }

    private void onJobDataFetchingStarted(Job job, String workerId) {
        LOG.info("Downloading input data for {}", job.getExtId());
        job.setWorkerId(workerId);
        job.setStartTime(LocalDateTime.now());
        job.setStatus(Job.Status.RUNNING);
        job.setStage(JobStep.DATA_FETCH.getText());
        jobDataService.save(job);
    }

    private void onJobProcessingStarted(Job job, String workerId) {
        FtepService service = job.getConfig().getService();
        LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(), service.getName());
        // Update GUI endpoint URL for client access
        if (service.getType() == FtepService.Type.APPLICATION) {
            String zooId = job.getExtId();
            FtepWorkerBlockingStub worker = workerFactory.getWorkerById(workerId);
            String guiUrl = guiService.getGuiUrl(worker, GrpcUtil.toRpcJob(job), service.getApplicationPort());
            LOG.info("Updating GUI URL for job {} ({}): {}", zooId, job.getConfig().getService().getName(), guiUrl);
            job.setGuiUrl(guiUrl);
            job.setGuiEndpoint(null); // No reverse-proxy implemented
            jobDataService.save(job);
        }
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);
    }

    private void onContainerExit(Job job, String workerId, JobEnvironment jobEnvironment, int exitCode) throws Exception {
        switch (exitCode) {
            case 0:
                // Normal exit
                break;
            case 137:
                LOG.info("Docker container for {} terminated via SIGKILL (exit code 137)", job.getExtId());
                break;
            case 143:
                LOG.info("Docker container for {} terminated via SIGTERM (exit code 143)", job.getExtId());
                break;
            default:
                throw new Exception("Docker container returned with exit code " + exitCode);
        }
        job.setStage(JobStep.OUTPUT_LIST.getText());
        job.setEndTime(LocalDateTime.now()); // End time is when processing ends
        job.setGuiUrl(null); // Any GUI services will no longer be available
        job.setGuiEndpoint(null); // Any GUI services will no longer be available
        jobDataService.save(job);
        try {
            ingestOutput(job, GrpcUtil.toRpcJob(job), workerFactory.getWorkerById(workerId), jobEnvironment);
        } catch (IOException e) {
            throw new Exception("Error ingesting output for : " + e.getMessage());
        }
    }

    private void onJobError(Job job, String description) {
        endJobWithError(job, new ServiceExecutionException(description));
    }

    private void onJobError(Job job, Throwable t) {
        endJobWithError(job, t);
    }

    private void endJobWithError(Job job, Throwable cause) {
        LOG.error("Error in Job {}", job.getExtId(), cause);
        job.setStatus(Job.Status.ERROR);
        job.setEndTime(LocalDateTime.now(ZoneOffset.UTC));
        jobDataService.save(job);
        responseObservers.get(job.getExtId())
                .onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(cause)));
    }

    private void ingestOutput(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerBlockingStub worker, JobEnvironment jobEnvironment) throws IOException, InterruptedException, StatusException {
        // Enumerate files in the job output directory
        Multimap<String, String> outputsByRelativePath = listOutputFiles(job, rpcJob, worker, jobEnvironment);
        // Repatriate output files
        SetMultimap<String, FtepFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker, job.getConfig().getInputs(), jobEnvironment, outputsByRelativePath);
        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(outputFiles.entries().stream().collect(toMultimap(e -> e.getKey(), e -> e.getValue().getUri().toString(), MultimapBuilder.hashKeys().hashSetValues()::build)));
        job.setOutputFiles(ImmutableSet.copyOf(outputFiles.values()));
        jobDataService.save(job);

        if (job.getConfig().getService().getType() == FtepService.Type.BULK_PROCESSOR) {
            // Auto-publish the output files
            ImmutableSet.copyOf(outputFiles.values()).forEach(f -> securityService.publish(FtepFile.class, f.getId()));
        }

        Job parentJob = job.getParentJob();
        if (parentJob != null) {
            completeParentJob(parentJob);
        } else {
            notifyResponseObserverJobComplete(job, outputFiles);
        }
    }

    private void completeParentJob(Job parentJob) throws StatusException {
        if (allChildJobCompleted(parentJob)) {

            // Must collect all child jobs, save for parent and send a response
            SetMultimap<String, FtepFile> jobOutputFiles = HashMultimap.create();
            // Only collect outputs from SUCCESSFUL subjobs
            for (Job subJob : parentJob.getSubJobs()) {
                if (subJob.getStatus().equals(Status.COMPLETED)) {
                    subJob.getOutputs().forEach((k, v) -> subJob.getOutputFiles().stream().filter(x -> x.getUri().toString().equals(v)).findFirst().ifPresent(match -> jobOutputFiles.put(k, match)));
                }
            }

            // Wrap up the parent job
            parentJob.setStatus(Job.Status.COMPLETED);
            parentJob.setStage(JobStep.OUTPUT_LIST.getText());
            parentJob.setEndTime(LocalDateTime.now());
            parentJob.setGuiUrl(null);
            parentJob.setGuiEndpoint(null);
            parentJob.setOutputs(jobOutputFiles.entries().stream().collect(toMultimap(e -> e.getKey(), e -> e.getValue().getUri().toString(), HashMultimap::create)));
            parentJob.setOutputFiles(ImmutableSet.copyOf(jobOutputFiles.values()));
            jobDataService.save(parentJob);

            notifyResponseObserverJobComplete(parentJob, jobOutputFiles);
        }
        // TODO shall an error be sent in case of any subjobs error?
    }

    private void notifyResponseObserverJobComplete(Job job, SetMultimap<String, FtepFile> jobOutputFiles) throws StatusException {
        List<JobParam> outputs = jobOutputFiles.entries().stream()
                .map(e -> JobParam.newBuilder().setParamName(e.getKey()).addParamValue(e.getValue().getUri().toASCIIString()).build())
                .collect(toList());
        StreamObserver<FtepJobResponse> responseObserver = responseObservers.get(job.getExtId());
        if (responseObserver != null) {
            responseObserver.onNext(FtepJobResponse.newBuilder()
                    .setJob(GrpcUtil.toRpcJob(job))
                    .setJobOutputs(FtepJobResponse.JobOutputs.newBuilder().addAllOutputs(outputs).build())
                    .build());
            responseObserver.onCompleted();
        } else {
            String description = "Failed to ingest outputs? TODO fix this note!";
            LOG.error("Failed to run processor: {}; notifying gRPC client", description);
            io.grpc.Status status = io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withDescription(description);
            throw status.asException();
        }
    }

    private boolean allChildJobCompleted(Job parentJob) {
        return !parentJob.getSubJobs().stream().anyMatch(j -> j.getStatus() != Job.Status.COMPLETED && j.getStatus() != Job.Status.ERROR);
    }

    private SetMultimap<String, String> listOutputFiles(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerGrpc.FtepWorkerBlockingStub worker, JobEnvironment jobEnvironment) {
        FtepService service = job.getConfig().getService();

        OutputFileList outputFileList = worker.listOutputFiles(ListOutputFilesParam.newBuilder()
                .setJob(rpcJob)
                .setOutputsRootPath(jobEnvironment.getOutputDir())
                .build());
        List<String> relativePaths = outputFileList.getItemsList().stream()
                .map(OutputFileItem::getRelativePath)
                .collect(toList());

        SetMultimap<String, String> outputsByRelativePath;
        if (service.getType() == FtepService.Type.APPLICATION) {
            // Collect all files in the output directory with simple index IDs
            outputsByRelativePath = IntStream.range(0, relativePaths.size()).boxed()
                    .collect(HashMultimap::create, (mm, i) -> mm.put(Integer.toString(i + 1), relativePaths.get(i)), Multimap::putAll);
        } else {
            // Ensure outputs are in the
            List<FtepServiceDescriptor.Parameter> expectedServiceOutputs = service.getServiceDescriptor().getDataOutputs();
            outputsByRelativePath = HashMultimap.create();

            for (FtepServiceDescriptor.Parameter expectedOutput : expectedServiceOutputs) {
                List<String> relativePathValues = relativePaths.stream()
                        .filter(path -> path.startsWith(expectedOutput.getId() + "/"))
                        .collect(Collectors.toList());

                if (relativePathValues.isEmpty()) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        if (expectedOutput.getMinOccurs() == 0) {
                            LOG.info("Service defined optional output with ID '{}' which was not found in the job outputs", expectedOutput.getId());
                        } else {
                            LOG.info("Service defined required output with ID '{}' which was not found in the job outputs", expectedOutput.getId());
                            throw new ServiceExecutionException(String.format("Did not find expected single output for '%s' in outputs list: %s", expectedOutput.getId(), relativePaths));
                        }
                    }
                } else if (expectedOutput.getMinOccurs() <= relativePathValues.size() && relativePathValues.size() <= expectedOutput.getMaxOccurs()) {
                    LOG.debug("Found acceptable number of output files for ID {} (min {}, max {})", expectedOutput.getId(), expectedOutput.getMinOccurs(), expectedOutput.getMaxOccurs());
                    outputsByRelativePath.putAll(expectedOutput.getId(), relativePathValues);
                } else {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.info("Service defined output with ID '{}' with between {} and {} files, but found {}", expectedOutput.getId(), expectedOutput.getMinOccurs(), expectedOutput.getMaxOccurs(), relativePathValues.size());
                        throw new ServiceExecutionException(String.format("Did not find acceptable number of outputs for '%s' in outputs list: %s", expectedOutput.getId(), relativePaths));
                    }
                }
            }
        }
        return outputsByRelativePath;
    }

    private SetMultimap<String, FtepFile> repatriateAndIngestOutputFiles(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob,
                                                                         FtepWorkerGrpc.FtepWorkerBlockingStub worker,
                                                                         Multimap<String, String> inputs,
                                                                         JobEnvironment jobEnvironment,
                                                                         Multimap<String, String> outputsByRelativePath) throws IOException, InterruptedException {
        List<RetrievedOutputFile> retrievedOutputFiles = new ArrayList<>(outputsByRelativePath.size());

        SetMultimap<String, FtepFile> outputFiles = HashMultimap.create();
        Map<String, GeoServerSpec> geoServerSpecs = getGeoServerSpecs(inputs);
        Map<String, String> collectionSpecs = getCollectionSpecs(inputs);

        for (String outputId : outputsByRelativePath.keySet()) {
            OutputProductMetadata outputProduct = getOutputMetadata(job, geoServerSpecs, collectionSpecs, outputId);

            for (String relativePath : outputsByRelativePath.get(outputId)) {
                GetOutputFileParam getOutputFileParam = GetOutputFileParam.newBuilder().setJob(rpcJob)
                        .setPath(Paths.get(jobEnvironment.getOutputDir()).resolve(relativePath).toString()).build();

                FtepWorkerGrpc.FtepWorkerStub asyncWorker = FtepWorkerGrpc.newStub(worker.getChannel());

                try (FileStreamClient<GetOutputFileParam> fileStreamClient = new FileStreamClient<GetOutputFileParam>() {
                    private OutputFileMetadata outputFileMetadata;

                    @Override
                    public OutputStream buildOutputStream(FileStream.FileMeta fileMeta) throws IOException {
                        LOG.info("Collecting output '{}' with filename {} ({} bytes)", outputId, fileMeta.getFilename(),
                                fileMeta.getSize());

                        OutputFileMetadata.OutputFileMetadataBuilder outputFileMetadataBuilder = OutputFileMetadata.builder();

                        outputFileMetadata = outputFileMetadataBuilder.outputProductMetadata(outputProduct)
                                .build();

                        setOutputPath(ftepFileRegistrar.prepareNewOutputProduct(outputProduct, relativePath.toString()));
                        LOG.info("Writing output file for job {}: {}", job.getExtId(), getOutputPath());
                        return new BufferedOutputStream(Files.newOutputStream(getOutputPath(), CREATE, TRUNCATE_EXISTING, WRITE));
                    }

                    @Override
                    public void onCompleted() {
                        super.onCompleted();
                        Pair<OffsetDateTime, OffsetDateTime> startEndDateTimes = getStartEndDateTimes(outputId);
                        outputFileMetadata.setStartDateTime(startEndDateTimes.getKey());
                        outputFileMetadata.setEndDateTime(startEndDateTimes.getValue());
                        retrievedOutputFiles.add(new RetrievedOutputFile(outputFileMetadata, getOutputPath()));
                    }

                    private Pair<OffsetDateTime, OffsetDateTime> getStartEndDateTimes(String outputId) {
                        try {
                            //Retrieve the parameter
                            Optional<FtepServiceDescriptor.Parameter> outputParameter = getServiceOutputParameter(outputId);
                            if (outputParameter.isPresent()) {
                                String regexp = outputParameter.get().getTimeRegexp();
                                if (regexp != null) {
                                    Pattern p = Pattern.compile(regexp);
                                    Matcher m = p.matcher(getOutputPath().getFileName().toString());
                                    if (m.find()) {
                                        if (regexp.contains("?<startEnd>")) {
                                            OffsetDateTime startEndDateTime = parseOffsetDateTime(m.group("startEnd"), LocalTime.MIDNIGHT);
                                            return Pair.of(startEndDateTime, startEndDateTime);
                                        } else {
                                            OffsetDateTime start = null, end = null;
                                            if (regexp.contains("?<start>")) {
                                                start = parseOffsetDateTime(m.group("start"), LocalTime.MIDNIGHT);
                                            }

                                            if (regexp.contains("?<end>")) {
                                                end = parseOffsetDateTime(m.group("end"), LocalTime.MIDNIGHT);
                                            }
                                            return Pair.of(start, end);
                                        }
                                    }
                                }
                            }
                        } catch (RuntimeException e) {
                            LOG.error("Unable to parse date from regexp");
                        }
                        return Pair.of(null, null);
                    }

                    private Optional<FtepServiceDescriptor.Parameter> getServiceOutputParameter(String outputId) {
                        return job.getConfig().getService().getServiceDescriptor().getDataOutputs().stream().filter(p -> p.getId().equals(outputId)).findFirst();
                    }

                    private OffsetDateTime parseOffsetDateTime(String startDateStr, LocalTime defaultTime) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd[[ ]['T']HHmm[ss][.SSS][XXX]]");
                        TemporalAccessor temporalAccessor = formatter.parseBest(startDateStr, OffsetDateTime::from, LocalDate::from);
                        if (temporalAccessor instanceof OffsetDateTime) {
                            return (OffsetDateTime) temporalAccessor;
                        } else if (temporalAccessor instanceof LocalDateTime) {
                            return ((LocalDateTime) temporalAccessor).atOffset(ZoneOffset.UTC);
                        } else {
                            return ((LocalDate) temporalAccessor).atTime(defaultTime).atOffset(ZoneOffset.UTC);
                        }
                    }
                }) {
                    asyncWorker.getOutputFile(getOutputFileParam, fileStreamClient.getFileStreamObserver());
                    fileStreamClient.getLatch().await();
                }
            }
        }

        postProcessOutputProducts(retrievedOutputFiles).forEach(Unchecked.consumer(retrievedOutputFile -> {
            outputFiles.put(
                    retrievedOutputFile.getOutputFileMetadata().getOutputProductMetadata().getOutputId(),
                    ftepFileRegistrar.registerOutput(retrievedOutputFile.getOutputFileMetadata(), retrievedOutputFile.getPath()));
        }));
        return outputFiles;
    }

    private Map<String, GeoServerSpec> getGeoServerSpecs(Multimap<String, String> inputs) {
        // TODO Implement GeoServer output control
        return Collections.emptyMap();
    }

    private Map<String, String> getCollectionSpecs(Multimap<String, String> inputs) {
        // TODO Implement output collections
        return Collections.emptyMap();
    }

    private OutputProductMetadata getOutputMetadata(Job job, Map<String, GeoServerSpec> geoServerSpecs,
                                                    Map<String, String> collectionSpecs, String outputId) {
        OutputProductMetadata.OutputProductMetadataBuilder outputProductMetadataBuilder = OutputProductMetadata.builder()
                .owner(job.getOwner())
                .service(job.getConfig().getService())
                .outputId(outputId)
                .jobId(job.getExtId());

        HashMap<String, Object> properties = new HashMap<>(ImmutableMap.<String, Object>builder()
                .put("jobId", job.getExtId()).put("intJobId", job.getId())
                .put("serviceName", job.getConfig().getService().getName())
                .put("jobOwner", job.getOwner().getName())
                .put("jobStartTime",
                        job.getStartTime().atOffset(ZoneOffset.UTC).toString())
                .put("jobEndTime", job.getEndTime().atOffset(ZoneOffset.UTC).toString())
                .build());

        GeoServerSpec geoServerSpecForOutput = geoServerSpecs.get(outputId);
        if (geoServerSpecForOutput != null) {
            properties.put("geoServerSpec", geoServerSpecForOutput);
        }

        String collectionSpecForOutput = collectionSpecs.get(outputId);
        if (collectionSpecForOutput != null) {
            properties.put("collection", collectionSpecForOutput);
        }

        return outputProductMetadataBuilder.productProperties(properties).build();
    }

    private List<RetrievedOutputFile> postProcessOutputProducts(List<RetrievedOutputFile> retrievedOutputFiles) throws IOException {
        // TODO Extract post-process possibilities to separate classes

        // Detect and zip up shapefiles
        Set<RetrievedOutputFile> shapefiles = retrievedOutputFiles.stream()
                .filter(output -> MoreFiles.getFileExtension(output.getPath()).equals("shp"))
                .collect(toSet());
        for (RetrievedOutputFile shapefile : shapefiles) {
            LOG.info("Processing detected shapefile: {}", shapefile);
            postProcessShapefile(shapefile, retrievedOutputFiles);
        }

        // Try to read CRS/AOI from all files - note that CRS/AOI may still be null after this
        retrievedOutputFiles.stream()
                .peek(retrievedOutputFile -> LOG.debug("Attempting to extract geometry from output file {}", retrievedOutputFile.getPath()))
                .forEach(retrievedOutputFile -> {
                    retrievedOutputFile.getOutputFileMetadata().setCrs(getOutputCrs(retrievedOutputFile.getPath()));
                    retrievedOutputFile.getOutputFileMetadata().setGeometry(getOutputGeometry(retrievedOutputFile.getPath()));
                });
        return retrievedOutputFiles;
    }

    private void postProcessShapefile(RetrievedOutputFile shapefile, List<RetrievedOutputFile> outputProducts) throws IOException {
        // Clone the metadata of the .shp
        OutputFileMetadata originalMetadata = shapefile.getOutputFileMetadata();
        OutputFileMetadata shapefileMetadata = OutputFileMetadata.builder()
                .outputProductMetadata(OutputProductMetadata.builder()
                        .owner(originalMetadata.getOutputProductMetadata().getOwner())
                        .service(originalMetadata.getOutputProductMetadata().getService())
                        .outputId(originalMetadata.getOutputProductMetadata().getOutputId())
                        .jobId(originalMetadata.getOutputProductMetadata().getJobId())
                        .productProperties(originalMetadata.getOutputProductMetadata().getProductProperties())
                        .build())
                .crs(Optional.ofNullable(originalMetadata.getCrs()).orElseGet(() -> getOutputCrs(shapefile.getPath())))
                .geometry(Optional.ofNullable(originalMetadata.getGeometry()).orElseGet(() -> getOutputGeometry(shapefile.getPath())))
                .build();

        Shapefile zippedShapefile = GeoUtil.zipShapeFile(shapefile.getPath(), true);

        // Remove the individual shapefile sidecar files from the outputProducts list, then add the new zip
        zippedShapefile.getContents().forEach(f -> outputProducts.removeIf(p -> p.getPath().equals(f)));
        outputProducts.add(new RetrievedOutputFile(shapefileMetadata, zippedShapefile.zip));
    }

    private String getOutputCrs(Path outputPath) {
        try {
            return GeoUtil.extractEpsg(outputPath);
        } catch (Exception e) {
            return null;
        }
    }

    private String getOutputGeometry(Path outputPath) {
        try {
            return GeoUtil.geojsonToWkt(GeoUtil.extractBoundingBox(outputPath));
        } catch (Exception e) {
            return null;
        }
    }

    // gRPC interface
    @Override
    public void cancelJob(CancelJobParams request, StreamObserver<CancelJobResponse> responseObserver) {
        com.cgi.eoss.ftep.rpc.Job rpcJob = request.getJob();
        Job job = jobDataService.getById(Long.parseLong(rpcJob.getIntJobId()));
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
    }

    private void cancelJob(Job job) {
        LOG.info("Cancelling job with id {}", job.getId());
        JobSpec queuedJobSpec = (JobSpec) ftepQueueService.receiveSelectedObject(FtepQueueService.jobQueueName, "jobId = " + job.getId());
        if (queuedJobSpec != null) {
            LOG.info("Refunding user for job id {}", job.getId());
            job.setStatus(Status.CANCELLED);
            jobDataService.save(job);
        }
    }

    // gRPC interface
    @Override
    public void stopJob(StopServiceParams request, StreamObserver<StopServiceResponse> responseObserver) {
        com.cgi.eoss.ftep.rpc.Job rpcJob = request.getJob();
        try {
            Job job = jobDataService.getById(Long.parseLong(rpcJob.getIntJobId()));
            //FtepWorkerGrpc.FtepWorkerBlockingStub worker = Optional.ofNullable(workerFactory.getWorkerById(job.getWorkerId())).orElseThrow(() -> new IllegalStateException("F-TEP worker not found for job " + rpcJob.getId()));
            FtepWorkerGrpc.FtepWorkerBlockingStub worker = Optional.ofNullable(jobWorkers.get(rpcJob)).orElseThrow(() -> new IllegalStateException("F-TEP worker not found for job " + rpcJob.getId()));
            LOG.info("Stop requested for job {}", rpcJob.getId());
            worker.stopContainer(rpcJob);
            LOG.info("Successfully stopped job {}", rpcJob.getId());
            responseObserver.onNext(StopServiceResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (NumberFormatException e) {
            LOG.error("Failed to stop job {} - message {}; notifying gRPC client", rpcJob.getId(), e.getMessage());
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        } finally {
            jobWorkers.remove(rpcJob);
        }
    }

    // gRPC interface
    public void listWorkers(ListWorkersParams request, StreamObserver<WorkersList> responseObserver) {
        try {
            responseObserver.onNext(workerFactory.listWorkers());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to enumerate workers", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }
}
