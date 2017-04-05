package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.catalogue.external.ExternalProductDataService;
import com.cgi.eoss.ftep.catalogue.files.OutputProductService;
import com.cgi.eoss.ftep.catalogue.files.ReferenceDataService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.model.internal.ReferenceDataMetadata;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.ftep.rpc.catalogue.FileResponse;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFileUri;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.geojson.GeoJsonObject;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@GRpcService
@Log4j2
public class CatalogueServiceImpl extends CatalogueServiceGrpc.CatalogueServiceImplBase implements CatalogueService {

    private static final int FILE_STREAM_CHUNK_BYTES = 8192;

    private final FtepFileDataService ftepFileDataService;
    private final OutputProductService outputProductService;
    private final ReferenceDataService referenceDataService;
    private final ExternalProductDataService externalProductDataService;

    @Autowired
    public CatalogueServiceImpl(FtepFileDataService ftepFileDataService, OutputProductService outputProductService, ReferenceDataService referenceDataService, ExternalProductDataService externalProductDataService) {
        this.ftepFileDataService = ftepFileDataService;
        this.outputProductService = outputProductService;
        this.referenceDataService = referenceDataService;
        this.externalProductDataService = externalProductDataService;
    }

    @Override
    public FtepFile ingestReferenceData(ReferenceDataMetadata referenceData, MultipartFile file) throws IOException {
        FtepFile ftepFile = referenceDataService.ingest(referenceData.getOwner(), referenceData.getFilename(), referenceData.getGeometry(), referenceData.getProperties(), file);
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
        return ftepFileDataService.save(ftepFile);
    }

    @Override
    public FtepFile indexExternalProduct(GeoJsonObject geoJson) {
        // This will return an already-persistent object
        return externalProductDataService.ingest(geoJson);
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
    public void downloadFtepFile(FtepFileUri request, StreamObserver<FileResponse> responseObserver) {
        try {
            FtepFile file = ftepFileDataService.getByUri(request.getUri());
            Resource fileResource = getAsResource(file);

            // First message is the metadata
            FileResponse.FileMeta fileMeta = FileResponse.FileMeta.newBuilder()
                    .setFilename(fileResource.getFilename())
                    .setSize(fileResource.contentLength())
                    .build();
            responseObserver.onNext(FileResponse.newBuilder().setMeta(fileMeta).build());

            // Then read the file, chunked at 8kB
            LocalDateTime startTime = LocalDateTime.now();
            try (ReadableByteChannel channel = Channels.newChannel(fileResource.getInputStream())) {
                ByteBuffer buffer = ByteBuffer.allocate(FILE_STREAM_CHUNK_BYTES);
                int position = 0;
                while (channel.read(buffer) > 0) {
                    int size = buffer.position();
                    buffer.rewind();
                    responseObserver.onNext(FileResponse.newBuilder().setChunk(FileResponse.Chunk.newBuilder()
                            .setPosition(position)
                            .setData(ByteString.copyFrom(buffer, size))
                            .build()).build());
                    position += buffer.position();
                    buffer.flip();
                }
            }
            LOG.info("Transferred FtepFile {} ({} bytes) in {}", fileResource.getFilename(), fileResource.contentLength(), Duration.between(startTime, LocalDateTime.now()));

            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to serve file download for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }
}
