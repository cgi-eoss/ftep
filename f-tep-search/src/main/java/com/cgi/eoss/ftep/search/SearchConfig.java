package com.cgi.eoss.ftep.search;

import com.cgi.eoss.ftep.catalogue.CatalogueConfig;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.search.api.SearchProvider;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collection;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,

        CatalogueConfig.class,
        PersistenceConfig.class
})
@ComponentScan(basePackageClasses = SearchConfig.class)
public class SearchConfig {

    @Bean
    public SearchFacade searchFacade(Collection<SearchProvider> searchProviders) {
        return new SearchFacade(searchProviders);
    }

    @Bean
    public OkHttpClient httpClient() {
        return new OkHttpClient.Builder()
                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.logica.com", 80)))
                .build();
    }

}
