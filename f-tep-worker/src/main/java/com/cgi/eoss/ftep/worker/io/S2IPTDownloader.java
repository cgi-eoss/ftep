package com.cgi.eoss.ftep.worker.io;

import com.google.common.base.Preconditions;
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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class S2IPTDownloader implements Downloader {

    private static final Pattern S2_PRODUCT_PATTERN = Pattern.compile("/(?<PRODUCT>S2A.*_(?<YEAR>\\d{4})(?<MONTH>\\d{2})(?<DAY>\\d{2})T\\d{6})");
    private static final String IPT_S2_URI_FORMAT = "https://static.eocloud.eu/v1/AUTH_8f07679eeb0a43b19b33669a4c888c45/eorepo/Sentinel-2/MSI/L1C/${YEAR}/${MONTH}/${DAY}/${PRODUCT}.zip";
    private static final Set<String> GROUPS = ImmutableSet.of("PRODUCT", "YEAR", "MONTH", "DAY");

    private final IptDownloader iptDownloader;

    @Override
    public Set<String> getProtocols() {
        // TODO Enable this downloader for s2, when prioritisation is possible
        return ImmutableSet.of();
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        String s2Product = uri.getPath();
        Matcher s2Regex = S2_PRODUCT_PATTERN.matcher(s2Product);
        Preconditions.checkArgument(s2Regex.matches(), "S2 product name not recognised: " + s2Regex);
        Map<String, String> s2Attrs = GROUPS.stream().collect(Collectors.toMap(Function.identity(), s2Regex::group));

        try {
            URI iptUri = new URI(StrSubstitutor.replace(IPT_S2_URI_FORMAT, s2Attrs));
            return iptDownloader.download(targetDir, iptUri);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
