package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.WalletTransaction;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.persistence.service.WalletDataService;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.io.CountingOutputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service to aggregate and analyze platform usage data, and generate reports.
 */
@Service
@Log4j2
public class ReportsCollector {
    private static final DateTimeFormatter GRAYLOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    // Database connection handler
    private final JobDataService jobDataService;

    // This singleton can talk to the Graylog server, handling authentication as well
    private final JobsApiExtension jobsApiExtension;

    private final UserDataService userDataService;
    private final WalletDataService walletDataService;

    @Autowired
    public ReportsCollector(JobDataService jobDataService, JobsApiExtension jobsApiExtension, UserDataService userDataService, WalletDataService walletDataService) {
        this.jobDataService = jobDataService;
        this.jobsApiExtension = jobsApiExtension;
        this.userDataService = userDataService;
        this.walletDataService = walletDataService;
    }

    /**
     * Fetches Job metrics from the database, filling the two parameters with
     * the results; and also returns the number of generated products.
     */
    private int fetchFromDatabase(YearMonth period,
                                  Map<UserService, JobMetrics> userPerServiceDetails,
                                  Map<String, Long> productList, Long userId) {

        List<Job> jobs = Optional.ofNullable(userId).isPresent()
                ? jobDataService.findByOwnerAndStartIn(userDataService.getById(userId), period)
                : jobDataService.findByStartIn(period);

        jobs.forEach(j -> {
            UserService userService = new UserService(j.getOwner().getEmail(), j.getOwner().getName(), j.getConfig().getService().getName());

            JobMetrics metrics = new JobMetrics(
                    1,
                    (null == j.getEndTime() || null == j.getStartTime()) ? 0 :
                            (j.getEndTime().toEpochSecond(ZoneOffset.UTC) - j.getStartTime().toEpochSecond(ZoneOffset.UTC)),
                    j.getConfig().getInputFiles().stream()
                            .mapToLong(f -> Optional.ofNullable(f.getFilesize()).orElse(0L))
                            .sum(),
                    j.getOutputFiles().stream()
                            .mapToLong(f -> Optional.ofNullable(f.getFilesize()).orElse(0L))
                            .sum(),
                    walletDataService.getLaunchTransactionByJobId(j.getId())
                            .map(WalletTransaction::getBalanceChange)
                            .orElse(0)
            );

            // Fill Map of job data per user per service

            if (null != (userPerServiceDetails.putIfAbsent(userService, metrics))) {
                ++(userPerServiceDetails.get(userService).totalNumOfJobsInService);
                userPerServiceDetails.get(userService).totalTime += metrics.totalTime;
                userPerServiceDetails.get(userService).totalInSize += metrics.totalInSize;
                userPerServiceDetails.get(userService).totalOutSize += metrics.totalOutSize;
                userPerServiceDetails.get(userService).totalLaunchCost += metrics.totalLaunchCost;
            }

            // Fill Map of unique products
            j.getConfig().getInputFiles().forEach(i -> productList.putIfAbsent(i.getUri().toString(), Optional.ofNullable(i.getFilesize()).orElse(0L)));
            j.getOutputFiles().forEach(o -> productList.putIfAbsent(o.getUri().toString(), Optional.ofNullable(o.getFilesize()).orElse(0L)));
        });

        // Return number of generated products
        return jobs.stream().mapToInt(j -> j.getOutputFiles().size()).sum();
    }

    /**
     * Fetches integer data from the Graylog search REST API, using the
     * JobsApiExtension.loadGraylogCustomSearch function which takes a custom
     * urlPath and query parameters. If the returned value is -1 then the
     * search was unsuccessful.
     */
    private int fetchJsonData(Map<String, String> qParameters) {
        Map<String, Object> jsonBody = jobsApiExtension.loadGraylogCustomSearch("search/universal/absolute", qParameters);
        return jsonBody.containsKey("total_results") ? (Integer) (jsonBody.get("total_results")) : -1;
    }

