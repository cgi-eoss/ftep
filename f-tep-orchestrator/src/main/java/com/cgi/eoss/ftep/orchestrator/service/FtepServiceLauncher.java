package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.costing.CostingService;
import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.rpc.FtepServiceLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.FtepServiceResponse;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.JobParam;
import com.cgi.eoss.ftep.rpc.ListWorkersParams;
import com.cgi.eoss.ftep.rpc.WorkersList;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.ftep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.LaunchContainerResponse;
import com.cgi.eoss.ftep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.ftep.rpc.worker.OutputFileItem;
import com.cgi.eoss.ftep.rpc.worker.OutputFileList;
import com.cgi.eoss.ftep.rpc.worker.OutputFileResponse;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * <p>Primary entry point for WPS services to launch in F-TEP.</p> <p>Provides access to F-TEP data services and job
 * distribution capability.</p>
 */
@Service
@Log4j2
@GRpcService
public class FtepServiceLauncher extends FtepServiceLauncherGrpc.FtepServiceLauncherImplBase {

    private static final String TIMEOUT_PARAM = "timeout";

    // TODO Synchronise this list at startup from workers
    private final Map<com.cgi.eoss.ftep.rpc.Job, FtepWorkerGrpc.FtepWorkerBlockingStub> jobWorkers = new HashMap<>();

    private final WorkerFactory workerFactory;
    private final JobDataService jobDataService;
    private final FtepGuiServiceManager guiService;
    private final CatalogueService catalogueService;
    private final CostingService costingService;
    private final FtepSecurityService securityService;

