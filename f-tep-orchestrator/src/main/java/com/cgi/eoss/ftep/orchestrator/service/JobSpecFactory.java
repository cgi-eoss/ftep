package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.JobParam;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

@Service
@Log4j2
public class JobSpecFactory {

    private static final String MAGIC_PARALLEL_PARAM_KEY = "parallelInputs";
    private static final String MAGIC_TIMEOUT_PARAM_KEY = "timeout";
    private static final String DATABASKET_URI_REGEX = "ftep://databasket/(\\d+)";

    private final SearchFacade searchFacade;
    private final JobDataService jobDataService;
    private final DatabasketDataService databasketDataService;

    @Autowired
    public JobSpecFactory(SearchFacade searchFacade, JobDataService jobDataService, DatabasketDataService databasketDataService) {
        this.searchFacade = searchFacade;
        this.jobDataService = jobDataService;
        this.databasketDataService = databasketDataService;
    }

    /**
     * <p>Expand a service launching request into its decomposed job specifications. This includes:</p>
     * <ol>
     * <li>Evaluate JobParams with searchParameter=true and replace the values with the search results</li>
     * <li>Evaluate JobParams with parallelParameter=true and expand to one JobSpec per value</li>
     * </ol>
     *
     * @param request The service launch request to be expanded.
     * @return The JobSpecs representing the expanded configuration.
     */
    public List<JobSpec> expandJobParams(FtepServiceParams request) {
        String zooId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();
        String jobConfigLabel = request.getJobConfigLabel();
        String parentId = request.getJobParent();
        List<JobParam> originalParams = request.getInputsList();
        String systematicParameter = request.getSystematicParameter();
        List<String> parallelParameters = request.getParallelParametersList();
        List<String> searchParameters = request.getSearchParametersList();

        // Replace any "dynamic" parameter values, e.g. search parameters, and evaluate Databasket contents
        List<JobParam> populatedParams = originalParams.stream()
                .map(this::evaluateSearchParam)
                .map(this::evaluateDatabasket)
                .collect(toList());

        // Expand parallel parameters
        List<List<JobParam>> expandedParams = expandParallelParams(populatedParams);

        Optional<Job> parentJob;
        if (!Strings.isNullOrEmpty(parentId)) {
            LOG.debug("Reattaching child job(s) to parent {}", parentId);
            parentJob = Optional.of(jobDataService.reload(Long.valueOf(parentId)));
        } else if (expandedParams.size() > 1) {
            LOG.debug("Creating new parent for {} child jobs", expandedParams.size());
            Job newParentJob = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, GrpcUtil.paramsListToMap(populatedParams), systematicParameter, parallelParameters, searchParameters);
            newParentJob.setParent(true);
            parentJob = Optional.of(jobDataService.save(newParentJob));
        } else {
            parentJob = Optional.empty();
        }