    /**
     * Retrieving uploaded reference data for a given year and month
     * @param period
     * @return
     */
    private Map<String, String> getParamUploadRefData(YearMonth period) {
        String urlPartUploadRefData = "response:201 AND request:\\/secure\\/api\\/v2.0\\/ftepFiles\\/refData AND verb:POST";
        Map<String, String> qParamUploadRefData = ImmutableMap.<String, String>builder()
                .put("fields", "timestamp")
                .put("from", period.atDay(1).atStartOfDay(ZoneOffset.UTC).format(GRAYLOG_DATE_FORMAT))
                .put("to", period.atEndOfMonth().plusDays(1).atStartOfDay(ZoneOffset.UTC).format(GRAYLOG_DATE_FORMAT))
                .put("limit", "1000")
                .put("query", urlPartUploadRefData)
                .build();
        return qParamUploadRefData;
    }

    /**
     * Retrieving downloaded products and reference data for a given year and month
     * @param period
     * @return
     */
    private Map<String, String> getParamDownloadProdAndRefData(YearMonth period) {
        String urlPartDownloadProdAndRefData = "response:200 AND request:\\/secure\\/api\\/v2.0\\/ftepFiles\\/*\\/dl";
        Map<String, String> qParamDownloadProdAndRefData = ImmutableMap.<String, String>builder()
                .put("fields", "timestamp")
                .put("from", period.atDay(1).atStartOfDay(ZoneOffset.UTC).format(GRAYLOG_DATE_FORMAT))
                .put("to", period.atEndOfMonth().plusDays(1).atStartOfDay(ZoneOffset.UTC).format(GRAYLOG_DATE_FORMAT))
                .put("limit", "1000")
                .put("query", urlPartDownloadProdAndRefData)
                .build();
        return qParamDownloadProdAndRefData;
    }

    /**
     * Generating a general report in the form of a UsageReport object
     * @param period
     * @return
     */

    public UsageReport generateUsageReportJson(YearMonth period) {
        return generateUsageReportJson(period, null);
    }

    /**
     * Generating a report for one user in the form of a UsageReport object
     * @param period
     * @param userId
     * @return
     */

    public UsageReport generateUsageReportJson(YearMonth period, Long userId) {

        // Fetch the data for populating the report

        Map<String, String> qParamUploadRefData = getParamUploadRefData(period);
        Map<String, String> qParamDownloadProdAndRefData = getParamDownloadProdAndRefData(period);

        int paramUploadRefData = fetchJsonData(qParamUploadRefData);
        int paramDownloadProdAndRefData = fetchJsonData(qParamDownloadProdAndRefData);

        Map<UserService, JobMetrics> userPerServiceDetails = new HashMap<>();
        Map<String, Long> productList = new HashMap<>();
        int generatedProducts = fetchFromDatabase(period, userPerServiceDetails, productList, userId);

        // Create, populate and return a UsageReport object

        UsageReport report = new UsageReport();
        List<UserService> userServices = userPerServiceDetails.keySet().stream()
                .map(key -> {
                    key.jobMetrics = userPerServiceDetails.get(key);
                    return key;
                })
                .collect(Collectors.toList());

        report.setUserServices(userServices);
        report.setTotalNumOfJobs(userServices.stream().mapToInt(userService -> userService.jobMetrics.totalNumOfJobsInService).sum());
        report.setTotalInSize(userServices.stream().mapToLong(userService -> userService.jobMetrics.totalInSize).sum());
        report.setTotalOutSize(userServices.stream().mapToLong(userService -> userService.jobMetrics.totalOutSize).sum());
        report.setTotalTime(userServices.stream().mapToLong(userService -> userService.jobMetrics.totalTime).sum());
        report.setGeneratedProducts(generatedProducts);
        report.setUploadedReferenceData(paramUploadRefData);
        report.setDownloadedProductsAndReferenceData(paramDownloadProdAndRefData);
        report.setUniqueProducts(productList);

        // Collect all download costs
        int totalDownloadCost = walletDataService.getDownloadTransactionsByTimeAndOwner(period, userId).stream()
                .mapToInt(WalletTransaction::getBalanceChange)
                .sum();
        report.setTotalDownloadCost(totalDownloadCost);

        // Collect all job launch costs
        int totalJobLaunchCost = userServices.stream()
                .mapToInt(userService -> Optional.ofNullable(userService.jobMetrics.totalLaunchCost).orElse(0))
                .sum();
        report.setTotalLaunchCost(totalJobLaunchCost);

        return report;
    }

    /**
     * Generate a general XLS report containing site usage data for all users
     * @param period
     * @param outputStream
     * @return
     */
    public long generateUsageReport(YearMonth period, OutputStream outputStream) {
        return generateUsageReport(period, outputStream, null);
    }


