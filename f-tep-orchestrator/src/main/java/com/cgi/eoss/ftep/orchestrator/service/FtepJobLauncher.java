package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.costing.CostingService;
import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor.Parameter;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.model.internal.Shapefile;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.rpc.BuildServiceParams;
import com.cgi.eoss.ftep.rpc.BuildServiceResponse;
import com.cgi.eoss.ftep.rpc.CancelJobParams;
import com.cgi.eoss.ftep.rpc.CancelJobResponse;
import com.cgi.eoss.ftep.rpc.FileStream;
import com.cgi.eoss.ftep.rpc.FileStreamClient;
import com.cgi.eoss.ftep.rpc.FtepJobLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.FtepServiceResponse;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.JobParam;
import com.cgi.eoss.ftep.rpc.ListWorkersParams;
import com.cgi.eoss.ftep.rpc.StopServiceParams;
import com.cgi.eoss.ftep.rpc.StopServiceResponse;
import com.cgi.eoss.ftep.rpc.WorkersList;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc.FtepWorkerBlockingStub;
import com.cgi.eoss.ftep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.ftep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.rpc.worker.LaunchContainerResponse;
import com.cgi.eoss.ftep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.ftep.rpc.worker.OutputFileItem;
import com.cgi.eoss.ftep.rpc.worker.OutputFileList;
import com.cgi.eoss.ftep.rpc.worker.StopContainerResponse;
import com.cgi.eoss.ftep.security.FtepSecurityService;

//import com.fasterxml.jackson.core.JsonParseException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.type.MapType;
//import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
//import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.io.MoreFiles;
//import com.mysema.commons.lang.Pair;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

