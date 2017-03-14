package com.cgi.eoss.ftep.zoomanager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ComponentScan(
        basePackageClasses = ZooManagerConfig.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ZooManagerApplication.class)
)
public class ZooManagerConfig {

    @Bean
    public Path zcfgBasePath(@Value("${ftep.zoomanager.zcfg.path:/var/www/cgi-bin}") String zcfgBasePath) {
        return Paths.get(zcfgBasePath);
    }

    @Bean
    public Path zooServicesStubJar(@Value("${ftep.zoomanager.stub.jarFile:/var/www/cgi-bin/jars/f-tep-services.jar}") String zooServicesStubJar) {
        return Paths.get(zooServicesStubJar);
    }

}
