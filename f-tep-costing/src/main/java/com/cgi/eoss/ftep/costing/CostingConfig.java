package com.cgi.eoss.ftep.costing;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,

        PersistenceConfig.class
})
public class CostingConfig {

    @Bean
    public ExpressionParser costingExpressionParser() {
        return new SpelExpressionParser();
    }

    @Bean
    public CostingService costingService(ExpressionParser costingExpressionParser) {
        return new CostingServiceImpl(costingExpressionParser);
    }

}
