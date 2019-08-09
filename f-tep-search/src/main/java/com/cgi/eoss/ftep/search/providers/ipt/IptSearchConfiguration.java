package com.cgi.eoss.ftep.search.providers.ipt;

import com.cgi.eoss.ftep.catalogue.external.ExternalProductDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "ftep.search.ipt.enabled", havingValue = "true", matchIfMissing = true)
public class IptSearchConfiguration {

    @Value("${ipt.search.ipt.baseUrl:https://finder.eocloud.eu/resto/}")
    private String baseUrl;

    @Bean
    public IptSearchProvider iptSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, ExternalProductDataService externalProductService) {
        return new IptSearchProvider(1,
                IptSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .build(),
                httpClient,
                objectMapper,
                externalProductService);
    }

}
