package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.cgi.eoss.ftep.rpc.RpcClientConfig;
import com.cgi.eoss.ftep.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        PersistenceConfig.class,
        SecurityConfig.class,
        RpcClientConfig.class
})
@ComponentScan(basePackageClasses = CatalogueConfig.class)
@EnableEurekaClient
public class CatalogueConfig {

    @Bean
    public Path outputProductBasedir(@Value("${ftep.catalogue.outputProducts.baseDir:/data/outputProducts}") String baseDir) {
        return Paths.get(baseDir);
    }

    @Bean
    public Path referenceDataBasedir(@Value("${ftep.catalogue.refData.baseDir:/data/refData}") String baseDir) {
        return Paths.get(baseDir);
    }

}
