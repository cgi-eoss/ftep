package com.cgi.eoss.ftep.orchestrator.io;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.ONE_SECOND;
import static org.awaitility.Duration.TEN_MINUTES;

@Slf4j
public class CachingSymlinkIOManager implements ServiceInputOutputManager {
    private static final HashFunction HASH_FUNCTION = Hashing.sha1();

    private final LoadingCache<URI, Path> loadingCache;

    public CachingSymlinkIOManager(Path cacheRoot, DownloaderFactory downloaderFactory) {
        this.loadingCache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .build(new UriCacheLoader(cacheRoot, downloaderFactory));
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

    private static String hashUri(URI uri) {
        return HASH_FUNCTION.hashString(uri.toString(), Charset.forName("UTF-8")).toString();
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
                    LOG.info("Download already in progress, waiting for completion for URI {}: {}", uri, resultDir);
                    blockUntilDownloadFinishes(inProgressDir, resultDir);
                    return resultDir;
                }

                LOG.info("URI not found in cache, downloading: {}", uri);

                // Create the temp directory to show we are working on this uri
                try {
                    Files.createDirectories(inProgressDir);
                    Path downloadedFile = downloaderFactory.getDownloader(uri).download(inProgressDir, uri);

                    String filename = downloadedFile.getFileName().toString();

                    if (filename.toLowerCase().endsWith(".zip")) {
                        ZipHandler.unzip(downloadedFile, inProgressDir);
                        Files.delete(downloadedFile);
                    }

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
    }

    private void blockUntilDownloadFinishes(Path inProgressDir, Path resultDir) {
        // TODO Remove when this class is a singleton in an environment; LoadingCache does concurrency
        with().pollInterval(ONE_SECOND)
                .and().atMost(TEN_MINUTES)
                .await("Download task has finished")
                .until(() -> !Files.exists(inProgressDir) && Files.exists(resultDir));
    }

}
