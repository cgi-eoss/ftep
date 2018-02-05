package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.io.ServiceIoException;
import com.cgi.eoss.ftep.io.ZipHandler;
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
import org.jooq.lambda.Unchecked;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

/**
 * <p>A multi-protocol downloader facade which saves to a shared download cache, and symlinks the target output
 * directory to this cache.</p>
 */
@Log4j2
public class CachingSymlinkDownloaderFacade implements DownloaderFacade {

    // SHA-1 is still safe for our purpose - almost-certainly-unique URL->directory name hashing
    @SuppressWarnings("deprecation")
    private static final HashFunction HASH_FUNCTION = Hashing.sha1();
    private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
    private static final int DEFAULT_MAX_WEIGHT = 1024;
    private static final String URI_FILENAME = ".uri";

    private static final Predicate<URI> ALWAYS_EVICT_URI = uri -> uri.toString().startsWith("ftep://databasket/") || uri.toString().startsWith("ftep://serviceContext/");

    private final Path cacheRoot;
    private final boolean unzipAllDownloads;
    private final LoadingCache<URI, Path> cache;
    private final Set<Downloader> downloaders = new HashSet<>();

    public CachingSymlinkDownloaderFacade(Path cacheRoot) {
        this(cacheRoot, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_MAX_WEIGHT, true);
    }

