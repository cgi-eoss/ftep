package com.cgi.eoss.ftep.search.providers.ceda;

import com.cgi.eoss.ftep.rpc.Credentials;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.MoreFiles;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.text.StrSubstitutor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class CedaQuicklooksCache {

    private static final int CONNECT_TIMEOUT = 2000;
    private static final String FTPS_SCHEME = "ftps";
    private static final DateTimeFormatter PRODUCT_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter YEAR_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FOLDER_FORMAT = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FOLDER_FORMAT = DateTimeFormatter.ofPattern("dd");
    private static final Pattern OLD_S2_PRODUCT_PATTERN = Pattern.compile(
            "(?<PRODUCT>(?<MISSION>\\w{3})_(?<FILECLASS>OPER_)(?<FILETYPE>(?<FILECATEGORY>\\w{3})_(?<PRODUCTLEVEL>\\w{6}))_(?<SITECENTRE>\\w{4})_(?<CREATIONDATE>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2})_(?<RORBIT>R\\d{3})_(?<VALIDITYPERIOD>V(?<VALIDITYSTART>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2})_(?<VALIDITYEND>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2})))$"
    );
    private static final Pattern NEW_S2_PRODUCT_PATTERN = Pattern.compile(
            "(?<PRODUCT>(?<MISSION>\\w{3})_(?<PRODUCTLEVEL>\\w{6})_(?<VALIDITYSTART>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2})_(?<PROCESSINGBASELINE>N\\d{4})_(?<RORBIT>R\\d{3})_(?<TILE>T\\w{5})_(?<PRODUCTDISCRIMINATOR>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2}))$"
    );

    private final LoadingCache<String, Path> cache;
    private final Path cacheBaseDir;
    private final Credentials creds;

    public CedaQuicklooksCache(CedaSearchProperties cedaSearchProperties, String quicklooksCacheDirectory) {
        this.cacheBaseDir = Paths.get(quicklooksCacheDirectory);
        this.creds = Credentials.newBuilder().setUsername(cedaSearchProperties.getUsername()).setPassword(cedaSearchProperties.getPassword()).build();
        this.cache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(1000L)
                .removalListener(new PathDeletingRemovalListener())
                .build(new CedaQuicklookDownloader());
    }

    public Path get(String productIdentifier) throws ExecutionException {
        return cache.get(productIdentifier);
    }

    private static final class PathDeletingRemovalListener implements RemovalListener<String, Path> {
        @Override
        public void onRemoval(RemovalNotification<String, Path> notification) {
            LOG.info("Quicklook cache entry {} expired: {}", notification.getKey(), notification.getCause());
            if (Files.exists(notification.getValue())) {
                LOG.info("Removing cached quicklook path: {}", notification.getValue());
                try {
                    MoreFiles.deleteRecursively(notification.getValue());
                } catch (IOException e) {
                    LOG.error("Unable to delete expired quicklook {}", notification.getValue(), e);
                }
            }
        }
    }

    private final class CedaQuicklookDownloader extends CacheLoader<String, Path> {
        private static final String CEDA_FTP_URI_FORMAT = "ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/${PRODUCT_LEVEL}/${YEAR}/${MONTH}/${DAY}/${PRODUCT}.png";

        @Override
        public Path load(String key) throws Exception {
            LOG.info("Retrieving quicklook for: {}", key);

            URI uri = getUriForProduct(key);

            FTPClient ftpClient = FTPS_SCHEME.equals(uri.getScheme()) ? new FTPSClient() : new FTPClient();
            ftpClient.setConnectTimeout(CONNECT_TIMEOUT);

            try {
                if (uri.getPort() == -1) {
                    ftpClient.connect(uri.getHost());
                } else {
                    ftpClient.connect(uri.getHost(), uri.getPort());
                }
                ftpClient.login(creds.getUsername(), creds.getPassword());
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                Path outputFile = cacheBaseDir.resolve(Paths.get(uri.getPath()).getFileName().toString());
                Files.createDirectories(outputFile.getParent());
                try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
                    boolean success = ftpClient.retrieveFile(uri.getPath(), os);
                    if (!success) {
                        LOG.error("FTP download error: {}", ftpClient.getReplyString());
                        throw new IOException("Failed to download via FTP: " + uri);
                    }
                }
                LOG.info("Successfully downloaded quicklook via FTP: {}", outputFile);
                return outputFile;
            } finally {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            }
        }

        private URI getUriForProduct(String productIdentifier) {
            Matcher s2Regex = getMatcher(productIdentifier);
            Preconditions.checkArgument(s2Regex.matches(), "S2 product name not recognised: " + s2Regex);

            String productId = s2Regex.group("PRODUCT");
            String productLevel = s2Regex.group("PRODUCTLEVEL");
            LocalDateTime productStartDate = LocalDateTime.parse(s2Regex.group("VALIDITYSTART"), PRODUCT_ID_DATE_FORMAT);

            Map<String, String> s2Attrs = ImmutableMap.of(
                    "PRODUCT", productId,
                    "PRODUCT_LEVEL", productLevel.equals("MSIL1C") ? "L1C_MSI" : productLevel.substring(3) + "_" + productLevel.substring(0, 3),
                    "YEAR", YEAR_FOLDER_FORMAT.format(productStartDate),
                    "MONTH", MONTH_FOLDER_FORMAT.format(productStartDate),
                    "DAY", DAY_FOLDER_FORMAT.format(productStartDate)
            );

            return URI.create(StrSubstitutor.replace(CEDA_FTP_URI_FORMAT, s2Attrs));
        }

        private Matcher getMatcher(String s2Product) {
            // Test against the new-style naming convention first, then try the old
            return NEW_S2_PRODUCT_PATTERN.matcher(s2Product).matches() ? NEW_S2_PRODUCT_PATTERN.matcher(s2Product) : OLD_S2_PRODUCT_PATTERN.matcher(s2Product);
        }
    }

}
