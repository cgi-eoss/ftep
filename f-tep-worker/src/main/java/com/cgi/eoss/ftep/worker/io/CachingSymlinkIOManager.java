package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFileUri;
import com.cgi.eoss.ftep.rpc.catalogue.UriDataSourcePolicies;
import com.cgi.eoss.ftep.rpc.catalogue.UriDataSourcePolicy;
import com.cgi.eoss.ftep.rpc.catalogue.Uris;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StrSubstitutor;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

@Log4j2
@Service("cachingSymlinkIOManager")
public class CachingSymlinkIOManager implements ServiceInputOutputManager {

    // SHA-1 is still safe for our purpose - almost-certainly-unique URL->directory name hashing
    @SuppressWarnings("deprecation")
    public static final HashFunction HASH_FUNCTION = Hashing.sha1();

    private static final String FTEP_SERVICE_CONTEXT = "ftep://serviceContext/${serviceName}";

    private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
    private static final int DEFAULT_MAX_WEIGHT = 1024;
    private static final String URI_FILENAME = ".uri";

    private final FtepServerClient ftepServerClient;
    private final Path cacheRoot;
    private final DownloaderFacade downloaderFacade;
    private final LoadingCache<URI, Path> loadingCache;

    @Autowired
    public CachingSymlinkIOManager(FtepServerClient ftepServerClient,
                                   @Qualifier("cacheConcurrencyLevel") Integer concurrencyLevel,
                                   @Qualifier("cacheMaxWeight") Integer maximumWeight,
                                   @Qualifier("cacheRoot") Path cacheRoot,
                                   DownloaderFacade downloaderFacade) {
        this.ftepServerClient = ftepServerClient;
        this.cacheRoot = cacheRoot;
        this.downloaderFacade = downloaderFacade;
        this.loadingCache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrencyLevel)
                .maximumWeight(maximumWeight)
                .weigher(new GigabyteWeigher())
                .removalListener(new PathDeletingRemovalListener())
                .build(new UriCacheLoader(cacheRoot, downloaderFacade));
    }

    public CachingSymlinkIOManager(FtepServerClient ftepServerClient,
                                   Path cacheRoot,
                                   DownloaderFacade downloaderFacade) {
        this(ftepServerClient, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_MAX_WEIGHT, cacheRoot, downloaderFacade);
    }

    @PostConstruct
    public void scanExistingCacheEntries() throws IOException {
        Files.list(cacheRoot)
                .map(dir -> dir.resolve(URI_FILENAME))
                .filter(Files::exists)
                .map(Unchecked.function(uriFile -> URI.create(new String(Files.readAllBytes(uriFile)).trim())))
                .peek(uri -> LOG.debug("Registering existing cache path for: {}", uri))
                .forEach(Unchecked.consumer(loadingCache::get));
    }

    @Scheduled(fixedRate = 30000)
    public void expireDeletedCachePaths() {
        loadingCache.invalidateAll(
                loadingCache.asMap().entrySet().stream()
                        .filter(e -> Files.notExists(e.getValue()))
                        .map(Map.Entry::getKey)
                        .peek(uri -> LOG.debug("Invalidating deleted cache path for: {}", uri))
                        .collect(Collectors.toSet())
        );
    }

    @Override
    public void prepareInput(Path targetDir, Set<URI> uris) throws IOException {
        if (uris.size() == 1) {
            // Single URI, link directly to the targetDir
            downloadAndSymlink(Iterables.getOnlyElement(uris), targetDir);
        } else {
            // Multiple URIs, link to a subdir named after the filename portion of the URI
            Files.createDirectories(targetDir);
            for (URI uri : uris) {
                String linkName = MoreFiles.getNameWithoutExtension(Paths.get(uri.getPath()));
                downloadAndSymlink(uri, targetDir.resolve(linkName));
            }
        }
    }

    private void downloadAndSymlink(URI uri, Path link) {
        try {
            Path downloadDir = loadingCache.get(uri);
            LOG.debug("Linking {} to {}", link, downloadDir);
            Files.createSymbolicLink(link, downloadDir);
        } catch (Exception e) {
            throw new ServiceIoException("Could not populate cache from URI: " + uri, e);
        }
    }

    @Override
    public Path getServiceContext(String serviceName) {
        try {
            URI uri = URI.create(StrSubstitutor.replace(FTEP_SERVICE_CONTEXT, ImmutableMap.of("serviceName", serviceName)));
            loadingCache.invalidate(uri);
            return loadingCache.get(uri);
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
                    .forEach(uriPolicy -> loadingCache.invalidate(URI.create(uriPolicy.getUri().getUri())));
        }
    }

    private static final class UriCacheLoader extends CacheLoader<URI, Path> {
        private final Path cacheRoot;
        private final DownloaderFacade downloaderFacade;

        public UriCacheLoader(Path cacheRoot, DownloaderFacade downloaderFacade) {
            this.cacheRoot = cacheRoot;
            this.downloaderFacade = downloaderFacade;
        }

        @Override
        public Path load(URI uri) throws Exception {
            String cacheKey = hashUri(uri);
            Path resultDir = cacheRoot.resolve(cacheKey);

            if (!Files.isDirectory(resultDir)) {
                Path inProgressDir = resultDir.resolveSibling(resultDir.getFileName() + ".part");

                // If a download is already in progress, wait for it to finish
                if (Files.isDirectory(inProgressDir)) {
                    LOG.warn("Partial download found for URI, cleaning up and continuing: {} downloading to {}", uri, inProgressDir);
                    MoreFiles.deleteRecursively(inProgressDir);
                }

                LOG.info("URI not found in cache, downloading \"{}\" to {}", uri, resultDir);

                try {
                    // Create the temp directory to show we are working on this uri
                    Files.createDirectories(inProgressDir);

                    // Write the source uri to the directory, for backwards resolution if needed
                    Path urlFile = inProgressDir.resolve(URI_FILENAME);
                    Files.write(urlFile, ImmutableList.of(uri.toString()), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE_NEW);

                    // Download the file, and unzip it if necessary
                    Path downloadedFile = downloaderFacade.download(inProgressDir, uri);
                    if (downloadedFile.getFileName().toString().toLowerCase().endsWith(".zip")) {
                        ZipHandler.unzip(downloadedFile, inProgressDir);
                        Files.delete(downloadedFile);
                    }

                    // Move the temp directory to the final location
                    Files.move(inProgressDir, resultDir, ATOMIC_MOVE);
                } catch (Exception e) {
                    try {
                        // Try to clean up if any error occurred
                        MoreFiles.deleteRecursively(inProgressDir);
                    } catch (Exception e1) {
                        LOG.error("Unable to clean up failed cache directory", e1);
                    }
                    throw new ServiceIoException("Failed to populate cache directory: " + inProgressDir, e);
                }
            }

            LOG.debug("Returning cached directory for URI {}: {}", uri, resultDir);
            return resultDir;
        }

        private String hashUri(URI uri) {
            return HASH_FUNCTION.hashString(uri.toString(), Charset.forName("UTF-8")).toString();
        }
    }

    /**
     * <p>Calculates the relative weight of cache entries by simply counting the number of gigabytes in the entry.</p>
     * <p>The result is rounded towards positive infinity, so the minimum weight of an entry (even &lt;1 GB) is always
     * 1.</p>
     */
    private static final class GigabyteWeigher implements Weigher<URI, Path> {
        @Override
        public int weigh(URI key, Path value) {
            try {
                long dirSize = Files.walk(value)
                        .filter(Files::isRegularFile)
                        .mapToLong(this::getFileSize)
                        .sum();
                return (int) Math.ceil(dirSize / FileUtils.ONE_GB);
            } catch (IOException e) {
                LOG.warn("Could not calculate size of directory {}", value);
                return 1;
            }
        }

        private long getFileSize(Path p) {
            try {
                return Files.size(p);
            } catch (IOException e) {
                LOG.warn("Could not calculate size of file {}", p);
                return 0;
            }
        }
    }

    /**
     * <p>Fully removes the cached download directory when a cache entry expires.</p>
     */
    private static final class PathDeletingRemovalListener implements RemovalListener<URI, Path> {
        @Override
        public void onRemoval(RemovalNotification<URI, Path> notification) {
            LOG.info("Download cache entry {} expired: {}", notification.getKey(), notification.getCause());
            if (Files.exists(notification.getValue())) {
                LOG.info("Removing cached download path: {}", notification.getValue());
                try {
                    MoreFiles.deleteRecursively(notification.getValue());
                } catch (IOException e) {
                    LOG.error("Unable to delete expired cache directory {}", notification.getValue(), e);
                }
            }
        }
    }

}