    public CachingSymlinkDownloaderFacade(Path cacheRoot, Integer concurrencyLevel, Integer maximumWeight, Boolean unzipAllDownloads) {
        this.cacheRoot = cacheRoot;
        this.unzipAllDownloads = unzipAllDownloads;
        this.cache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrencyLevel)
                .maximumWeight(maximumWeight)
                .weigher(new GigabyteWeigher())
                .removalListener(new PathDeletingRemovalListener())
                .build(new UriCacheLoader());
    }

    @Override
    public Path download(URI uri, Path targetDir) {
        return download(ImmutableMap.of(uri, Optional.ofNullable(targetDir))).get(uri);
    }

    @Override
    public Map<URI, Path> download(Map<URI, Optional<Path>> uriPathMap) {
        try {
            evictAlwaysFreshUris(uriPathMap.keySet());
            ImmutableMap<URI, Path> downloadDirs = cache.getAll(uriPathMap.keySet());
            Map<URI, Path> targetDirs = new HashMap<>();
            for (URI uri : uriPathMap.keySet()) {
                targetDirs.put(uri, symlinkTargetToCache(uriPathMap.get(uri), downloadDirs.get(uri)));
            }
            return targetDirs;
        } catch (ServiceIoException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceIoException(e);
        }
    }

    @Override
    public Stream<URI> resolveUri(URI uri) {
        return getAvailableDownloaders(uri).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No downloader found for URI: %s", uri)))
                .resolveUri(uri);
    }

    @Override
    public boolean isSupportedProtocol(String scheme) {
        return downloaders.stream().anyMatch(d -> d.getProtocols().contains(scheme));
    }

    @Override
    public void registerDownloader(Downloader downloader) {
        this.downloaders.add(downloader);
    }

    @Override
    public void unregisterDownloader(Downloader downloader) {
        this.downloaders.remove(downloader);
    }

    @Override
    public void cleanUp(URI uri) {
        cache.invalidate(uri);
    }

    @PostConstruct
    public void scanExistingCacheEntries() throws IOException {
        try (Stream<Path> cacheDirContents = Files.list(cacheRoot)) {
            cacheDirContents
                    .map(dir -> dir.resolve(URI_FILENAME))
                    .filter(Files::exists)
                    .map(Unchecked.function(uriFile -> URI.create(new String(Files.readAllBytes(uriFile)).trim())))
                    .peek(uri -> LOG.debug("Registering existing cache path for: {}", uri))
                    .forEach(Unchecked.consumer(cache::get));
        }
    }

    @Scheduled(fixedRate = 30000)
    public void expireDeletedCachePaths() {
        cache.invalidateAll(
                cache.asMap().entrySet().stream()
                        .filter(e -> Files.notExists(e.getValue()))
                        .map(Map.Entry::getKey)
                        .peek(uri -> LOG.debug("Invalidating deleted cache path for: {}", uri))
                        .collect(Collectors.toSet())
        );
    }

    private Path symlinkTargetToCache(Optional<Path> targetDir, Path downloadDir) {
        return targetDir
                .map(Unchecked.function(t -> Files.createSymbolicLink(t, downloadDir)))
                .orElse(downloadDir);
    }

    private void evictAlwaysFreshUris(Set<URI> uris) {
        cache.invalidateAll(uris.stream()
                .filter(ALWAYS_EVICT_URI)
                .collect(Collectors.toSet()));
    }

    private ImmutableList<Downloader> getAvailableDownloaders(URI uri) {
        return ImmutableList.sortedCopyOf(
                new DownloaderUriComparator(uri),
                downloaders.stream().filter(d -> d.getProtocols().contains(uri.getScheme())).collect(Collectors.toSet()));
    }

    /**
     * <p>Calculates the relative weight of cache entries by simply counting the number of gigabytes in the entry.</p>
     * <p>The result is rounded towards positive infinity, so the minimum weight of an entry (even &lt;1 GB) is always
     * 1.</p>
     */
    private static final class GigabyteWeigher implements Weigher<URI, Path> {
        @Override
        public int weigh(URI key, Path value) {
            try (Stream<Path> dirContents = Files.walk(value)) {
                double dirSize = dirContents
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

    private final class UriCacheLoader extends CacheLoader<URI, Path> {
        @Override
        public Path load(URI uri) throws Exception {
            String cacheDirKey = HASH_FUNCTION.hashString(uri.toString(), Charset.forName("UTF-8")).toString();
            Path cacheDir = cacheRoot.resolve(cacheDirKey);

            // Shortcut return if the directory already exists
            if (Files.isDirectory(cacheDir)) {
                return resolveCacheSymlink(cacheDir).orElse(cacheDir);
            }

            LOG.info("URI not found in cache, downloading \"{}\" to {}", uri, cacheDir);

            Path inProgressDir = cacheDir.resolveSibling(cacheDir.getFileName() + ".part");
            if (Files.isDirectory(inProgressDir)) {
                LOG.warn("Partial download found for URI, cleaning up and continuing: {} downloading to {}", uri, inProgressDir);
                MoreFiles.deleteRecursively(inProgressDir);
            }

            try {
                // Create the temp directory to show we are working on this uri
                Files.createDirectories(inProgressDir);

                // Write the source uri to the directory, for backwards resolution if needed
                Path urlFile = inProgressDir.resolve(URI_FILENAME);
                Files.write(urlFile, ImmutableList.of(uri.toString()), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE_NEW);

                // Download the file!
                Path downloaded = doDownload(inProgressDir, uri);
                if (unzipAllDownloads && downloaded.getFileName().toString().toLowerCase().endsWith(".zip")) {
                    ZipHandler.unzip(downloaded, inProgressDir);
                    Files.delete(downloaded);
                }

                // Move the temp directory to the final location
                Files.move(inProgressDir, cacheDir, ATOMIC_MOVE);
            } catch (Exception e) {
                try {
                    // Try to clean up if any error occurred
                    MoreFiles.deleteRecursively(inProgressDir);
                } catch (Exception e1) {
                    LOG.error("Unable to clean up failed cache directory", e1);
                }
                throw new ServiceIoException("Failed to populate cache directory: " + inProgressDir, e);
            }

            return resolveCacheSymlink(cacheDir).orElse(cacheDir);
        }

        private Path doDownload(Path targetDir, URI uri) {
            List<Downloader> availableDownloaders = getAvailableDownloaders(uri);
            for (Downloader downloader : availableDownloaders) {
                try {
                    LOG.debug("Attempting download with {} for uri: {}", downloader, uri);
                    return downloader.download(targetDir, uri);
                } catch (Exception e) {
                    LOG.error("Failed to download with {} uri: {}", downloader, uri, e);
                }
            }
            throw new ServiceIoException("No downloader was able to process the URI: " + uri);
        }

        private Optional<Path> resolveCacheSymlink(Path cacheDir) {
            // If the downloader returned a single symlink (i.e. it's a "resolving" downloader), point to that rather than the cacheDir directly
            try (Stream<Path> cacheDirList = Files.list(cacheDir)) {
                Set<Path> cacheDirContents = cacheDirList
                        .filter(f -> !f.getFileName().toString().equals(URI_FILENAME))
                        .collect(Collectors.toSet());

                if (cacheDirContents.size() == 1) {
                    Path file = Iterables.getOnlyElement(cacheDirContents);
                    if (Files.isSymbolicLink(file)) {
                        return Optional.of(file);
                    }
                }
            } catch (IOException e) {
                LOG.warn("Failed to read contents of cache directory {}", cacheDir, e);
            }
            return Optional.empty();
        }
    }

}
