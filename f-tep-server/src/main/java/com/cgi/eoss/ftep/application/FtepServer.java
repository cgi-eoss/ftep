package com.cgi.eoss.ftep.application;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.orchestrator.OrchestratorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.EndpointMBeanExportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * <p>Application running the F-TEP orchestrator and associated "master" services.</p>
 */
@Import({
        ApiConfig.class,
        OrchestratorConfig.class
})
@SpringBootApplication(scanBasePackageClasses = FtepServer.class, exclude = {
        EndpointMBeanExportAutoConfiguration.class
})
@PropertySource(value = "file:/var/f-tep/etc/f-tep-server.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:${user.home}/.config/f-tep/f-tep-server.properties", ignoreResourceNotFound = true)
public class FtepServer {
    public static void main(String[] args) {
        SpringApplication.run(FtepServer.class, args);
    }
}
