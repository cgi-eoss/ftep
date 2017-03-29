package com.cgi.eoss.ftep.worker.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class S2CEDADownloader implements Downloader {
    private static final Pattern S2_PRODUCT_PATTERN = Pattern.compile("/(?<PRODUCT>S2A.*_(?<YEAR>\\d{4})(?<MONTH>\\d{2})(?<DAY>\\d{2})T\\d{6})");
    private static final String CEDA_FTP_URI_FORMAT = "ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/L1C_MSI/%s/%s/%s/%s.zip";

    private final FtpDownloader ftpDownloader;

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("s2");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        String s2Product = uri.getPath();
        Matcher s2Regex = S2_PRODUCT_PATTERN.matcher(s2Product);
        Preconditions.checkArgument(s2Regex.matches(), "S2 product name not recognised: " + s2Regex);

        try {
            URI ftpUri = new URI(String.format(CEDA_FTP_URI_FORMAT, s2Regex.group("YEAR"), s2Regex.group("MONTH"), s2Regex.group("DAY"), s2Regex.group("PRODUCT")));
            return ftpDownloader.download(targetDir, ftpUri);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
