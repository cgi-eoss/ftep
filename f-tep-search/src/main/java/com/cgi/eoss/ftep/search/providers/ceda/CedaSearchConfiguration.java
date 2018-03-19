package com.cgi.eoss.ftep.search.providers.ceda;

import com.cgi.eoss.ftep.catalogue.external.ExternalProductDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;

@Configuration
@ConditionalOnProperty(value = "ftep.search.ceda.enabled", havingValue = "true")
public class CedaSearchConfiguration {

    @Value("${ftep.search.ceda.baseUrl:http://opensearch.ceda.ac.uk/opensearch/json}")
    private String baseUrl;
    @Value("${ftep.search.ceda.ftpBaseUri:ftp://ftp.ceda.ac.uk}")
    private String ftpBaseUri;
    @Value("${ftep.search.ceda.username:}")
    private String username;
    @Value("${ftep.search.ceda.password:}")
    private String password;
    @Value("${ftep.search.ceda.usableProductsOnly:true}")
    private boolean usableProductsOnly;
    @Value("${ftep.search.ceda.quicklooksCacheDirectory:/data/ql/ceda}")
    private String quicklooksCacheDirectory;

    @Bean
    public CedaSearchProperties cedaSearchProperties() {
        return CedaSearchProperties.builder()
                .baseUrl(HttpUrl.parse(baseUrl))
                .ftpBaseUri(URI.create(ftpBaseUri))
                .username(username)
                .password(password)
                .usableProductsOnly(usableProductsOnly)
                .build();
    }

    @Bean
    public OkHttpClient cedaHttpClient(CedaSearchProperties searchProperties, OkHttpClient httpClient) {
        return httpClient.newBuilder()
                .addInterceptor(chain -> {
                    Request authenticatedRequest = chain.request().newBuilder()
                            .header("Authorization", Credentials.basic(searchProperties.getUsername(), searchProperties.getPassword()))
                            .build();
                    return chain.proceed(authenticatedRequest);
                })
                .build();
    }

    @Bean
    public CedaQuicklooksCache cedaQuicklooksCache(CedaSearchProperties cedaSearchProperties) throws IOException {
        return new CedaQuicklooksCache(cedaSearchProperties, quicklooksCacheDirectory);
    }

    @Bean
    public CedaSearchProvider restoSearchProvider(CedaSearchProperties cedaSearchProperties, OkHttpClient cedaHttpClient, ObjectMapper objectMapper, ExternalProductDataService externalProductService, CedaQuicklooksCache quicklooksCache) {
        return new CedaSearchProvider(0,
                cedaSearchProperties,
                cedaHttpClient,
                objectMapper,
                externalProductService, quicklooksCache);
    }

}
