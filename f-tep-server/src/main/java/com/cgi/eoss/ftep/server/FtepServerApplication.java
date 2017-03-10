package com.cgi.eoss.ftep.server;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.catalogue.CatalogueConfig;
import com.cgi.eoss.ftep.orchestrator.OrchestratorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * <p>Application running the F-TEP orchestrator and associated "master" services.</p>
 */
@Import({
        ApiConfig.class,
        CatalogueConfig.class,
        OrchestratorConfig.class
})
@SpringBootApplication(scanBasePackageClasses = FtepServerApplication.class)
public class FtepServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FtepServerApplication.class, args);
    }

}
