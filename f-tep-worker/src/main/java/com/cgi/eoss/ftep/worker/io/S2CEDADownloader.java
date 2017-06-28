package com.cgi.eoss.ftep.worker.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class S2CEDADownloader implements Downloader {

    private static final DateTimeFormatter PRODUCT_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter YEAR_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FOLDER_FORMAT = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FOLDER_FORMAT = DateTimeFormatter.ofPattern("dd");

    // See https://sentinel.esa.int/web/sentinel/user-guides/sentinel-2-msi/naming-convention for pattern origin
    private static final Pattern OLD_S2_PRODUCT_PATTERN = Pattern.compile(
            "/(?<PRODUCT>(?<MISSION>\\w{3})_(?<FILECLASS>OPER_)(?<FILETYPE>(?<FILECATEGORY>\\w{3})_(?<PRODUCTLEVEL>\\w{6}))_(?<SITECENTRE>\\w{4})_(?<CREATIONDATE>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2})_(?<RORBIT>R\\d{3})_(?<VALIDITYPERIOD>V(?<VALIDITYSTART>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2})_(?<VALIDITYEND>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2})))$"
    );
    private static final Pattern NEW_S2_PRODUCT_PATTERN = Pattern.compile(
            "/(?<PRODUCT>(?<MISSION>\\w{3})_(?<PRODUCTLEVEL>\\w{6})_(?<VALIDITYSTART>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2})_(?<PROCESSINGBASELINE>N\\d{4})_(?<RORBIT>R\\d{3})_(?<TILE>T\\w{5})_(?<PRODUCTDISCRIMINATOR>\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2}))$"
    );

    private static final String CEDA_FTP_URI_FORMAT = "ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/${PRODUCT_LEVEL}/${YEAR}/${MONTH}/${DAY}/${PRODUCT}.zip";

    private final FtpDownloader ftpDownloader;

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("sentinel2");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        String s2Product = uri.getPath();
        Matcher s2Regex = getMatcher(s2Product);
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

        try {
            URI ftpUri = new URI(StrSubstitutor.replace(CEDA_FTP_URI_FORMAT, s2Attrs));
            return ftpDownloader.download(targetDir, ftpUri);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private Matcher getMatcher(String s2Product) {
        // Test against the new-style naming convention first, then try the old
        return NEW_S2_PRODUCT_PATTERN.matcher(s2Product).matches() ? NEW_S2_PRODUCT_PATTERN.matcher(s2Product) : OLD_S2_PRODUCT_PATTERN.matcher(s2Product);
    }

}
