package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.FtepServiceDockerBuildInfo;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.internal.OutputFileMetadata;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.model.internal.Pair;
import com.cgi.eoss.ftep.model.internal.RetrievedOutputFile;
import com.cgi.eoss.ftep.model.internal.Shapefile;
import com.cgi.eoss.ftep.orchestrator.service.gui.GuiUrlService;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.rpc.FileStream;
import com.cgi.eoss.ftep.rpc.FileStreamClient;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.worker.ContainerExit;
import com.cgi.eoss.ftep.rpc.worker.DockerImageBuildEvent;
import com.cgi.eoss.ftep.rpc.worker.DockerImageBuildEventType;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobError;
import com.cgi.eoss.ftep.rpc.worker.JobEvent;
import com.cgi.eoss.ftep.rpc.worker.JobEventType;
import com.cgi.eoss.ftep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.ftep.rpc.worker.OutputFileItem;
import com.cgi.eoss.ftep.rpc.worker.OutputFileList;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.io.MoreFiles;
import io.grpc.StatusException;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

@Log4j2
@Service
public class FtepOrchestratorService {

    private final ServiceDataService serviceDataService;
    private final JobDataService jobDataService;
    private final GuiUrlService guiUrlService;
    private final WorkerFactory workerFactory;
    private final FtepFileRegistrar ftepFileRegistrar;
    private final PlatformTransactionManager platformTransactionManager;

    @Autowired
    public FtepOrchestratorService(ServiceDataService serviceDataService,
                                   JobDataService jobDataService,
                                   GuiUrlService guiUrlService,
                                   WorkerFactory workerFactory,
                                   FtepFileRegistrar ftepFileRegistrar,
                                   PlatformTransactionManager platformTransactionManager) {
        this.serviceDataService = serviceDataService;
        this.jobDataService = jobDataService;
        this.guiUrlService = guiUrlService;
        this.workerFactory = workerFactory;
        this.ftepFileRegistrar = ftepFileRegistrar;
        this.platformTransactionManager = platformTransactionManager;
    }

    @Transactional
    public void updateBuildStatus(String serviceName, String buildFingerprint, DockerImageBuildEvent event) {
        try (CloseableThreadContext.Instance ctc = Logging.imageBuildLoggingContext(serviceName, buildFingerprint)) {
            LOG.trace("Updating Docker image build status: {}", serviceName);
            FtepService service = serviceDataService.getByName(serviceName);
            if (service.getDockerBuildInfo().getDockerBuildStatus().equals(FtepServiceDockerBuildInfo.Status.CANCELLED)) {
                // Do not forward updates for cancelled builds
                return;
            }
            DockerImageBuildEventType dockerImageBuildEventType = event.getDockerImageBuildEventType();
            service.getDockerBuildInfo().setLastBuiltFingerprint(buildFingerprint);
            switch (dockerImageBuildEventType) {
                case BUILD_IN_PROCESS:
                    service.getDockerBuildInfo().setDockerBuildStatus(FtepServiceDockerBuildInfo.Status.IN_PROCESS);
                    break;
                case BUILD_COMPLETED:
                    service.getDockerBuildInfo().setDockerBuildStatus(FtepServiceDockerBuildInfo.Status.COMPLETED);
                    break;
                case BUILD_FAILED:
                    service.getDockerBuildInfo().setDockerBuildStatus(FtepServiceDockerBuildInfo.Status.FAILED);
                    break;
            }
            serviceDataService.save(service);
        }
    }

