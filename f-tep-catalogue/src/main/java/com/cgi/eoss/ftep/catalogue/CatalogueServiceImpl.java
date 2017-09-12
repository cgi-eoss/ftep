package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.catalogue.external.ExternalProductDataService;
import com.cgi.eoss.ftep.catalogue.files.OutputProductService;
import com.cgi.eoss.ftep.catalogue.files.ReferenceDataService;
import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.model.DataSource;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.model.internal.ReferenceDataMetadata;
import com.cgi.eoss.ftep.persistence.service.DataSourceDataService;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.rpc.FileStream;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.ftep.rpc.catalogue.DatabasketContents;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFileUri;
import com.cgi.eoss.ftep.rpc.catalogue.UriDataSourcePolicies;
import com.cgi.eoss.ftep.rpc.catalogue.UriDataSourcePolicy;
import com.cgi.eoss.ftep.rpc.catalogue.Uris;
import com.cgi.eoss.ftep.security.FtepPermission;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.base.Stopwatch;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.apache.logging.log4j.CloseableThreadContext;
import org.geojson.GeoJsonObject;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@GRpcService
@Log4j2
public class CatalogueServiceImpl extends CatalogueServiceGrpc.CatalogueServiceImplBase implements CatalogueService {

    private static final int FILE_STREAM_CHUNK_BYTES = 8192;

    private final FtepFileDataService ftepFileDataService;
    private final DataSourceDataService dataSourceDataService;
    private final DatabasketDataService databasketDataService;
    private final OutputProductService outputProductService;
    private final ReferenceDataService referenceDataService;
    private final ExternalProductDataService externalProductDataService;
    private final FtepSecurityService securityService;

    @Autowired
    public CatalogueServiceImpl(FtepFileDataService ftepFileDataService, DataSourceDataService dataSourceDataService, DatabasketDataService databasketDataService, OutputProductService outputProductService, ReferenceDataService referenceDataService, ExternalProductDataService externalProductDataService, FtepSecurityService securityService) {
        this.ftepFileDataService = ftepFileDataService;
        this.dataSourceDataService = dataSourceDataService;
        this.databasketDataService = databasketDataService;
        this.outputProductService = outputProductService;
        this.referenceDataService = referenceDataService;
        this.externalProductDataService = externalProductDataService;
        this.securityService = securityService;
    }

    @Override
    public FtepFile ingestReferenceData(ReferenceDataMetadata referenceData, MultipartFile file) throws IOException {
        FtepFile ftepFile = referenceDataService.ingest(referenceData.getOwner(), referenceData.getFilename(), referenceData.getGeometry(), referenceData.getProperties(), file);
        ftepFile.setDataSource(dataSourceDataService.getForRefData(ftepFile));
        return ftepFileDataService.save(ftepFile);
    }

    @Override
    public Path provisionNewOutputProduct(OutputProductMetadata outputProduct, String filename) throws IOException {
        return outputProductService.provision(outputProduct.getJobId(), filename);
    }

    @Override
    public FtepFile ingestOutputProduct(OutputProductMetadata outputProduct, Path path) throws IOException {
        FtepFile ftepFile = outputProductService.ingest(
                outputProduct.getOwner(),
                outputProduct.getJobId(),
                outputProduct.getCrs(),
                outputProduct.getGeometry(),
                outputProduct.getProperties(),
                path);
        ftepFile.setDataSource(dataSourceDataService.getForService(outputProduct.getService()));
        return ftepFileDataService.save(ftepFile);
    }

    @Override
    public FtepFile indexExternalProduct(GeoJsonObject geoJson) {
        // This will return an already-persistent object
        FtepFile ftepFile = externalProductDataService.ingest(geoJson);
        ftepFile.setDataSource(dataSourceDataService.getForExternalProduct(ftepFile));
        return ftepFile;
    }

    @Override
    public Resource getAsResource(FtepFile file) {
        switch (file.getType()) {
            case REFERENCE_DATA:
                return referenceDataService.resolve(file);
            case OUTPUT_PRODUCT:
                return outputProductService.resolve(file);
            case EXTERNAL_PRODUCT:
                return externalProductDataService.resolve(file);
            default:
                throw new UnsupportedOperationException("Unable to materialise FtepFile: " + file);
        }
    }

    @Override
    public void delete(FtepFile file) throws IOException {
        switch (file.getType()) {
            case REFERENCE_DATA:
                referenceDataService.delete(file);
                break;
            case OUTPUT_PRODUCT:
                outputProductService.delete(file);
                break;
            case EXTERNAL_PRODUCT:
                externalProductDataService.delete(file);
                break;
        }
        ftepFileDataService.delete(file);
    }

