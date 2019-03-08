package com.cgi.eoss.ftep.clouds.ipt;

import com.cgi.eoss.ftep.clouds.ipt.persistence.Keypair;
import com.cgi.eoss.ftep.clouds.ipt.persistence.KeypairRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(value = "ftep.clouds.ipt.enabled", havingValue = "true")
@EnableJpaRepositories(basePackageClasses = KeypairRepository.class,
        entityManagerFactoryRef = "iptEntityManager",
        transactionManagerRef = "iptTransactionManager")
@EntityScan(basePackageClasses = Keypair.class)
public class IptPersistenceConfiguration {

    @Bean
    @ConfigurationProperties("ftep.clouds.ipt.datasource")
    public DataSourceProperties iptDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource iptDataSource() {
        return iptDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean iptEntityManager() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();

        factoryBean.setDataSource(iptDataSource());
        factoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        factoryBean.setPackagesToScan(IptPersistenceConfiguration.class.getPackage().getName());

        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager iptTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(iptEntityManager().getObject());
        return transactionManager;
    }

}
