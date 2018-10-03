package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.io.ServiceIoException;
import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.rpc.Credentials;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.jayway.jsonpath.JsonPath;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.apache.logging.log4j.CloseableThreadContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>Downloader for accessing data from <a href="https://finder.eocloud.eu">EO Cloud</a>. Uses IPT's token
 * authentication process.</p>
 */
@Log4j2
public class IptHttpDownloader implements Downloader {

    private static final Multimap<String, String> PROTOCOL_COLLECTIONS = ImmutableMultimap.<String, String>builder()
            .put("sentinel1", "Sentinel1")
            .put("sentinel2", "Sentinel2")
            .put("sentinel3", "Sentinel3")
            .put("landsat", "Landsat5")
            .put("landsat", "Landsat7")
            .put("landsat", "Landsat8")
            .put("envisat", "Envisat")
            .build();

    private final FtepServerClient ftepServerClient;
    private final OkHttpClient httpClient;
    private final OkHttpClient searchClient;
    private final ObjectMapper objectMapper;
    private final DownloaderFacade downloaderFacade;
    private final Properties properties;
    private final ProtocolPriority protocolPriority;

    public IptHttpDownloader(OkHttpClient okHttpClient, int downloadTimeout, int searchTimeout, FtepServerClient ftepServerClient, DownloaderFacade downloaderFacade, Properties properties, ProtocolPriority protocolPriority) {
        // Use a long timeout as the data access can be slow
        this.httpClient = okHttpClient.newBuilder().connectTimeout(downloadTimeout, TimeUnit.SECONDS).readTimeout(downloadTimeout, TimeUnit.SECONDS).build();
        // Use a long timeout as the search query takes a while...
        this.searchClient = okHttpClient.newBuilder().readTimeout(searchTimeout, TimeUnit.SECONDS).build();
        this.ftepServerClient = ftepServerClient;
        this.downloaderFacade = downloaderFacade;
        this.objectMapper = new ObjectMapper();
        this.properties = properties;
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
        return PROTOCOL_COLLECTIONS.keySet();
    }

    @Override
    public int getPriority(URI uri) {
        return protocolPriority.get(uri.getScheme());
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        // IPT downloading is three-step:
        //   1. Call an authentication endpoint to get a token
        //   2. Get the product download location URL by searching the IPT catalogue
        //   3. Download the product using the token identity as a parameter

        Credentials credentials = ftepServerClient.credentialsServiceBlockingStub().getCredentials(
                GetCredentialsParams.newBuilder().setHost(HttpUrl.parse(properties.getIptDownloadUrl()).host()).build());
        String authToken = getAuthToken(credentials);

        HttpUrl downloadUrl = getDownloadUrl(uri, authToken);

        LOG.debug("Resolved IPT download URL with auth token: {}", downloadUrl);

        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new ServiceIoException("Unsuccessful HTTP response: " + response);
        }

        String filename = Iterables.getLast(downloadUrl.pathSegments()) + ".zip";
        Path outputFile = targetDir.resolve(filename);

        try (BufferedSource source = response.body().source();
             BufferedSink sink = Okio.buffer(Okio.sink(outputFile))) {
            long downloadedBytes = sink.writeAll(source);
            LOG.debug("Downloaded {} bytes for {}", downloadedBytes, uri);
        }
        response.close();

        LOG.info("Successfully downloaded via IPT: {}", outputFile);
        return outputFile;
    }

    private String getAuthToken(Credentials credentials) throws IOException {
        Request authRequest = new Request.Builder()
                .url(properties.getAuthEndpoint())
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("domainName", properties.getAuthDomain())
                        .addFormDataPart("userName", credentials.getUsername())
                        .addFormDataPart("userPass", credentials.getPassword())
                        .build())
                .build();

        try (Response response = httpClient.newCall(authRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceIoException("Unsuccessful IPT login: " + response);
            }
            IptTokenResponse iptTokenResponse = objectMapper.readValue(response.body().string(), IptTokenResponse.class);
            LOG.debug("Logged into IPT as user '{}' (id: {}) with tokenIdentity '{}'", iptTokenResponse.getUserName(), iptTokenResponse.getUserId(), iptTokenResponse.getTokenIdentity());
            return iptTokenResponse.getTokenIdentity();
        }
    }

    private HttpUrl getDownloadUrl(URI uri, String authToken) throws IOException {
        // Trim the leading slash from the path and get the search URL
        String productId = uri.getPath().substring(1);

        List<HttpUrl> searchUrls = PROTOCOL_COLLECTIONS.get(uri.getScheme()).stream()
                .map(collection -> buildSearchUrl(collection, productId))
                .collect(Collectors.toList());

        for (HttpUrl searchUrl : searchUrls) {
            try {
                return findDownloadUrl(uri, searchUrl, authToken);
            } catch (Exception e) {
                LOG.debug("Failed to locate download URL: {}", e.getMessage());
            }
        }
        throw new ServiceIoException("Unable to locate IPT product data for " + uri);
    }

    private HttpUrl findDownloadUrl(URI uri, HttpUrl searchUrl, String authToken) throws IOException {
        Request request = new Request.Builder().url(searchUrl).get().build();

        try (Response response = searchClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for IPT search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from IPT: " + response.message());
            }

            String responseBody = response.body().string();
            String productPath = JsonPath.read(responseBody, "$.features[0].properties.productIdentifier");

            return HttpUrl.parse(properties.getIptDownloadUrl()).newBuilder()
                    .addPathSegments(productPath.replaceFirst("^/eodata/", ""))
                    .addQueryParameter("token", authToken)
                    .build();
        } catch (SocketTimeoutException timeout) {
            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                LOG.error("Timeout locating EO product data for {}; please try again and contact the F-TEP support team if the error persists", uri);
            }
            throw new ServiceIoException("Timeout locating IPT product data for " + uri);
        }
    }

    private HttpUrl buildSearchUrl(String collection, String productId) {
        return HttpUrl.parse(properties.getIptSearchUrl()).newBuilder()
                .addPathSegments("api/collections")
                .addPathSegment(collection)
                .addPathSegment("search.json")
                .addQueryParameter("maxRecords", "1")
                .addQueryParameter("productIdentifier", "%" + productId + "%")
                .build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class IptTokenResponse {
        private String token;
        private String tokenIdentity;
        private String userName;
        private String userId;
        private String objectStoreUrl;
    }

    @Value
    @Builder
    public static final class Properties {
        private String iptSearchUrl;
        private String iptDownloadUrl;
        private String authEndpoint;
        private String authDomain;
    }

}