//import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
//import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.time.LocalDate;
import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.OffsetDateTime;
import java.time.ZoneOffset;
//import java.time.format.DateTimeFormatter;
//import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toMap;

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

    private final Map<String, StreamObserver<FtepServiceResponse>> responseObservers = new HashMap<>();
    private final Map<String, FtepWorkerBlockingStub> workerStubCache = new ConcurrentHashMap<>();

    @Autowired
    public FtepJobLauncher(WorkerFactory workerFactory, JobDataService jobDataService,
        FtepGuiServiceManager guiService, FtepFileRegistrar ftepFileRegistrar,
        CostingService costingService, FtepSecurityService securityService,
        FtepQueueService ftepQueueService
    ) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.guiService = guiService;
        this.ftepFileRegistrar = ftepFileRegistrar;
        this.costingService = costingService;
        this.securityService = securityService;
        this.ftepQueueService = ftepQueueService;
    }

    @Override
    public void launchService(FtepServiceParams request, StreamObserver<FtepServiceResponse> responseObserver) {
        submitJob(request, responseObserver);
    }

    /**
     * Transforms the gRPC request into ObjectMessages and initiates their submission into the Queue.
     */
    @Override
    public void submitJob(FtepServiceParams request, StreamObserver<FtepServiceResponse> responseObserver) {
        String zooId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();
        String jobConfigLabel = request.getJobConfigLabel();
        String parentId = request.getJobParent();
        List<JobParam> rpcInputs = request.getInputsList();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        Job job = null;
        com.cgi.eoss.ftep.rpc.Job rpcJob = null;
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
            responseObserver.onNext(FtepServiceResponse.newBuilder().setJob(rpcJob).build());

            ctc.put("jobId", String.valueOf(job.getId()));
            FtepService service = job.getConfig().getService();
            if (service.getType() == FtepService.Type.PARALLEL_PROCESSOR) {
                Collection<String> parallelInput = inputs.get("parallelInputs");
                List<String> newInputs = explodeParallelInput(parallelInput);
                checkCost(job.getOwner(), job.getConfig(), newInputs);

                // Creates FtepFile records for each input, and validates user access
                ftepFileRegistrar.registerAndCheckInputs(job);
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

                FtepWorkerGrpc.FtepWorkerBlockingStub worker = workerFactory.getWorker(job.getConfig());
                jobWorkers.put(rpcJob, worker);

                SetMultimap<String, FtepFile> jobOutputFiles = MultimapBuilder.hashKeys().hashSetValues().build();

                Map<String, FtepFile> jobOutputs = executeJob(job, rpcJob, rpcInputs, worker);
                jobOutputs.forEach(jobOutputFiles::put);

                chargeUser(job.getOwner(), job);

                // Transform the results for the WPS response
                List<JobParam> outputs = jobOutputFiles.asMap().entrySet().stream()
                        .map(e -> JobParam.newBuilder().setParamName(e.getKey()).addAllParamValue(e.getValue().stream().map(f -> f.getUri().toASCIIString()).collect(toSet())).build())
                        .collect(toList());

                responseObserver.onNext(FtepServiceResponse.newBuilder()
                        .setJobOutputs(FtepServiceResponse.JobOutputs.newBuilder().addAllOutputs(outputs).build())
                        .build());
                ////responseObserver.onCompleted();
                responseObservers.put(job.getExtId(), responseObserver);
                submitJobRequestToQueue(job, rpcJob, rpcInputs, SINGLE_JOB_PRIORITY);
            }
        } catch (Exception e) {
            if (job != null) {
                endJobWithError(job);
            }
            LOG.error("Failed to run processor; notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        } finally {
            Optional.ofNullable(rpcJob).ifPresent(j -> Optional.ofNullable(jobWorkers.remove(j)).ifPresent(worker -> worker.cleanUp(j)));
        }
    }

    private void submitJobRequestToQueue(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, Iterable<JobParam> rpcInputs, int priority) throws IOException {
    }

    // gRPC interface
    @Override
    public void buildService(BuildServiceParams buildServiceParams, StreamObserver<BuildServiceResponse> responseObserver) {
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
        return true;
    }

    private boolean isValidUri(String test) {
        try {
            return URI.create(test).getScheme() != null;
        } catch (Exception unused) {
            return false;
        }
    }

    private FtepWorkerBlockingStub getWorkerById(String workerId) {
        FtepWorkerBlockingStub existingWorkerStub = workerStubCache.get(workerId);
        if (existingWorkerStub != null) {
            return existingWorkerStub;
        } else {
            FtepWorkerBlockingStub newWorkerStub = workerFactory.getWorkerById(workerId);
            workerStubCache.put(workerId, newWorkerStub);
            return newWorkerStub;
        }
    }

    private List<String> explodeParallelInput(Collection<String> inputUris) {
        List<String> results = new ArrayList<>();
        for (String inputUri : inputUris) {
            if (inputUri.startsWith("ftep://databasket")) {
        //        Databasket dataBasket = getDatabasketFromUri(inputUri);
        //        results.addAll(dataBasket.getFiles().stream().map(f -> f.getUri().toString()).collect(toList()));
            } else if (inputUri.contains((","))) {
                results.addAll(Arrays.asList(inputUri.split(",")));
            } else {
                results.add(inputUri);
            }
        }
        return results;
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
        FtepWorkerBlockingStub worker = getWorkerById(workerId);
        FtepService service = job.getConfig().getService();
        LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(), service.getName());
        // Update GUI endpoint URL for client access
        if (service.getType() == FtepService.Type.APPLICATION) {
            String zooId = job.getExtId();
            String guiUrl = guiService.getGuiUrl(worker, GrpcUtil.toRpcJob(job));
            LOG.info("Updating GUI URL for job {} ({}): {}", zooId, job.getConfig().getService().getName(), guiUrl);
            job.setGuiUrl(guiUrl);
            jobDataService.save(job);
        }
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);
    }

    private void onContainerExit(Job job, String workerId, JobEnvironment jobEnvironment, int exitCode) throws Exception {
        FtepWorkerBlockingStub worker = getWorkerById(workerId);
        switch (exitCode) {
            case 0:
                // Normal exit
                break;
            case 137:
                LOG.info("Docker container for {} terminated via SIGKILL (exit code 137)",
                    job.getExtId());
                break;
            case 143:
                LOG.info("Docker container for {} terminated via SIGTERM (exit code 143)",
                    job.getExtId());
                break;
            default:
                throw new Exception("Docker container returned with exit code " + exitCode);
        }
        job.setStage(JobStep.OUTPUT_LIST.getText());
        job.setEndTime(LocalDateTime.now()); // End time is when processing ends
        job.setGuiUrl(null); // Any GUI services will no longer be available
        jobDataService.save(job);
        try {
            ingestOutput(job, GrpcUtil.toRpcJob(job), worker, jobEnvironment);
        } catch (Exception e) {
            throw new Exception("Error ingesting output for : " + e.getMessage());
        }
    }

    private void onJobError(Job job, String description) {
        LOG.error("Error in Job {}: {}", job.getExtId(), description);
        endJobWithError(job);
    }

    private void onJobError(Job job, Throwable t) {
        LOG.error("Error in Job " + job.getExtId(), t);
        endJobWithError(job);
    }

    private void endJobWithError(Job job) {
        job.setStatus(Job.Status.ERROR);
        job.setEndTime(LocalDateTime.now(ZoneOffset.UTC));
        jobDataService.save(job);
    }

    private void ingestOutput(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerBlockingStub worker, JobEnvironment jobEnvironment) throws IOException, Exception {
    }

    private void completeParentJob(Job parentJob) {
        if (allChildJobCompleted(parentJob)) {
            StreamObserver<FtepServiceResponse> responseObserver;

            // Must collect all child jobs, save for parent and send a response
            SetMultimap<String, FtepFile> jobOutputFiles = MultimapBuilder.hashKeys().hashSetValues().build();
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
            parentJob.setOutputs(jobOutputFiles.entries().stream().collect(toMultimap(
                e -> e.getKey(),
                e -> e.getValue().getUri().toString(),
                MultimapBuilder.hashKeys().hashSetValues()::build)));
            parentJob.setOutputFiles(ImmutableSet.copyOf(jobOutputFiles.values()));
            jobDataService.save(parentJob);
            List<JobParam> outputs = jobOutputFiles.asMap().entrySet().stream()
                .map(e -> JobParam.newBuilder().setParamName(e.getKey())
                    .addAllParamValue(e.getValue().stream().map(f -> f.getUri().toASCIIString()).collect(toSet()))
                    .build())
                .collect(toList());
            responseObserver = responseObservers.get(parentJob.getExtId());
            if (responseObserver != null) {
                responseObserver.onNext(FtepServiceResponse.newBuilder().setJobOutputs(FtepServiceResponse.JobOutputs.newBuilder().addAllOutputs(outputs).build()).build());
                responseObserver.onCompleted();
            } else {
                String description = "Failed to collect outputs? TODO fix this note!";
                LOG.error("Failed to run processor: {}; notifying gRPC client <2>", description);
                throw new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withDescription(description));
            }
        }
    }

    private boolean allChildJobCompleted(Job parentJob) {
        return !parentJob.getSubJobs().stream().anyMatch(j -> j.getStatus() != Job.Status.COMPLETED && j.getStatus() != Job.Status.ERROR);
    }

    private Map<String, String> listOutputFiles(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerGrpc.FtepWorkerBlockingStub worker, JobEnvironment jobEnvironment) {
        FtepService service = job.getConfig().getService();

        OutputFileList outputFileList = worker.listOutputFiles(ListOutputFilesParam.newBuilder()
                .setJob(rpcJob)
                .setOutputsRootPath(jobEnvironment.getOutputDir())
                .build());
        List<String> relativePaths = outputFileList.getItemsList().stream()
                .map(OutputFileItem::getRelativePath)
                .collect(Collectors.toList());

        Map<String, String> outputsByRelativePath;

        if (service.getType() == FtepService.Type.APPLICATION) {
            // Collect all files in the output directory with simple index IDs
            outputsByRelativePath = IntStream.range(0, relativePaths.size())
                    .boxed()
                    .collect(toMap(i -> Integer.toString(i + 1), relativePaths::get));
        } else {
            // Ensure we have one file per expected output
            Set<String> expectedServiceOutputIds = service.getServiceDescriptor().getDataOutputs().stream()
                    .map(FtepServiceDescriptor.Parameter::getId).collect(toSet());
            outputsByRelativePath = new HashMap<>(expectedServiceOutputIds.size());

            for (String expectedOutputId : expectedServiceOutputIds) {
                Optional<String> relativePath = relativePaths.stream()
                        .filter(path -> path.startsWith(expectedOutputId + "/"))
                        .reduce((a, b) -> null);
                if (relativePath.isPresent()) {
                    outputsByRelativePath.put(expectedOutputId, relativePath.get());
                } else {
                    try (CloseableThreadContext.Instance ctc = Logging.userLoggingContext()) {
                        LOG.info("Service defined output with ID '{}' but no matching directory was found in the job outputs", expectedOutputId);
                    }
                    throw new ServiceExecutionException(String.format("Did not find expected single output for '%s' in outputs list: %s", expectedOutputId, relativePaths));
                }
            }
        }

        return outputsByRelativePath;
    }

    private Map<String, FtepFile> repatriateAndIngestOutputFiles(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerGrpc.FtepWorkerBlockingStub worker, Multimap<String, String> inputs, JobEnvironment jobEnvironment, Map<String, String> outputsByRelativePath) throws IOException, InterruptedException {
        BiMap<OutputProductMetadata, Path> outputProducts = HashBiMap.create(outputsByRelativePath.size());
        Map<String, FtepFile> outputFtepFiles = new HashMap<>(outputsByRelativePath.size());

        for (Map.Entry<String, String> output : outputsByRelativePath.entrySet()) {
            String outputId = output.getKey();
            String relativePath = output.getValue();
            GetOutputFileParam getOutputFileParam = GetOutputFileParam.newBuilder()
                    .setJob(rpcJob)
                    .setPath(Paths.get(jobEnvironment.getOutputDir()).resolve(relativePath).toString())
                    .build();

            FtepWorkerGrpc.FtepWorkerStub asyncWorker = FtepWorkerGrpc.newStub(worker.getChannel());

            try (FileStreamClient<GetOutputFileParam> fileStreamClient = new FileStreamClient<GetOutputFileParam>() {
                private OutputProductMetadata outputProduct;

                @Override
                public OutputStream buildOutputStream(FileStream.FileMeta fileMeta) throws IOException {
                    try (CloseableThreadContext.Instance ctc = Logging.userLoggingContext()) {
                        LOG.info("Collecting output '{}' with filename {} ({} bytes)", outputId, fileMeta.getFilename(), fileMeta.getSize());
                    }

                    this.outputProduct = OutputProductMetadata.builder()
                            .owner(job.getOwner())
                            .service(job.getConfig().getService())
                            .outputId(outputId)
                            .jobId(job.getExtId())
                            .crs(Iterables.getOnlyElement(inputs.get("crs"), null))
                            .geometry(Iterables.getOnlyElement(inputs.get("aoi"), null))
                            .properties(new HashMap<>(ImmutableMap.<String, Object>builder()
                                    .put("jobId", job.getExtId())
                                    .put("intJobId", job.getId())
                                    .put("serviceName", job.getConfig().getService().getName())
                                    .put("jobOwner", job.getOwner().getName())
                                    .put("jobStartTime", job.getStartTime().atOffset(ZoneOffset.UTC).toString())
                                    .put("jobEndTime", job.getEndTime().atOffset(ZoneOffset.UTC).toString())
                                    .put("filename", fileMeta.getFilename())
                                    .build()))
                            .build();

                    setOutputPath(ftepFileRegistrar.prepareNewOutputProduct(outputProduct, fileMeta.getFilename()));
                    LOG.info("Writing output file for job {}: {}", job.getExtId(), getOutputPath());
                    return new BufferedOutputStream(Files.newOutputStream(getOutputPath(), CREATE, TRUNCATE_EXISTING, WRITE));
                }

                @Override
                public void onCompleted() {
                    LOG.info("Completed writing output file for job {}: {}", job.getExtId(), getOutputPath());
                    outputProducts.put(outputProduct, getOutputPath());
                }
            }) {
                asyncWorker.getOutputFile(getOutputFileParam, fileStreamClient.getFileStreamObserver());
                fileStreamClient.getLatch().await();
                LOG.info("Retrieved output for job {}: {}", job.getExtId(), output.getKey());
            }
        }

        try (CloseableThreadContext.Instance ctc = Logging.userLoggingContext()) {
            LOG.info("Retrieved all outputs for job {}: {}", job.getExtId(), outputsByRelativePath.keySet());
        }

        postProcessOutputProducts(outputProducts)
                .forEach((outputProduct, outputPath) -> outputFtepFiles.put(outputProduct.getOutputId(), ftepFileRegistrar.registerOutput(outputProduct, outputPath)));

        return outputFtepFiles;
    }

    private BiMap<OutputProductMetadata, Path> postProcessOutputProducts(BiMap<OutputProductMetadata, Path> outputProducts) throws IOException {
        // TODO Extract post-process possibilities to separate classes

        // Detect and zip up shapefiles
        Set<Path> shapefiles = outputProducts.values().stream()
                .filter(p -> MoreFiles.getFileExtension(p).equals("shp"))
                .collect(toSet());
        for (Path shapefile : shapefiles) {
            LOG.info("Processing detected shapefile: {}", shapefile);
            postProcessShapefile(shapefile, outputProducts);
        }

        // Try to read CRS/AOI from all files if not set by input parameters - note that CRS/AOI may still be null after this
        outputProducts.forEach((outputProduct, outputPath) -> {
            outputProduct.setCrs(Optional.ofNullable(outputProduct.getCrs()).orElseGet(() -> getOutputCrs(outputPath)));
            outputProduct.setGeometry(Optional.ofNullable(outputProduct.getGeometry()).orElseGet(() -> getOutputGeometry(outputPath)));
        });

        return outputProducts;
    }

    private void postProcessShapefile(Path shapefile, BiMap<OutputProductMetadata, Path> outputProducts) throws IOException {
        // Clone the metadata of the .shp
        OutputProductMetadata originalMetadata = outputProducts.inverse().get(shapefile);
        OutputProductMetadata shapefileMetadata = OutputProductMetadata.builder()
                .owner(originalMetadata.getOwner())
                .service(originalMetadata.getService())
                .outputId(originalMetadata.getOutputId())
                .jobId(originalMetadata.getJobId())
                .crs(Optional.ofNullable(originalMetadata.getCrs()).orElseGet(() -> getOutputCrs(shapefile)))
                .geometry(Optional.ofNullable(originalMetadata.getGeometry()).orElseGet(() -> getOutputGeometry(shapefile)))
                .properties(originalMetadata.getProperties())
                .build();

        Shapefile zippedShapefile = GeoUtil.zipShapeFile(shapefile, true);

        // Remove the individual shapefile sidecar files from the outputProducts list, then add the new zip
        zippedShapefile.getContents().forEach(f -> outputProducts.inverse().remove(f));
        outputProducts.put(shapefileMetadata, zippedShapefile.zip);
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

    /*@Override
    public void stopService(StopServiceParams request, StreamObserver<StopServiceResponse> responseObserver) {
        stopJob(request, responseObserver);
    }*/

    // gRPC interface
    @Override
    public void stopJob(StopServiceParams request, StreamObserver<StopServiceResponse> responseObserver) {
        com.cgi.eoss.ftep.rpc.Job rpcJob = request.getJob();

        try {
            FtepWorkerGrpc.FtepWorkerBlockingStub worker = Optional.ofNullable(jobWorkers.get(rpcJob)).orElseThrow(() -> new IllegalStateException("F-TEP worker not found for job " + rpcJob.getId()));
            LOG.info("Stop requested for job {}", rpcJob.getId());
            StopContainerResponse stopContainerResponse = worker.stopContainer(rpcJob);
            jobWorkers.remove(rpcJob);
            LOG.info("Successfully stopped job {}", rpcJob.getId());
            responseObserver.onNext(StopServiceResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to stop job {}; notifying gRPC client", rpcJob.getId(), e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
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

// ---

    private Map<String, FtepFile> executeJob(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, List<JobParam> rpcInputs, FtepWorkerGrpc.FtepWorkerBlockingStub worker) throws IOException, InterruptedException {
        String zooId = job.getExtId();
        String userId = job.getOwner().getName();
        FtepService service = job.getConfig().getService();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        // Create workspace and prepare inputs
        JobEnvironment jobEnvironment = prepareEnvironment(job, rpcJob, rpcInputs, worker);

        // Configure and launch the docker container
        launchContainer(job, rpcJob, worker, jobEnvironment);

        // TODO Implement async service command execution

        // Update GUI endpoint URL for client access
        if (service.getType() == FtepService.Type.APPLICATION) {
            String guiUrl = guiService.getGuiUrl(worker, rpcJob);
            LOG.info("Updating GUI URL for job {} ({}): {}", zooId, job.getConfig().getService().getName(), guiUrl);
            job.setGuiUrl(guiUrl);
            jobDataService.save(job);
        }

        // Wait for exit, with timeout if necessary
        waitForContainerExit(job, rpcJob, worker, inputs);

        // Enumerate files in the job output directory
        Map<String, String> outputsByRelativePath = listOutputFiles(job, rpcJob, worker, jobEnvironment);

        // Repatriate output files
        Map<String, FtepFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker, inputs, jobEnvironment, outputsByRelativePath);

        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(outputFiles.entrySet().stream().collect(toMultimap(
                e -> e.getKey(),
                e -> e.getValue().getUri().toString(),
                MultimapBuilder.hashKeys().hashSetValues()::build)));
        job.setOutputFiles(ImmutableSet.copyOf(outputFiles.values()));
        jobDataService.save(job);

        // Ensure output files inherit the ACEs of the job which created them
        securityService.setParentAcl(Job.class, job.getId(), FtepFile.class, job.getOutputFiles().stream().map(FtepFile::getId).collect(toSet()));

        if (service.getType() == FtepService.Type.BULK_PROCESSOR) {
            // Auto-publish the output files
            ImmutableSet.copyOf(outputFiles.values()).forEach(f -> securityService.publish(FtepFile.class, f.getId()));
        }

        return outputFiles;
    }

    private JobEnvironment prepareEnvironment(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, List<JobParam> rpcInputs, FtepWorkerGrpc.FtepWorkerBlockingStub worker) {
        LOG.info("Downloading input data for {}", job.getExtId());
        job.setStartTime(LocalDateTime.now(ZoneOffset.UTC));
        job.setStatus(Job.Status.RUNNING);
        job.setStage(JobStep.DATA_FETCH.getText());
        jobDataService.save(job);

        return worker.prepareEnvironment(JobInputs.newBuilder()
                .setJob(rpcJob)
                .addAllInputs(rpcInputs)
                .build());
    }

    private void launchContainer(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerGrpc.FtepWorkerBlockingStub worker, JobEnvironment jobEnvironment) {
        FtepService service = job.getConfig().getService();
        String dockerImageTag = service.getDockerTag();

        JobDockerConfig.Builder dockerConfigBuilder = JobDockerConfig.newBuilder()
                .setJob(rpcJob)
                .setServiceName(service.getName())
                .setDockerImage(dockerImageTag)
                .addBinds("/data:/data:ro")
                .addBinds(jobEnvironment.getWorkingDir() + "/FTEP-WPS-INPUT.properties:" + "/home/worker/workDir/FTEP-WPS-INPUT.properties:ro")
                .addBinds(jobEnvironment.getInputDir() + ":" + "/home/worker/workDir/inDir:ro")
                .addBinds(jobEnvironment.getOutputDir() + ":" + "/home/worker/workDir/outDir:rw")
                .addBinds(jobEnvironment.getTempDir() + ":" + "/home/worker/procDir:rw");

        if (service.getType() == FtepService.Type.APPLICATION) {
            dockerConfigBuilder.addPorts(FtepGuiServiceManager.GUACAMOLE_PORT);
        }

        LOG.info("Launching docker container for job {}", job.getExtId());
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);
        LaunchContainerResponse launchContainerResponse = worker.launchContainer(dockerConfigBuilder.build());
        LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(), service.getName());
    }

    private void waitForContainerExit(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerGrpc.FtepWorkerBlockingStub worker, Multimap<String, String> inputs) {
        ContainerExitCode exitCode;
        if (inputs.containsKey(TIMEOUT_PARAM)) {
            int timeout = Integer.parseInt(Iterables.getOnlyElement(inputs.get(TIMEOUT_PARAM)));
            exitCode = worker.waitForContainerExitWithTimeout(ExitWithTimeoutParams.newBuilder().setJob(rpcJob).setTimeout(timeout).build());
        } else {
            exitCode = worker.waitForContainerExit(ExitParams.newBuilder().setJob(rpcJob).build());
        }

        switch (exitCode.getExitCode()) {
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
                throw new ServiceExecutionException("Docker container returned with exit code " + exitCode);
        }

        job.setStage(JobStep.OUTPUT_LIST.getText());
        job.setEndTime(LocalDateTime.now(ZoneOffset.UTC)); // End time is when processing ends
        job.setGuiUrl(null); // Any GUI services will no longer be available
        jobDataService.save(job);
    }
}
