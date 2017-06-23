package com.cgi.eoss.ftep.search;

import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.search.api.SearchProvider;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collection;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
})
@ComponentScan(basePackageClasses = SearchConfig.class)
public class SearchConfig {

    @Bean
    public SearchFacade searchFacade(Collection<SearchProvider> searchProviders) {
        return new SearchFacade(searchProviders);
    }

}
