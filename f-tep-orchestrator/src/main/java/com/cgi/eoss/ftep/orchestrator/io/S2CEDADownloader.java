package com.cgi.eoss.ftep.orchestrator.io;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S2CEDADownloader implements Downloader {
    private static final Pattern S2_PRODUCT_PATTERN = Pattern.compile("/(?<PRODUCT>S2A.*_(?<YEAR>\\d{4})(?<MONTH>\\d{2})(?<DAY>\\d{2})T\\d{6})");
    private static final String CEDA_FTP_URI_FORMAT = "ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/L1C_MSI/%s/%s/%s/%s.zip";

    private static final HttpFtpDownloader ftpDownloadHandler = new HttpFtpDownloader();

    @Override
    public void download(Path target, URI uri) throws IOException {
        String s2Product = uri.getPath();
        Matcher s2Regex = S2_PRODUCT_PATTERN.matcher(s2Product);
        Preconditions.checkArgument(s2Regex.matches(), "S2 product name not recognised: " + s2Regex);

        try {
            URI ftpUri = new URI(String.format(CEDA_FTP_URI_FORMAT, s2Regex.group("YEAR"), s2Regex.group("MONTH"), s2Regex.group("DAY"), s2Regex.group("PRODUCT")));
            ftpDownloadHandler.download(target, ftpUri);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