    @Transactional
    public void processJobEvent(String workerId, String jobId, String internalJobId, JobEvent jobEvent) {
        LOG.trace("Processing job event: {}", jobId);
        Job job = jobDataService.reload(Long.parseLong(internalJobId));
        try (CloseableThreadContext.Instance ctc = Logging.jobLoggingContext(job.getExtId(), String.valueOf(job.getId()), job.getOwner().getName(), job.getConfig().getService().getName())) {
            JobEventType jobEventType = jobEvent.getJobEventType();
            switch (jobEventType) {
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
        }
    }

    private void onJobDataFetchingStarted(Job job, String workerId) {
        Logging.withUserLoggingContext(() -> LOG.info("Downloading input data for job {} (UUID {})", job.getId(), job.getExtId()));
        job.setWorkerId(workerId);
        job.setStartTime(LocalDateTime.now(ZoneOffset.UTC));
        job.setStatus(Job.Status.RUNNING);
        job.setStage(JobStep.DATA_FETCH.getText());

        // We start a new maximally-isolated transaction here as our JPA
        // configuration gets tangled and can sometimes lead to deadlocks in
        // postgres.
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobDataService.save(job);
            }
        });
    }

    private void onJobProcessingStarted(Job job, String workerId) {
        FtepService service = job.getConfig().getService();
        Logging.withUserLoggingContext(() -> LOG.info("Processing started for job {} (UUID {}), service {}", job.getId(), job.getExtId(), service.getName()));
        // Update GUI endpoint URL for client access
        if (service.getType() == FtepService.Type.APPLICATION) {
            String zooId = job.getExtId();
            String guiEndpoint = guiUrlService.getBackendEndpoint(workerId, GrpcUtil.toRpcJob(job), service.getApplicationPort());
            String guiUrl = guiUrlService.buildGuiUrl(workerId, GrpcUtil.toRpcJob(job), service.getApplicationPort());
            LOG.info("Updating GUI URL for job {} ({}): {}", zooId, job.getConfig().getService().getName(), guiUrl);
            job.setGuiEndpoint(guiEndpoint);
            job.setGuiUrl(guiUrl);
            jobDataService.save(job);
            guiUrlService.update();
        }
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);
    }

    @Transactional
    public void processContainerExit(String workerId, String jobId, String internalJobId, ContainerExit containerExit) {
        LOG.trace("Processing container exit event: {}", jobId);
        Job job = jobDataService.reload(Long.parseLong(internalJobId));
        try (CloseableThreadContext.Instance ctc = Logging.jobLoggingContext(job.getExtId(), String.valueOf(job.getId()), job.getOwner().getName(), job.getConfig().getService().getName())) {
            try {
                onContainerExit(job, workerId, containerExit.getJobEnvironment(), containerExit.getExitCode());
            } catch (Exception e) {
                endJobWithError(job, e);
            }
        }

    }

    private void onContainerExit(Job job, String workerId, JobEnvironment jobEnvironment, int exitCode) throws Exception {
        switch (exitCode) {
            case 0:
                Logging.withUserLoggingContext(() -> LOG.info("Docker container terminated normally (exit code 0) for job {} (UUID {})", job.getId(), job.getExtId()));
                break;
            case 137:
                Logging.withUserLoggingContext(() -> LOG.info("Docker container terminated via SIGKILL (exit code 137) for job {} (UUID {})", job.getId(), job.getExtId()));
                break;
            case 143:
                Logging.withUserLoggingContext(() -> LOG.info("Docker container terminated via SIGTERM (exit code 143) for job {} (UUID {})", job.getId(), job.getExtId()));
            default:
                Logging.withUserLoggingContext(() -> LOG.error("Docker container terminated abnormally (exit code {}) for job {} (UUID {})", exitCode, job.getId(), job.getExtId()));
                throw new Exception("Docker container returned with exit code " + exitCode);
        }
        job.setStage(JobStep.OUTPUT_LIST.getText());
        job.setEndTime(LocalDateTime.now(ZoneOffset.UTC)); // End time is when processing ends
        job.setGuiUrl(null); // Any GUI services will no longer be available
        job.setGuiEndpoint(null); // Any GUI services will no longer be available
        jobDataService.save(job);
        try {
            ingestOutput(job, GrpcUtil.toRpcJob(job), workerFactory.getWorkerById(workerId), jobEnvironment);
        } catch (IOException e) {
            throw new Exception("Error ingesting output for : " + e.getMessage());
        }
    }

    private void ingestOutput(Job job, com.cgi.eoss.ftep.rpc.Job rpcJob, FtepWorkerGrpc.FtepWorkerBlockingStub worker, JobEnvironment jobEnvironment) throws IOException, InterruptedException, StatusException {
        Logging.withUserLoggingContext(() -> LOG.info("Ingesting outputs for job {} (UUID {})", job.getId(), job.getExtId()));
        // Enumerate files in the job output directory
        Multimap<String, String> outputsByRelativePath = listOutputFiles(job, rpcJob, worker, jobEnvironment);
        // Repatriate output files
        SetMultimap<String, FtepFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker, job.getConfig().getInputs(), jobEnvironment, outputsByRelativePath);
        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(outputFiles.entries().stream().collect(toMultimap(e -> e.getKey(), e -> e.getValue().getUri().toString(), MultimapBuilder.hashKeys().hashSetValues()::build)));
        job.setOutputFiles(new HashSet<>(outputFiles.values()));
        jobDataService.save(job);

        Job parentJob = job.getParentJob();
        if (parentJob != null) {
            completeParentJob(parentJob);
        }
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
                    Logging.withUserLoggingContext(() -> {
                        if (expectedOutput.getMinOccurs() == 0) {
                            LOG.info("Service defined optional output with ID '{}' which was not found in the job outputs", expectedOutput.getId());
                        } else {
                            LOG.info("Service defined required output with ID '{}' which was not found in the job outputs", expectedOutput.getId());
                            throw new ServiceExecutionException(String.format("Did not find expected single output for '%s' in outputs list: %s", expectedOutput.getId(), relativePaths));
                        }
                    });
                } else if (expectedOutput.getMinOccurs() <= relativePathValues.size() && relativePathValues.size() <= expectedOutput.getMaxOccurs()) {
                    LOG.debug("Found acceptable number of output files for ID {} (min {}, max {})", expectedOutput.getId(), expectedOutput.getMinOccurs(), expectedOutput.getMaxOccurs());
                    outputsByRelativePath.putAll(expectedOutput.getId(), relativePathValues);
                } else {
                    Logging.withUserLoggingContext(() -> {
                        LOG.info("Service defined output with ID '{}' with between {} and {} files, but found {}", expectedOutput.getId(), expectedOutput.getMinOccurs(), expectedOutput.getMaxOccurs(), relativePathValues.size());
                        throw new ServiceExecutionException(String.format("Did not find acceptable number of outputs for '%s' in outputs list: %s", expectedOutput.getId(), relativePaths));
                    });
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

    private void completeParentJob(Job parentJob) throws StatusException {
        if (allChildJobCompleted(parentJob)) {

            // Must collect all child jobs, save for parent and send a response
            SetMultimap<String, FtepFile> jobOutputFiles = HashMultimap.create();
            // Only collect outputs from SUCCESSFUL subjobs
            for (Job subJob : parentJob.getSubJobs()) {
                if (subJob.getStatus().equals(Job.Status.COMPLETED)) {
                    subJob.getOutputs().forEach((k, v) -> subJob.getOutputFiles().stream().filter(x -> x.getUri().toString().equals(v)).findFirst().ifPresent(match -> jobOutputFiles.put(k, match)));
                }
            }

            // Wrap up the parent job
            parentJob.setStatus(Job.Status.COMPLETED);
            parentJob.setStage(JobStep.OUTPUT_LIST.getText());
            parentJob.setEndTime(LocalDateTime.now(ZoneOffset.UTC));
            parentJob.setGuiUrl(null);
            parentJob.setGuiEndpoint(null);
            parentJob.setOutputs(jobOutputFiles.entries().stream().collect(toMultimap(e -> e.getKey(), e -> e.getValue().getUri().toString(), HashMultimap::create)));
            parentJob.setOutputFiles(new HashSet<>(jobOutputFiles.values()));
            jobDataService.save(parentJob);
        }
        // TODO shall an error be sent in case of any subjobs error?
    }

    private boolean allChildJobCompleted(Job parentJob) {
        return parentJob.getSubJobs().stream().noneMatch(j -> j.getStatus() != Job.Status.COMPLETED && j.getStatus() != Job.Status.ERROR);
    }

    @Transactional
    public void processJobError(String workerId, String jobId, String internalJobId, JobError jobError) {
        processJobError(workerId, jobId, internalJobId, new ServiceExecutionException(jobError.getErrorDescription()));
    }

    @Transactional
    public void processJobError(String workerId, String jobId, String internalJobId, Exception exception) {
        LOG.trace("Processing job error: {}", jobId);
        Job job = jobDataService.reload(Long.parseLong(internalJobId));
        try (CloseableThreadContext.Instance ctc = Logging.jobLoggingContext(job.getExtId(), String.valueOf(job.getId()), job.getOwner().getName(), job.getConfig().getService().getName())) {
            endJobWithError(job, exception);
        }
    }

    private void endJobWithError(Job job, Throwable cause) {
        LOG.error("Error in Job {}", job.getExtId(), cause);
        job.setStatus(Job.Status.ERROR);
        job.setEndTime(LocalDateTime.now(ZoneOffset.UTC));
        jobDataService.save(job);
    }

}
