package com.cgi.eoss.ftep.io;

import com.cgi.eoss.ftep.io.download.DownloaderFacade;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFileUri;
import com.cgi.eoss.ftep.rpc.catalogue.UriDataSourcePolicies;
import com.cgi.eoss.ftep.rpc.catalogue.UriDataSourcePolicy;
import com.cgi.eoss.ftep.rpc.catalogue.Uris;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.MoreFiles;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.text.StrSubstitutor;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class ServiceInputOutputManagerImpl implements ServiceInputOutputManager {

    private static final String FTEP_SERVICE_CONTEXT = "ftep://serviceContext/${serviceName}";

    private final FtepServerClient ftepServerClient;
    private final DownloaderFacade downloaderFacade;

    public ServiceInputOutputManagerImpl(FtepServerClient ftepServerClient, DownloaderFacade downloaderFacade) {
        this.ftepServerClient = ftepServerClient;
        this.downloaderFacade = downloaderFacade;
    }

    @Override
    public void prepareInput(Path target, Collection<URI> uris) throws IOException {
        // Multiple URIs, download to a subdir named after the filename portion of the URI
        Files.createDirectories(target);
        Map<URI, Optional<Path>> inputs = uris.stream()
                .flatMap(downloaderFacade::resolveUri)
                .collect(Collectors.toMap(
                        uri -> uri,
                        uri -> Optional.of(target.resolve(MoreFiles.getNameWithoutExtension(Paths.get(uri.getPath()))))));
        downloaderFacade.download(inputs);
    }

    @Override
    public Path getServiceContext(String serviceName) {
        try {
            URI uri = URI.create(StrSubstitutor.replace(FTEP_SERVICE_CONTEXT, ImmutableMap.of("serviceName", serviceName)));
            return downloaderFacade.download(uri, null);
        } catch (Exception e) {
            throw new ServiceIoException("Could not construct service context for " + serviceName, e);
        }
    }

    @Override
    public boolean isSupportedProtocol(String scheme) {
        return downloaderFacade.isSupportedProtocol(scheme);
    }

    @Override
    public void cleanUp(Set<URI> unusedUris) {
        if (!unusedUris.isEmpty()) {
            CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueService = ftepServerClient.catalogueServiceBlockingStub();

            Uris.Builder ftepFileUris = Uris.newBuilder();
            unusedUris.forEach(uri -> ftepFileUris.addFileUris(FtepFileUri.newBuilder().setUri(uri.toString()).build()));

            UriDataSourcePolicies dataSourcePolicies = catalogueService.getDataSourcePolicies(ftepFileUris.build());
            dataSourcePolicies.getPoliciesList().stream()
                    .filter(uriPolicy -> uriPolicy.getPolicy() == UriDataSourcePolicy.Policy.REMOTE_ONLY)
                    .peek(uriPolicy -> LOG.info("Evicting REMOTE_ONLY data from: {}", uriPolicy))
                    .forEach(uriPolicy -> downloaderFacade.cleanUp(URI.create(uriPolicy.getUri().getUri())));
        }
    }

}
