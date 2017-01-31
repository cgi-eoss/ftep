package com.cgi.eoss.ftep.persistence;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
})
@EnableJpaRepositories(basePackages = "com.cgi.eoss.ftep.persistence.dao",
        excludeFilters = {@ComponentScan.Filter(SpringJpaRepositoryIgnore.class)})
@EnableTransactionManagement
@EntityScan(basePackages = "com.cgi.eoss.ftep.model")
@ComponentScan("com.cgi.eoss.ftep.persistence")
public class PersistenceConfig {

}