        return expandedParams.stream()
                .map(params -> parentJob
                        .map(parent -> jobDataService.buildNew(UUID.randomUUID().toString(), userId, serviceId, parent.getConfig().getLabel(), GrpcUtil.paramsListToMap(params), parent, systematicParameter, parallelParameters, searchParameters))
                        .orElseGet(() -> jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, GrpcUtil.paramsListToMap(params), systematicParameter, parallelParameters, searchParameters)))
                .map(this::jobToSpec)
                .collect(toList());
    }

    private JobParam evaluateSearchParam(JobParam param) {
        if (!param.getSearchParameter()) {
            return param;
        }

        LOG.debug("Evaluating search-based input parameter: {}", param);

        ImmutableListMultimap.Builder<String, String> paramParameters = ImmutableListMultimap.<String, String>builder();
        for (String value : param.getParamValueList()) {
            String[] searchKeyValue = safeDecodeSearchParameter(value);
            paramParameters.put(searchKeyValue[0], searchKeyValue[1]);
        }

        try {
            SearchParameters searchParameters = new SearchParameters();
            searchParameters.setRequestUrl(SearchParameters.DEFAULT_REQUEST_URL);
            searchParameters.setParameters(paramParameters.build());

            SearchResults results = searchFacade.search(searchParameters);
            List<String> resultUris = results.getFeatures().stream().map(f -> f.getProperty("ftepUrl").toString()).collect(toList());

            LOG.debug("Evaluated search-based input parameter to URIs: {}", resultUris);

            JobParam.Builder evaluatedSearchResults = param.toBuilder().clearParamValue();
            evaluatedSearchResults.addAllParamValue(resultUris);
            return evaluatedSearchResults.build();
        } catch (IOException e) {
            throw new ServiceExecutionException("Could not evaluate search to expand job parameters", e);
        }
    }

    private String[] safeDecodeSearchParameter(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString()).split("=");
        } catch (UnsupportedEncodingException e) {
            throw new ServiceExecutionException("Could not expand search parameter: " + value, e);
        }
    }

    private JobParam evaluateDatabasket(JobParam param) {
        if (!param.getParallelParameter()) {
            return param;
        }

        Set<String> paramValues = new LinkedHashSet<>();

        for (String value : param.getParamValueList()) {
            if (value.matches(DATABASKET_URI_REGEX)) {
                Long databasketId = Long.parseLong(value.substring((value.lastIndexOf("/") + 1)));
                LOG.debug("Evaluating the contents of databasket with id " + databasketId);
                Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId)).orElseThrow(() -> new RuntimeException("Failed to load databasket for ID " + databasketId));
                List<String> fileUris = databasket.getFiles().stream()
                        .map(file -> file.getUri().toString())
                        .collect(toList());

                paramValues.addAll(fileUris);
            } else {
                paramValues.add(value);
            }
        }
        return param.toBuilder().clearParamValue().addAllParamValue(paramValues).build();
    }

    private List<List<JobParam>> expandParallelParams(List<JobParam> originalParams) {
        ImmutableList.Builder<List<JobParam>> expandedParams = ImmutableList.builder();

        // Split the original parameters up into static and parallel sets
        Map<Boolean, List<JobParam>> staticAndParallelParams = originalParams.stream()
                .collect(partitioningBy(param -> param.getParallelParameter() || param.getParamName().equals(MAGIC_PARALLEL_PARAM_KEY)));

        List<JobParam> staticParams = staticAndParallelParams.get(false);
        List<JobParam> parallelParams = staticAndParallelParams.get(true);

        if (parallelParams.isEmpty()) {
            expandedParams.add(staticParams);
        } else {
            parallelParams.stream()
                    .peek(p -> LOG.info("Expanding parallel parameter {}", p.getParamName()))
                    .forEach(parallelParam -> parallelParam.getParamValueList().stream()
                            .flatMap(v -> Arrays.stream(v.split(","))) // TODO Remove when values are reliably not comma-separated
                            .forEach(value -> expandedParams.add(ImmutableList.<JobParam>builder()
                                    .addAll(staticParams)
                                    .add(parallelParam.toBuilder().clearParamValue().addParamValue(value).build())
                                    .build())
                            ));
        }

        return expandedParams.build();
    }

    private JobSpec jobToSpec(Job job) {
        FtepService service = job.getConfig().getService();

        JobSpec.Builder jobSpecBuilder = JobSpec.newBuilder()
                .setService(GrpcUtil.toRpcService(service))
                .setJob(GrpcUtil.toRpcJob(job))
                .addAllInputs(GrpcUtil.mapToParams(job.getConfig().getInputs()));

        if (service.getType() == FtepService.Type.APPLICATION) {
            jobSpecBuilder.addExposedPorts(service.getApplicationPort());
        }

        job.getConfig().getInputs().entries().stream()
                .filter(e -> e.getKey().equals(MAGIC_TIMEOUT_PARAM_KEY)).findFirst()
                .ifPresent(e -> jobSpecBuilder.setHasTimeout(true).setTimeoutValue(Integer.parseInt(e.getValue())));

        return jobSpecBuilder.build();
    }

}
