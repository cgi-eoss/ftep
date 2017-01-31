package com.cgi.eoss.ftep.persistence;

import com.google.common.collect.ImmutableMap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = "com.cgi.eoss.ftep.persistence.dao",
        excludeFilters = {@ComponentScan.Filter(SpringJpaRepositoryIgnore.class)})
@EnableTransactionManagement
@EntityScan(basePackages = "com.cgi.eoss.ftep.model")
@ComponentScan("com.cgi.eoss.ftep.persistence")
public class PersistenceConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${jdbc.url}") String url,
            @Value("${jdbc.user}") String user,
            @Value("${jdbc.password}") String password,
            @Value("${jdbc.dataSourceClassName}") String dataSourceClassName) {
        Properties dataSourceProperties = new Properties();
        dataSourceProperties.putAll(ImmutableMap.builder()
                .put("url", url)
                .build());

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceProperties(dataSourceProperties);
        hikariConfig.setDataSourceClassName(dataSourceClassName);
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(8);

        return new HikariDataSource(hikariConfig);
    }

}
