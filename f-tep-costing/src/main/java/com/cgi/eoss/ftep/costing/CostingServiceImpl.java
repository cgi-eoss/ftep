package com.cgi.eoss.ftep.costing;

import com.cgi.eoss.ftep.model.CostingExpression;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.model.WalletTransaction;
import com.cgi.eoss.ftep.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.ftep.persistence.service.WalletDataService;
import com.google.common.base.Strings;
import org.springframework.expression.ExpressionParser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * <p>Default implementation of {@link CostingService}.</p>
 */
public class CostingServiceImpl implements CostingService {

    private final ExpressionParser expressionParser;
    private final CostingExpressionDataService costingDataService;
    private final WalletDataService walletDataService;
    private final CostingExpression defaultJobCostingExpression;
    private final CostingExpression defaultDownloadCostingExpression;

    public CostingServiceImpl(ExpressionParser costingExpressionParser, CostingExpressionDataService costingDataService,
                              WalletDataService walletDataService, String defaultJobCostingExpression, String defaultDownloadCostingExpression) {
        this.expressionParser = costingExpressionParser;
        this.costingDataService = costingDataService;
        this.walletDataService = walletDataService;
        this.defaultJobCostingExpression = CostingExpression.builder().costExpression(defaultJobCostingExpression).build();
        this.defaultDownloadCostingExpression = CostingExpression.builder().costExpression(defaultDownloadCostingExpression).build();
    }

    @Override
    public Integer estimateJobCost(JobConfig jobConfig) {
        CostingExpression costingExpression = getCostingExpression(jobConfig.getService());

        String expression = Strings.isNullOrEmpty(costingExpression.getEstimatedCostExpression())
                ? costingExpression.getCostExpression()
                : costingExpression.getEstimatedCostExpression();

        return ((Number) expressionParser.parseExpression(expression).getValue(jobConfig)).intValue();
    }

    @Override
    public Integer estimateDownloadCost(FtepFile ftepFile) {
        CostingExpression costingExpression = getCostingExpression(ftepFile);

        String expression = Strings.isNullOrEmpty(costingExpression.getEstimatedCostExpression())
                ? costingExpression.getCostExpression()
                : costingExpression.getEstimatedCostExpression();

        return ((Number) expressionParser.parseExpression(expression).getValue(ftepFile)).intValue();
    }

    @Override
    @Transactional
    public void chargeForJob(Wallet wallet, Job job) {
        CostingExpression costingExpression = getCostingExpression(job.getConfig().getService());

        String expression = costingExpression.getCostExpression();

        int cost = ((Number) expressionParser.parseExpression(expression).getValue(job)).intValue();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.JOB)
                .associatedId(job.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
    }

    @Override
    @Transactional
    public void chargeForDownload(Wallet wallet, FtepFile ftepFile) {
        CostingExpression costingExpression = getCostingExpression(ftepFile);

        String expression = costingExpression.getCostExpression();

        int cost = ((Number) expressionParser.parseExpression(expression).getValue(ftepFile)).intValue();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.DOWNLOAD)
                .associatedId(ftepFile.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
    }

    private CostingExpression getCostingExpression(FtepService ftepService) {
        return Optional.ofNullable(costingDataService.getServiceCostingExpression(ftepService)).orElse(defaultJobCostingExpression);
    }

    private CostingExpression getCostingExpression(FtepFile ftepFile) {
        return Optional.ofNullable(costingDataService.getDownloadCostingExpression(ftepFile)).orElse(defaultDownloadCostingExpression);
    }

}
