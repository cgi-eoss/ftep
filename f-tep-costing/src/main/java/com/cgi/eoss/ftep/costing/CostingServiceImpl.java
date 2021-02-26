package com.cgi.eoss.ftep.costing;

import com.cgi.eoss.ftep.batch.service.JobExpansionService;
import com.cgi.eoss.ftep.model.CostingExpression;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.model.WalletTransaction;
import com.cgi.eoss.ftep.model.internal.CostQuotation;
import com.cgi.eoss.ftep.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.WalletDataService;
import com.google.common.base.Strings;
import org.springframework.expression.ExpressionParser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Default implementation of {@link CostingService}.</p>
 */
public class CostingServiceImpl implements CostingService {

    private final boolean coinsDisabled;
    private final CostingExpression defaultDownloadCostingExpression;
    private final CostingExpression defaultJobCostingExpression;
    private final CostingExpressionDataService costingDataService;
    private final DatabasketDataService databasketDataService;
    private final ExpressionParser expressionParser;
    private final WalletDataService walletDataService;
    private final JobExpansionService jobExpansionService;

    public CostingServiceImpl(boolean coinsDisabled, ExpressionParser costingExpressionParser, CostingExpressionDataService costingDataService,
                              WalletDataService walletDataService, DatabasketDataService databasketDataService, String defaultJobCostingExpression, String defaultDownloadCostingExpression, JobExpansionService jobExpansionService) {
        this.coinsDisabled = coinsDisabled;
        this.expressionParser = costingExpressionParser;
        this.costingDataService = costingDataService;
        this.walletDataService = walletDataService;
        this.databasketDataService = databasketDataService;
        this.defaultJobCostingExpression = CostingExpression.builder().costExpression(defaultJobCostingExpression).build();
        this.defaultDownloadCostingExpression = CostingExpression.builder().costExpression(defaultDownloadCostingExpression).build();
        this.jobExpansionService = jobExpansionService;
    }

    @Override
    public Integer estimateJobCost(JobConfig jobConfig) {
        return estimateJobCost(jobConfig, false);
    }

    @Override
    public Integer estimateJobCost(JobConfig jobConfig, boolean postLaunch) {
        if (coinsDisabled) {
            return 0;
        }

        CostingExpression costingExpression = getCostingExpression(jobConfig.getService());
        String expression = Strings.isNullOrEmpty(costingExpression.getEstimatedCostExpression())
                ? costingExpression.getCostExpression()
                : costingExpression.getEstimatedCostExpression();

        return jobExpansionService.expandJobParamsFromJobConfig(jobConfig, postLaunch).stream()
                .mapToInt(config -> ((Number) expressionParser.parseExpression(expression).getValue(jobConfig)).intValue())
                .sum();
    }

    @Override
    public CostQuotation estimateSystematicJobCost(JobConfig jobConfig) {
        if (coinsDisabled) {
            return new CostQuotation(0, CostQuotation.Recurrence.ONE_OFF);
        }

        Optional<CostingExpression> serviceCostingExpression = costingDataService.getServiceCostingExpression(jobConfig.getService());

        CostQuotation.Recurrence recurrence;
        CostingExpression jobCostingExpression;
        if (serviceCostingExpression.isPresent()) {
            recurrence = CostQuotation.Recurrence.ONE_OFF;
            jobCostingExpression = serviceCostingExpression.get();
        } else {
            recurrence = CostQuotation.Recurrence.HOURLY;
            jobCostingExpression = defaultJobCostingExpression;
        }

        String estimateExpression = Strings.isNullOrEmpty(jobCostingExpression.getEstimatedCostExpression())
                ? jobCostingExpression.getCostExpression()
                : jobCostingExpression.getEstimatedCostExpression();
        int cost = ((Number) expressionParser.parseExpression(estimateExpression).getValue(jobConfig)).intValue();
        return new CostQuotation(cost, recurrence);
    }

    @Override
    public Integer estimateDownloadCost(FtepFile ftepFile) {
        if (coinsDisabled) {
            return 0;
        }

        CostingExpression costingExpression = getCostingExpression(ftepFile);
        String expression = Strings.isNullOrEmpty(costingExpression.getEstimatedCostExpression())
                ? costingExpression.getCostExpression()
                : costingExpression.getEstimatedCostExpression();
        return ((Number) expressionParser.parseExpression(expression).getValue(ftepFile)).intValue();
    }

    private int calculateNumberOfInputs(Collection<String> inputUris) {
        int inputCount = 1;
        for (String inputUri : inputUris) {
            if (inputUri.startsWith("ftep://databasket")) {
                Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(inputUri);
                if (!uriIdMatcher.matches()) {
                    throw new RuntimeException("Failed to load databasket for URI: " + inputUri);
                }
                Long databasketId = Long.parseLong(uriIdMatcher.group(1));
                Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId)).orElseThrow(() -> new RuntimeException("Failed to load databasket for ID " + databasketId));
                inputCount = databasket.getFiles().size();
            } else if (inputUri.contains((","))) {
                inputCount = inputUri.split(",").length;
            } else {
                inputCount = 1;
            }
        }
        return inputCount;
    }

    @Override
    @Transactional
    public void chargeForJob(Wallet wallet, Job job) {
        if (coinsDisabled) {
            return;
        }

        CostingExpression costingExpression = getCostingExpression(job.getConfig().getService());
        String expression = costingExpression.getCostExpression();

        int cost = ((Number) expressionParser.parseExpression(expression).getValue(job)).intValue();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.JOB)
                .associatedId(job.getId())
                .transactionTime(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        walletDataService.transact(walletTransaction);
    }

    @Override
    @Transactional
    public void chargeForDownload(Wallet wallet, FtepFile ftepFile) {
        if (coinsDisabled) {
            return;
        }

        CostingExpression costingExpression = getCostingExpression(ftepFile);
        String expression = costingExpression.getCostExpression();

        int cost = ((Number) expressionParser.parseExpression(expression).getValue(ftepFile)).intValue();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.DOWNLOAD)
                .associatedId(ftepFile.getId())
                .transactionTime(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        walletDataService.transact(walletTransaction);
    }

    private CostingExpression getCostingExpression(FtepService ftepService) {
        return costingDataService.getServiceCostingExpression(ftepService).orElse(defaultJobCostingExpression);
    }

    private CostingExpression getCostingExpression(FtepFile ftepFile) {
        return costingDataService.getDownloadCostingExpression(ftepFile).orElse(defaultDownloadCostingExpression);
    }
}
