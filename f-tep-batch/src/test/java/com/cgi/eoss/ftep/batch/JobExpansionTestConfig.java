package com.cgi.eoss.ftep.batch;

import com.cgi.eoss.ftep.search.api.SearchFacade;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

public class JobExpansionTestConfig {

    @Bean
    public SearchFacade searchFacade() {
        return mock(SearchFacade.class);
    }

}
