package com.cgi.eoss.ftep.metrics;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.persistence.EntityManager;

@Configuration
@Import({
        PersistenceConfig.class
})
public class MetricsConfig {

    @Bean
    public FtepServiceExecutionMetrics ftepServiceExecutionMetrics(EntityManager em) {
        return new FtepServiceExecutionMetrics(em);
    }

    @Bean
    public FtepUserMetrics ftepUserMetrics(EntityManager em) {
        return new FtepUserMetrics(em);
    }

}
