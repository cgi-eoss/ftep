package com.cgi.eoss.ftep.data.manager.core;

import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult;
import com.cgi.eoss.ftep.core.utils.beans.ParameterId;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zoo.project.ZooConstants;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipError;

import static com.cgi.eoss.ftep.data.manager.core.ZipHandler.unzip;
import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.ONE_SECOND;
import static org.awaitility.Duration.TEN_MINUTES;

/**
 */
// TODO Replace this whole class with a better multiprocess download manager!
public class MultiTenantFileDataManager implements DataManager {
    private static final Logger LOG = LoggerFactory.getLogger(MultiTenantFileDataManager.class);

    private static final HashFunction HASH_FUNCTION = Hashing.sha1();

    private final Map<URL, WorkingDirSymlink> downloadedFiles = Maps.newHashMap();

    private SecpDownloader secpDownloader = new SecpDownloader();

    private static URL urlFromString(String input) {
        try {
            return new URL(input);
        } catch (Exception e) {
            throw new RuntimeException("Received malformed download URL", e);
        }
    }

    @Override
    public DataManagerResult getData(Map<String, String> downloadConfiguration, String destination, Map<String, List<String>> inputUrlListsWithJobID) {
        DownloaderConfiguration configuration = new DownloaderConfiguration(
                Paths.get(downloadConfiguration.get(ZooConstants.ZOO_FTEP_DATA_DOWNLOAD_DIR_PARAM)),
                Paths.get(downloadConfiguration.get(ZooConstants.ZOO_FTEP_DOWNLOAD_TOOL_PATH_PARAM))
        );

        Map<ParameterId, List<URL>> downloadUrls = new HashMap<>();
        for (Map.Entry<String, List<String>> jobUrls : inputUrlListsWithJobID.entrySet()) {
            downloadUrls.put(
                    ParameterId.of(jobUrls.getKey()),
                    jobUrls.getValue().stream()
                            .map(MultiTenantFileDataManager::urlFromString)
                            .collect(Collectors.toList()));
        }

        Map<URL, DownloadTask> results = getData(configuration, Paths.get(destination), downloadUrls);

        DataManagerResult ret = new DataManagerResult();

        Map<String, List<String>> resultsSymlinks = Maps.newHashMap();

        for (Map.Entry<ParameterId, List<URL>> paramUrls : downloadUrls.entrySet()) {
            String parameterId = paramUrls.getKey().getId();
            resultsSymlinks.put(parameterId,
                    paramUrls.getValue().stream()
                            .filter(u -> results.get(u).getResult() == DownloadResult.COMPLETED)
                            .map(u -> results.get(u).getSymlink().toString())
                            .collect(Collectors.toList()));
        }

        ret.setUpdatedInputItems(resultsSymlinks);

        DataManagerResult.DataDownloadStatus status;
        if (results.values().stream().anyMatch(DownloadTask::isSuccess)) {
            if (results.values().stream().anyMatch(DownloadTask::isFailure)) {
                status = DataManagerResult.DataDownloadStatus.PARTIAL;
            } else {
                status = DataManagerResult.DataDownloadStatus.COMPLETE;
            }
        } else {
            status = DataManagerResult.DataDownloadStatus.NONE;
        }
        ret.setDownloadStatus(status);

        return ret;
    }

    Map<URL, DownloadTask> getData(DownloaderConfiguration configuration, Path destination, Map<ParameterId, List<URL>> downloadUrls) {
        Path downloadRoot = configuration.getDownloadDir();

        // Download all URLs
        Set<URL> allUrls = downloadUrls.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Set<DownloadTask> results = Sets.newHashSet();

        for (URL url : allUrls) {
            String urlId = hashUrl(url);
            Path resultDir = downloadRoot.resolve(urlId);

            Optional<WorkingDirSymlink> downloadResult = download(configuration, resultDir, url);

            if (downloadResult.isPresent()) {
                Path symlink = createSymlink(destination, downloadResult.get(), resultDir);
                results.add(new DownloadTask(url, symlink, DownloadResult.COMPLETED));
            } else {
                results.add(new DownloadTask(url, null, DownloadResult.FAILED));
            }
        }

        return results.stream().collect(Collectors.toMap(DownloadTask::getUrl, Function.identity()));
    }

