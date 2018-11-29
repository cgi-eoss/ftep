package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.catalogue.CatalogueConfig;
import com.cgi.eoss.ftep.costing.CostingConfig;
import com.cgi.eoss.ftep.metrics.MetricsConfig;
import com.cgi.eoss.ftep.orchestrator.service.WorkerFactory;
import com.cgi.eoss.ftep.queues.QueuesConfig;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.cgi.eoss.ftep.persistence.service.WorkerLocatorExpressionDataService;
import com.cgi.eoss.ftep.rpc.InProcessRpcConfig;
import com.cgi.eoss.ftep.search.SearchConfig;
import com.cgi.eoss.ftep.security.SecurityConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * <p>Spring configuration for the F-TEP Orchestrator component.</p>
 * <p>Manages access to distributed workers and provides RPC services.</p>
 */
@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,

        CatalogueConfig.class,
        CostingConfig.class,
        InProcessRpcConfig.class,
        MetricsConfig.class,
        PersistenceConfig.class,
        QueuesConfig.class,
        SearchConfig.class,
        SecurityConfig.class
})
@EnableEurekaClient
@ComponentScan(basePackageClasses = OrchestratorConfig.class)
public class OrchestratorConfig {

    @Bean
    public ExpressionParser workerLocatorExpressionParser() {
        return new SpelExpressionParser();
    }

    @Bean
    public WorkerFactory workerFactory(DiscoveryClient discoveryClient,
            @Value("${ftep.orchestrator.worker.eurekaServiceId:f-tep worker}") String workerServiceId,
            ExpressionParser workerLocatorExpressionParser,
            WorkerLocatorExpressionDataService workerLocatorExpressionDataService,
            @Value("${ftep.orchestrator.worker.defaultWorkerExpression:\"LOCAL\"}") String defaultWorkerExpression) {
        return new WorkerFactory(discoveryClient, workerServiceId, workerLocatorExpressionParser, workerLocatorExpressionDataService, defaultWorkerExpression);
    }
}
