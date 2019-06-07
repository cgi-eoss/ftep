package com.cgi.eoss.ftep.batch;

import com.cgi.eoss.ftep.catalogue.CatalogueConfig;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.rpc.worker.ResourceRequest;
import com.cgi.eoss.ftep.search.SearchConfig;
import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.batch.service.JobExpansionService;
import com.cgi.eoss.ftep.batch.service.JobExpansionServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Optional;

@Configuration
@Import({
        CatalogueConfig.class,
        SearchConfig.class
})
public class JobExpansionConfig {

    //use if user has provided property, else use Optional.empty();
    @Bean
    @ConditionalOnProperty("ftep.job.storage.size.gb")
    public ResourceRequest resourceRequest(@Value("${ftep.job.storage.size.gb:1}") int storageSize){
        return ResourceRequest.newBuilder().setStorage(storageSize).build();
    }

    @Bean
    public JobExpansionService batchService(SearchFacade searchFacade, JobDataService jobDataService, DatabasketDataService databasketDataService, Optional<ResourceRequest> jobStorageResourceRequest) {
        return new JobExpansionServiceImpl(searchFacade, jobDataService, databasketDataService, jobStorageResourceRequest);
    }

}