    private Path createSymlink(Path destination, WorkingDirSymlink workingDirSymlink, Path resultDir) {
        Path link = destination.resolve(workingDirSymlink.getFilename());
        Path target = workingDirSymlink.isSingleFile() ? resultDir.resolve(workingDirSymlink.getFilename()) : resultDir;

        try {
            return Files.createSymbolicLink(link, target).toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create symlink from " + link + " to " + target, e);
        }
    }

    private Optional<WorkingDirSymlink> download(DownloaderConfiguration configuration, Path resultDir, URL url) {
        Path inProgressDir = resultDir.resolveSibling(resultDir.getFileName() + ".part");

        if (Files.exists(resultDir)) {
            return Optional.of(downloadedFiles.get(url));
        } else {
            try {
                if (Files.exists(inProgressDir)) {
                    blockUntilDownloadFinishes(inProgressDir, resultDir);
                    return Optional.of(downloadedFiles.get(url));
                } else {
                    Path downloadDir = Files.createDirectories(inProgressDir);
                    Path downloadedFile = secpDownloader.download(configuration.getDownloadScript(), downloadDir, url);

                    String filename = downloadedFile.getFileName().toString();
                    String filetype = Files.probeContentType(downloadedFile);
                    WorkingDirSymlink download;

                    // Unzip if necessary
                    if (filename.toLowerCase().endsWith(".zip") && filetype.equals("application/zip")) {
                        unzip(downloadedFile, downloadDir);
                        download = new WorkingDirSymlink(FilenameUtils.removeExtension(filename), false);
                    } else {
                        download = new WorkingDirSymlink(filename, true);
                    }

                    Files.move(downloadDir, resultDir, StandardCopyOption.ATOMIC_MOVE);
                    downloadedFiles.put(url, download);

                    return Optional.of(download);
                }
            } catch (Exception|ZipError e) {
                LOG.error("Failed to download (or extract) file", e);
                // Clean up
                if (Files.exists(inProgressDir)) {
                    try {
                        Files.walk(inProgressDir, FileVisitOption.FOLLOW_LINKS)
                                .sorted(Comparator.reverseOrder())
                                .forEach(this::delete);
                    } catch (Exception delE) {
                        throw new RuntimeException("Could not clean up failed download", delE);
                    }
                }
                return Optional.empty();
            }
        }
    }

    private void blockUntilDownloadFinishes(Path inProgressDir, Path resultDir) {
        // Awaitility shouldn't be necessary for production code!
        with().pollInterval(ONE_SECOND)
                .and().atMost(TEN_MINUTES)
                .await("Download task has finished")
                .until(() -> !Files.exists(inProgressDir) && Files.exists(resultDir));
    }

    private String hashUrl(URL url) {
        return HASH_FUNCTION.hashString(url.toString(), Charset.forName("UTF-8")).toString();
    }

    private void delete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private enum DownloadResult {
        COMPLETED, FAILED
    }

    static final class DownloadTask {
        private final URL url;
        private final Path symlink;
        private final DownloadResult result;

        private DownloadTask(URL url, Path symlink, DownloadResult result) {
            this.url = url;
            this.symlink = symlink;
            this.result = result;
        }

        public URL getUrl() {
            return url;
        }

        public Path getSymlink() {
            return symlink;
        }

        public DownloadResult getResult() {
            return result;
        }

        public boolean isSuccess() {
            return result == DownloadResult.COMPLETED;
        }

        public boolean isFailure() {
            return !isSuccess();
        }
    }

}