    @Override
    public HttpUrl getWmsUrl(FtepFile.Type type, URI uri) {
        switch (type) {
            case OUTPUT_PRODUCT:
                // TODO Use the CatalogueUri pattern to determine file attributes
                String[] pathComponents = uri.getPath().split("/");
                String jobId = pathComponents[1];
                String filename = pathComponents[2];
                return outputProductService.getWmsUrl(jobId, filename);
            default:
                return null;
        }
    }

    @Override
    public boolean canUserRead(User user, URI uri) {
        if (uri.getScheme().equals("ftep") && uri.getHost().equals("databasket")) {
            Databasket databasket = getDatabasketFromUri(uri.toASCIIString());

            if (!securityService.hasUserPermission(user, FtepPermission.READ, Databasket.class, databasket.getId())) {
                logAccessFailure(uri);
                return false;
            }

            return databasket.getFiles().stream().allMatch(ftepFile -> canUserRead(user, ftepFile.getUri()));
        } else {
            FtepFile ftepFile = ftepFileDataService.getByUri(uri);

            if (ftepFile != null) {
                if (!securityService.hasUserPermission(user, FtepPermission.READ, FtepFile.class, ftepFile.getId())) {
                    logAccessFailure(ftepFile.getUri());
                    return false;
                }
            }

            return true;
        }
    }

    private void logAccessFailure(URI uri) {
        try (CloseableThreadContext.Instance ctc = Logging.userLoggingContext()) {
            LOG.info("Access denied to F-TEP resource: {}", uri);
        }
    }

    @Override
    public void downloadFtepFile(FtepFileUri request, StreamObserver<FileStream> responseObserver) {
        try {
            FtepFile file = ftepFileDataService.getByUri(request.getUri());
            Resource fileResource = getAsResource(file);

            Stopwatch stopwatch = Stopwatch.createStarted();
            try (ReadableByteChannel channel = Channels.newChannel(fileResource.getInputStream())) {
                GrpcUtil.streamFile(responseObserver,
                        fileResource.getFilename(),
                        fileResource.contentLength(),
                        channel
                );
            }
            LOG.info("Transferred FtepFile {} ({} bytes) in {}", fileResource.getFilename(), fileResource.contentLength(), stopwatch.stop().elapsed());

            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to serve file download for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    @Override
    public void getDatabasketContents(com.cgi.eoss.ftep.rpc.catalogue.Databasket request, StreamObserver<DatabasketContents> responseObserver) {
        try {
            // TODO Extract databasket ID from CatalogueUri pattern
            Databasket databasket = getDatabasketFromUri(request.getUri());

            DatabasketContents.Builder responseBuilder = DatabasketContents.newBuilder();
            databasket.getFiles().forEach(f -> responseBuilder.addFiles(
                    com.cgi.eoss.ftep.rpc.catalogue.FtepFile.newBuilder()
                            .setFilename(f.getFilename())
                            .setUri(FtepFileUri.newBuilder().setUri(f.getUri().toASCIIString()).build())
                            .build()
                    )
            );

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list databasket contents for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    @Override
    public void getDataSourcePolicies(Uris request, StreamObserver<UriDataSourcePolicies> responseObserver) {
        try {
            UriDataSourcePolicies.Builder responseBuilder = UriDataSourcePolicies.newBuilder();

            for (FtepFileUri fileUri : request.getFileUrisList()) {
                FtepFile ftepFile = ftepFileDataService.getByUri(fileUri.getUri());
                DataSource dataSource;

                if (ftepFile != null) {
                    dataSource = ftepFile.getDataSource() != null ? ftepFile.getDataSource() :
                            dataSourceDataService.getByName(URI.create(fileUri.getUri()).getScheme());
                } else {
                    dataSource = dataSourceDataService.getByName(URI.create(fileUri.getUri()).getScheme());
                }

                LOG.debug("Inferred DataSource {} from FtepFile: {}", dataSource, fileUri.getUri());

                // Default to CACHE mode
                DataSource.Policy policy = dataSource != null ? dataSource.getPolicy() : DataSource.Policy.CACHE;

                responseBuilder.addPolicies(UriDataSourcePolicy.newBuilder()
                        .setUri(fileUri)
                        .setPolicy(UriDataSourcePolicy.Policy.valueOf(policy.toString()))
                        .build());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list datasource access policies contents for {}", request.getFileUrisList(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    private Databasket getDatabasketFromUri(String uri) {
        Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(uri);
        if (!uriIdMatcher.matches()) {
            throw new CatalogueException("Failed to load databasket for URI: " + uri);
        }
        Long databasketId = Long.parseLong(uriIdMatcher.group(1));
        Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId)).orElseThrow(() -> new CatalogueException("Failed to load databasket for ID " + databasketId));
        LOG.debug("Listing databasket contents for id {}", databasketId);
        return databasket;
    }

}
