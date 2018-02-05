package com.cgi.eoss.ftep.clouds;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
})
@ComponentScan(basePackageClasses = CloudsConfig.class)
public class CloudsConfig {
}