    @Autowired
    public FtepServiceLauncher(WorkerFactory workerFactory, JobDataService jobDataService, FtepGuiServiceManager guiService, CatalogueService catalogueService, CostingService costingService, FtepSecurityService securityService) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.guiService = guiService;
        this.catalogueService = catalogueService;
        this.costingService = costingService;
        this.securityService = securityService;
    }

    @Override
    public void launchService(FtepServiceParams request, StreamObserver<FtepServiceResponse> responseObserver) {
        String zooId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();
        String jobConfigLabel = request.getJobConfigLabel();
        List<JobParam> rpcInputs = request.getInputsList();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        Job job = null;
        com.cgi.eoss.ftep.rpc.Job rpcJob = null;
        try (CloseableThreadContext.Instance ctc = CloseableThreadContext.push("F-TEP Service Orchestrator")
                .put("userId", userId).put("serviceId", serviceId).put("zooId", zooId)) {
            // TODO Allow re-use of existing JobConfig
            job = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, inputs);
            rpcJob = createRpcJob(job);

            // Post back the job metadata for async responses
            responseObserver.onNext(FtepServiceResponse.newBuilder().setJob(rpcJob).build());

            ctc.put("jobId", String.valueOf(job.getId()));
            FtepService service = job.getConfig().getService();

            checkCost(job.getOwner(), job.getConfig());

            if (!checkInputs(job.getOwner(), request.getInputsList())) {
                try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                    LOG.error("User does not have read access to all requested inputs", userId);
                }
                throw new ServiceExecutionException("User does not have read access to all requested inputs");
            }

            // TODO Determine WorkerEnvironment from service parameters
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
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (job != null) {
                job.setStatus(Job.Status.ERROR);
                job.setEndTime(LocalDateTime.now());
                jobDataService.save(job);
            }

            LOG.error("Failed to run processor; notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        } finally {
            Optional.ofNullable(rpcJob).ifPresent(jobWorkers::remove);
        }
    }

    @Override
    public void listWorkers(ListWorkersParams request, StreamObserver<WorkersList> responseObserver) {
        try {
            responseObserver.onNext(workerFactory.listWorkers());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to enumerate workers", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }

    private com.cgi.eoss.ftep.rpc.Job createRpcJob(Job job) {
        return com.cgi.eoss.ftep.rpc.Job.newBuilder()
                .setId(job.getExtId())
                .setIntJobId(String.valueOf(job.getId()))
                .setUserId(job.getOwner().getName())
                .setServiceId(job.getConfig().getService().getName())
                .build();
    }

    private void checkCost(User user, JobConfig jobConfig) {
        int estimatedCost = costingService.estimateJobCost(jobConfig);
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException("Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
        // TODO Should estimated balance be "locked" in the wallet?
    }

    private boolean checkInputs(User user, List<JobParam> inputsList) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(inputsList);

        Set<URI> inputUris = inputs.entries().stream()
                .filter(e -> this.isValidUri(e.getValue()))
                .flatMap(e -> Arrays.stream(StringUtils.split(e.getValue(), ',')).map(URI::create))
                .collect(Collectors.toSet());

        return inputUris.stream().allMatch(uri -> catalogueService.canUserRead(user, uri));
    }

    private boolean isValidUri(String test) {
        try {
            return URI.create(test).getScheme() != null;
        } catch (Exception unused) {
            return false;
        }
    }

    private Map<String, FtepFile> executeJob(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, List<JobParam> rpcInputs, FtepWorkerGrpc.FtepWorkerBlockingStub worker) throws IOException {
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

        if (service.getType() == FtepService.Type.BULK_PROCESSOR) {
            // Auto-publish the output files
            ImmutableSet.copyOf(outputFiles.values()).forEach(f -> securityService.publish(FtepFile.class, f.getId()));
        }

        return outputFiles;
    }

    private JobEnvironment prepareEnvironment(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, List<JobParam> rpcInputs, FtepWorkerGrpc.FtepWorkerBlockingStub worker) {
        LOG.info("Downloading input data for {}", job.getExtId());
        job.setStartTime(LocalDateTime.now());
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
                .addBinds(jobEnvironment.getOutputDir() + ":" + "/home/worker/workDir/outDir:rw");

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
        job.setEndTime(LocalDateTime.now()); // End time is when processing ends
        job.setGuiUrl(null); // Any GUI services will no longer be available
        jobDataService.save(job);
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
                    throw new ServiceExecutionException(String.format("Did not find expected single output for '%s' in outputs list: %s", expectedOutputId, relativePaths));
                }
            }
        }

        return outputsByRelativePath;
    }

    private Map<String, FtepFile> repatriateAndIngestOutputFiles(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerGrpc.FtepWorkerBlockingStub worker, Multimap<String, String> inputs, JobEnvironment jobEnvironment, Map<String, String> outputsByRelativePath) throws IOException {
        Map<String, FtepFile> outputFiles = new HashMap<>(outputsByRelativePath.size());

        for (Map.Entry<String, String> output : outputsByRelativePath.entrySet()) {
            String outputId = output.getKey();
            String relativePath = output.getValue();

            Iterator<OutputFileResponse> outputFile = worker.getOutputFile(GetOutputFileParam.newBuilder()
                    .setJob(rpcJob)
                    .setPath(Paths.get(jobEnvironment.getOutputDir()).resolve(relativePath).toString())
                    .build());

            // First message is the file metadata
            OutputFileResponse.FileMeta fileMeta = outputFile.next().getMeta();
            LOG.info("Collecting output '{}' with filename {} ({} bytes)", outputId, fileMeta.getFilename(), fileMeta.getSize());

            OutputProductMetadata outputProduct = OutputProductMetadata.builder()
                    .owner(job.getOwner())
                    .service(job.getConfig().getService())
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

            // TODO Configure whether files need to be transferred via RPC or simply copied
            Path outputPath = catalogueService.provisionNewOutputProduct(outputProduct, fileMeta.getFilename());
            LOG.info("Writing output file for job {}: {}", job.getExtId(), outputPath);
            try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputPath, CREATE, TRUNCATE_EXISTING, WRITE))) {
                outputFile.forEachRemaining(Unchecked.consumer(of -> of.getChunk().getData().writeTo(outputStream)));
            }

            outputFiles.put(outputId, catalogueService.ingestOutputProduct(outputProduct, outputPath));
        }

        return outputFiles;
    }

    private void chargeUser(User user, Job job) {
        costingService.chargeForJob(user.getWallet(), job);
    }

}
