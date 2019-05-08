package com.cgi.eoss.ftep.batch;

import com.cgi.eoss.ftep.catalogue.CatalogueConfig;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.search.SearchConfig;
import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.batch.service.JobExpansionService;
import com.cgi.eoss.ftep.batch.service.JobExpansionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        CatalogueConfig.class,
        SearchConfig.class
})
public class JobExpansionConfig {

    @Bean
    public JobExpansionService batchService(SearchFacade searchFacade, JobDataService jobDataService, DatabasketDataService databasketDataService) {
        return new JobExpansionServiceImpl(searchFacade, jobDataService, databasketDataService);
    }

}
