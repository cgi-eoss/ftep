package com.cgi.eoss.ftep.worker.io;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

@Slf4j
@Service("cachingSymlinkIOManager")
public class CachingSymlinkIOManager implements ServiceInputOutputManager {
    private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
    private static final int DEFAULT_MAX_WEIGHT = 1024;
    private static final HashFunction HASH_FUNCTION = Hashing.sha1();

    private final LoadingCache<URI, Path> loadingCache;

    @Autowired
    public CachingSymlinkIOManager(@Qualifier("cacheConcurrencyLevel") Integer concurrencyLevel,
                                   @Qualifier("cacheMaxWeight") Integer maximumWeight,
                                   @Qualifier("cacheRoot") Path cacheRoot,
                                   DownloaderFactory downloaderFactory) {
        this.loadingCache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrencyLevel)
                .maximumWeight(maximumWeight)
                .weigher(new GigabyteWeigher())
                .removalListener(new PathDeletingRemovalListener())
                .build(new UriCacheLoader(cacheRoot, downloaderFactory));
    }

    public CachingSymlinkIOManager(Path cacheRoot,
                                   DownloaderFactory downloaderFactory) {
        this(DEFAULT_CONCURRENCY_LEVEL, DEFAULT_MAX_WEIGHT, cacheRoot, downloaderFactory);
    }

    @Override
    public void prepareInput(Path link, URI uri) throws IOException {
        try {
            Path target = loadingCache.get(uri);
            LOG.info("Linking {} to {}", link, target);
            Files.createSymbolicLink(link, target);
        } catch (Exception e) {
            throw new ServiceIoException("Could not populate cache from URI: " + uri, e);
        }
    }

    private class UriCacheLoader extends CacheLoader<URI, Path> {
        private final Path cacheRoot;
        private final DownloaderFactory downloaderFactory;

        public UriCacheLoader(Path cacheRoot, DownloaderFactory downloaderFactory) {
            this.cacheRoot = cacheRoot;
            this.downloaderFactory = downloaderFactory;
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

                LOG.info("URI not found in cache, downloading: {}", uri);

                try {
                    // Create the temp directory to show we are working on this uri
                    Files.createDirectories(inProgressDir);

                    // Write the source uri to the directory, for backwards resolution if needed
                    Path urlFile = inProgressDir.resolve(".uri");
                    Files.write(urlFile, ImmutableList.of(uri.toString()), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE_NEW);

                    // Download the file, and unzip it if necessary
                    Path downloadedFile = downloaderFactory.getDownloader(uri).download(inProgressDir, uri);
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

            LOG.info("Returning cached directory for URI {}: {}", uri, resultDir);
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
            LOG.info("Removing cached download path: {}", notification.getValue());
            try {
                MoreFiles.deleteRecursively(notification.getValue());
            } catch (IOException e) {
                LOG.error("Unable to delete expired cache directory {}", notification.getValue(), e);
            }
        }
    }

}
