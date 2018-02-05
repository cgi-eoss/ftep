package com.cgi.eoss.ftep.io.download;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StrSubstitutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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

@Log4j2
public class S2IptDownloader implements Downloader {

    private static final Pattern S2_PRODUCT_PATTERN = Pattern.compile("/(?<PRODUCT>S2A.*_(?<YEAR>\\d{4})(?<MONTH>\\d{2})(?<DAY>\\d{2})T\\d{6})");
    private static final String IPT_S2_URI_FORMAT = "https://static.eocloud.eu/v1/AUTH_8f07679eeb0a43b19b33669a4c888c45/eorepo/Sentinel-2/MSI/L1C/${YEAR}/${MONTH}/${DAY}/${PRODUCT}.zip";
    private static final Set<String> GROUPS = ImmutableSet.of("PRODUCT", "YEAR", "MONTH", "DAY");

    private final IptHttpDownloader iptDownloader;
    private final ProtocolPriority protocolPriority;
    private final DownloaderFacade downloaderFacade;

    public S2IptDownloader(DownloaderFacade downloaderFacade, IptHttpDownloader iptDownloader, ProtocolPriority protocolPriority) {
        this.downloaderFacade = downloaderFacade;
        this.iptDownloader = iptDownloader;
        this.protocolPriority = protocolPriority;
    }

    @PostConstruct
    public void postConstruct() {
        downloaderFacade.registerDownloader(this);
    }

    @PreDestroy
    public void preDestroy() {
        downloaderFacade.unregisterDownloader(this);
    }

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("sentinel2");
    }

    @Override
    public int getPriority(URI uri) {
        return protocolPriority.get(uri.getScheme());
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        String s2Product = uri.getPath();
        Matcher s2Regex = S2_PRODUCT_PATTERN.matcher(s2Product);
        Preconditions.checkArgument(s2Regex.matches(), "S2 product name not recognised: " + s2Regex);
        Map<String, String> s2Attrs = GROUPS.stream().collect(Collectors.toMap(Function.identity(), s2Regex::group));

        // Ensure SAFE format suffix is present
        if (!s2Product.endsWith(".SAFE")) {
            s2Attrs.replace("PRODUCT", s2Attrs.get("PRODUCT") + ".SAFE");
        }

        try {
            URI iptUri = new URI(StrSubstitutor.replace(IPT_S2_URI_FORMAT, s2Attrs));
            return iptDownloader.download(targetDir, iptUri);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