    /**
     * The main class-method that executes the other functions to retrieve data
     * and returns a byte-stream (excel sheet), filled with the monthly-statistics.
     * The return value is the content-length. If userId is filled, the report is generated
     * just for this user.
     */
    public long generateUsageReport(YearMonth period, OutputStream outputStream, Long userId) {
        long bodyLength = 0;

        // Fetch data

        Map<String, String> qParamUploadRefData = getParamUploadRefData(period);
        Map<String, String> qParamDownloadProdAndRefData = getParamDownloadProdAndRefData(period);

        Map<UserService, JobMetrics> userPerServiceDetails = new HashMap<>();
        Map<String, Long> productList = new HashMap<>();
        int generatedProducts = fetchFromDatabase(period, userPerServiceDetails, productList, userId);

        int paramUploadRefData = fetchJsonData(qParamUploadRefData);
        int paramDownloadProdAndRefData = fetchJsonData(qParamDownloadProdAndRefData);

        // Create the Excel spreadsheet

        Workbook excelWorkbook = new HSSFWorkbook();
        Sheet sheet1 = excelWorkbook.createSheet("Sheet1");
        Font boldNormalFont = excelWorkbook.createFont();
        boldNormalFont.setBold(true);
        boldNormalFont.setFontHeightInPoints((short) 10);
        boldNormalFont.setFontName("Arial");
        CellStyle boldNormalCellStyle = excelWorkbook.createCellStyle();
        boldNormalCellStyle.setFont(boldNormalFont);

        // Create enough rows to hold all userPerServiceDetails -or- products -or- the middle metrics, plus a few rows for headers and summaries
        int rowMax = IntStream.of(userPerServiceDetails.keySet().size(), productList.keySet().size()).max().orElse(5) + 20;
        List<Row> rows = new ArrayList<>();
        for (int counter = 0; counter < rowMax; counter++) {
            rows.add(sheet1.createRow(counter));
        }

        // Header row

        String[] row0columns = {
                "User", "Service", "Jobs", "Total time (s)", "Input size (B)", "Output size (B)", "Total job launch cost", "", "Products generated", "", "Unique product usage:", ""
        };
        for (int idx = 0; idx < row0columns.length; idx++) {
            Cell cell = rows.get(0).createCell(idx);
            cell.setCellValue(row0columns[idx]);
            cell.setCellStyle(boldNormalCellStyle);
        }

        // Per User list, left side

        int dataRowIdx = 0;
        for (UserService perServiceDetailKey : ImmutableList.sortedCopyOf(userPerServiceDetails.keySet())) {
            JobMetrics currentMetric = userPerServiceDetails.get(perServiceDetailKey);
            ++dataRowIdx;
            Cell cell1 = rows.get(dataRowIdx).createCell(0);
            Cell cell2 = rows.get(dataRowIdx).createCell(1);
            Cell cell3 = rows.get(dataRowIdx).createCell(2);
            Cell cell4 = rows.get(dataRowIdx).createCell(3);
            Cell cell5 = rows.get(dataRowIdx).createCell(4);
            Cell cell6 = rows.get(dataRowIdx).createCell(5);
            Cell cell7 = rows.get(dataRowIdx).createCell(6);
            cell1.setCellValue(perServiceDetailKey.getUserMail());
            cell2.setCellValue(perServiceDetailKey.getServiceName());
            cell3.setCellValue(currentMetric.totalNumOfJobsInService);
            cell4.setCellValue(currentMetric.totalTime);
            cell5.setCellValue(currentMetric.totalInSize);
            cell6.setCellValue(currentMetric.totalOutSize);
            cell7.setCellValue(currentMetric.totalLaunchCost);
        }

        // Per User summary, left side

        ++dataRowIdx;
        for (int i = 0; i < 5; i++) {
            Cell cellSum = rows.get(dataRowIdx + 1).createCell(i+2);
            if (dataRowIdx > 1) {
                char column = (char) (i + 2 + (int) 'A');
                cellSum.setCellFormula("SUM(" + column + "2:" + column + dataRowIdx + ")");
            }
            cellSum.setCellStyle(boldNormalCellStyle);
        }

        // Extra summarized data cells, middle

        Cell cellEx1 = rows.get(1).createCell(8);
        Cell cellEx3 = rows.get(3).createCell(8);
        Cell cellEx4 = rows.get(4).createCell(8);
        Cell cellEx6 = rows.get(6).createCell(8);
        Cell cellEx7 = rows.get(7).createCell(8);
        Cell cellEx9 = rows.get(9).createCell(8);
        Cell cellEx10 = rows.get(10).createCell(8);

        cellEx1.setCellValue(generatedProducts);

        cellEx3.setCellValue("Products & reference data downloaded");
        cellEx3.setCellStyle(boldNormalCellStyle);
        cellEx4.setCellValue(paramDownloadProdAndRefData);

        cellEx6.setCellValue("Reference data uploaded");
        cellEx6.setCellStyle(boldNormalCellStyle);
        cellEx7.setCellValue(paramUploadRefData);

        cellEx9.setCellValue("Total download cost");
        cellEx9.setCellStyle(boldNormalCellStyle);

        int totalDownloadCost = walletDataService.getDownloadTransactionsByTimeAndOwner(period, userId).stream()
                .mapToInt(WalletTransaction::getBalanceChange)
                .sum();
        cellEx10.setCellValue(totalDownloadCost);

        // Product Unique List, right side

        dataRowIdx = 0;
        for (String productPath : ImmutableList.sortedCopyOf(productList.keySet())) {
            ++dataRowIdx;
            Cell cellUn1 = rows.get(dataRowIdx).createCell(10);
            Cell cellUn2 = rows.get(dataRowIdx).createCell(11);
            cellUn1.setCellValue(productPath);
            cellUn2.setCellValue(productList.get(productPath));
        }

        ++dataRowIdx;
        Cell cellUn3 = rows.get(dataRowIdx + 1).createCell(11);
        Cell cellUn4 = rows.get(dataRowIdx + 1).createCell(12);
        if (dataRowIdx > 1) {
            cellUn3.setCellFormula("SUM(L2:L" + dataRowIdx + ")");
        }
        cellUn3.setCellStyle(boldNormalCellStyle);
        cellUn4.setCellValue("Total unique product size (Bytes)");
        cellUn4.setCellStyle(boldNormalCellStyle);

        // Finally evaluate formulas and size the columns

        HSSFFormulaEvaluator.evaluateAllFormulaCells(excelWorkbook);
        for (int colIdx = 0; colIdx < 7; colIdx++) {
            sheet1.autoSizeColumn(colIdx);
        }
        sheet1.autoSizeColumn(8);
        sheet1.autoSizeColumn(10);
        sheet1.autoSizeColumn(11);

        // Stream the downloadable content

        try (CountingOutputStream writtenStream = new CountingOutputStream(outputStream)) {
            excelWorkbook.write(writtenStream);
            bodyLength = writtenStream.getCount();
            excelWorkbook.close();
        } catch (IOException ioe) {
            LOG.info("Problem while preparing the downloadable body content. Exception:\n" + ioe.getMessage());
        }
        return bodyLength;
    }

