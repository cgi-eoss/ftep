package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.catalogue.CatalogueConfig;
import com.cgi.eoss.ftep.orchestrator.service.FtepServiceLauncher;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * <p>Spring configuration for the F-TEP Orchestrator component.</p>
 * <p>Manages access to distributed workers and provides the {@link FtepServiceLauncher} RPC service.</p>
 */
@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        PersistenceConfig.class,
        CatalogueConfig.class
})
@EnableEurekaClient
@ComponentScan(basePackageClasses = OrchestratorConfig.class)
public class OrchestratorConfig {

}
