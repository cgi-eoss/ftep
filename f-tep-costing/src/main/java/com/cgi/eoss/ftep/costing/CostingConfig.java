package com.cgi.eoss.ftep.costing;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.cgi.eoss.ftep.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.WalletDataService;

import org.springframework.beans.factory.annotation.Value;
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
    public CostingService costingService(ExpressionParser costingExpressionParser,
            CostingExpressionDataService costingDataService,
            WalletDataService walletDataService,
            DatabasketDataService databasketDataService,
            @Value("${ftep.costing.defaultJobCostExpression:1}") String defaultJobCostExpression,
            @Value("${ftep.costing.defaultDownloadCostExpression:1}") String defaultDownloadCostExpression) {
        return new CostingServiceImpl(costingExpressionParser, costingDataService, walletDataService, databasketDataService, defaultJobCostExpression, defaultDownloadCostExpression);
    }
}