    /**
     * Custom nested class to obtain metrics from the Jobs
     */
    @AllArgsConstructor
    private static final class JobMetrics {
        public int totalNumOfJobsInService;
        public long totalTime;
        public long totalInSize;
        public long totalOutSize;
        public int totalLaunchCost;
    }

    @Data
    private static final class UserService implements Comparable<UserService> {
        private String userMail;
        private String userName;
        private String serviceName;
        private JobMetrics jobMetrics;

        private UserService(String userMail, String userName, String serviceName) {
            this.userMail = userMail;
            this.userName = userName;
            this.serviceName = serviceName;
        }

        @Override
        public int compareTo(UserService o) {
            return ComparisonChain.start()
                    .compare(userMail, o.userMail, Ordering.natural().nullsFirst())
                    .compare(userName, o.userName)
                    .compare(serviceName, o.serviceName)
                    .result();
        }
    }

    @Data
    public final class UsageReport {
        private List<UserService> userServices;
        private int totalNumOfJobs;
        private long totalTime;
        private long totalInSize;
        private long totalOutSize;
        private int generatedProducts;
        private int uploadedReferenceData;
        private int downloadedProductsAndReferenceData;
        private int totalLaunchCost;
        private int totalDownloadCost;
        private Map<String, Long> uniqueProducts;
    }

}
