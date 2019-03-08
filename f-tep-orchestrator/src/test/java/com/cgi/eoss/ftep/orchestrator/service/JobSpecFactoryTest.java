package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.orchestrator.OrchestratorConfig;
import com.cgi.eoss.ftep.orchestrator.OrchestratorTestConfig;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.JobParam;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.geojson.Feature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {OrchestratorConfig.class, OrchestratorTestConfig.class})
@TestPropertySource("classpath:test-orchestrator.properties")
public class JobSpecFactoryTest {

    private static final String PRODUCT_1_URI = "sentinel2:///S2B_MSIL1C_20181117T114349_N0207_R123_T30VUH_20181117T134439";
    private static final String PRODUCT_2_URI = "sentinel2:///S2B_MSIL1C_20190105T103429_N0207_R108_T33VVE_20190105T122413";

    @Autowired
    private JobSpecFactory jobSpecFactory;
    @Autowired
    private SearchFacade searchFacade;
    @Autowired
    private UserDataService userDataService;
    @Autowired
    private ServiceDataService serviceDataService;
    @Autowired
    private JobDataService jobDataService;

    private User ftepAdmin;
    private FtepService ftepService;

    @Before
    public void setUp() throws Exception {
        ftepAdmin = userDataService.getOrSave("ftep-admin");
        ftepService = Optional.ofNullable(serviceDataService.getByName("ftepService"))
                .orElseGet(() -> serviceDataService.save(new FtepService("ftepService", ftepAdmin, "dockerTag")));

        Feature feature1 = new Feature();
        feature1.setProperty("ftepUrl", PRODUCT_1_URI);
        Feature feature2 = new Feature();
        feature2.setProperty("ftepUrl", PRODUCT_2_URI);

        when(searchFacade.search(any())).thenReturn(
                SearchResults.builder()
                        .features(ImmutableList.<Feature>builder()
                                .add(feature1)
                                .add(feature2)
                                .build())
                        .build());
    }

    @Test
    public void testExpandJobParamsNoDynamic() throws Exception {
        String jobId = UUID.randomUUID().toString();
        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(ftepAdmin.getName())
                .setServiceId(ftepService.getName())
                .addInputs(JobParam.newBuilder().setParamName("input1").addParamValue("foo").build())
                .addInputs(JobParam.newBuilder().setParamName("input2").addParamValue("bar1").addParamValue("bar2").build())
                .build();

        List<JobSpec> jobSpecs = jobSpecFactory.expandJobParams(request);
        assertThat(jobSpecs, hasSize(1));

        JobSpec jobSpec = jobSpecs.get(0);
        assertThat(jobSpec.getJob().getId(), is(jobId));
        assertThat(jobSpec.getJob().getUserId(), is(ftepAdmin.getName()));
        assertThat(jobSpec.getJob().getServiceId(), is(ftepService.getName()));
        assertThat(jobSpec.getInputsCount(), is(2));
        Multimap<String, String> params = GrpcUtil.paramsListToMap(jobSpec.getInputsList());
        assertThat(params.get("input1"), is(ImmutableList.of("foo")));
        assertThat(params.get("input2"), is(ImmutableList.of("bar1", "bar2")));
    }

    @Test
    public void testExpandJobParamsToMultipleNoParallel() throws Exception {
        String jobId = UUID.randomUUID().toString();
        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(ftepAdmin.getName())
                .setServiceId(ftepService.getName())
                .addInputs(JobParam.newBuilder().setParamName("input1").addParamValue("foo").build())
                .addInputs(JobParam.newBuilder().setParamName("input2")
                        .setSearchParameter(true)
                        .addParamValue("searchparam1=a")
                        .addParamValue("searchparam2=b")
                        .addParamValue("searchparam3=c")
                        .build())
                .build();

        List<JobSpec> jobSpecs = jobSpecFactory.expandJobParams(request);
        assertThat(jobSpecs, hasSize(1));

        JobSpec jobSpec = jobSpecs.get(0);
        assertThat(jobSpec.getJob().getId(), is(jobId));
        assertThat(jobSpec.getJob().getUserId(), is(ftepAdmin.getName()));
        assertThat(jobSpec.getJob().getServiceId(), is(ftepService.getName()));
        assertThat(jobSpec.getInputsCount(), is(2));
        Multimap<String, String> params = GrpcUtil.paramsListToMap(jobSpec.getInputsList());
        assertThat(params.get("input1"), is(ImmutableList.of("foo")));
        assertThat(params.get("input2"), is(ImmutableList.of(PRODUCT_1_URI, PRODUCT_2_URI)));
    }

    @Test
    public void testExpandJobParamsToMultipleParallel() throws Exception {
        String jobId = UUID.randomUUID().toString();
        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(ftepAdmin.getName())
                .setServiceId(ftepService.getName())
                .addInputs(JobParam.newBuilder().setParamName("input1").addParamValue("foo").build())
                .addInputs(JobParam.newBuilder().setParamName("input2")
                        .setParallelParameter(true)
                        .setSearchParameter(true)
                        .addParamValue("searchparam1=a")
                        .addParamValue("searchparam2=b")
                        .addParamValue("searchparam3=c")
                        .build())
                .build();

        List<JobSpec> jobSpecs = jobSpecFactory.expandJobParams(request);
        assertThat(jobSpecs, hasSize(2));

        JobSpec jobSpec1 = jobSpecs.get(0);
        assertThat(jobSpec1.getJob().getId(), is(not(jobId)));
        assertThat(jobSpec1.getJob().getUserId(), is(ftepAdmin.getName()));
        assertThat(jobSpec1.getJob().getServiceId(), is(ftepService.getName()));
        assertThat(jobSpec1.getInputsCount(), is(2));
        Multimap<String, String> params1 = GrpcUtil.paramsListToMap(jobSpec1.getInputsList());
        assertThat(params1.get("input1"), is(ImmutableList.of("foo")));
        assertThat(params1.get("input2"), is(ImmutableList.of(PRODUCT_1_URI)));

        JobSpec jobSpec2 = jobSpecs.get(1);
        assertThat(jobSpec2.getJob().getId(), is(not(jobId)));
        assertThat(jobSpec2.getJob().getUserId(), is(ftepAdmin.getName()));
        assertThat(jobSpec2.getJob().getServiceId(), is(ftepService.getName()));
        assertThat(jobSpec2.getInputsCount(), is(2));
        Multimap<String, String> params2 = GrpcUtil.paramsListToMap(jobSpec2.getInputsList());
        assertThat(params2.get("input1"), is(ImmutableList.of("foo")));
        assertThat(params2.get("input2"), is(ImmutableList.of(PRODUCT_2_URI)));

        Job parentJob = jobDataService.getByExtId(UUID.fromString(jobId));
        assertThat(parentJob.isParent(), is(true));
        Set<String> subJobIds = parentJob.getSubJobs().stream().map(Job::getExtId).collect(toSet());
        assertThat(subJobIds, is(ImmutableSet.of(jobSpec1.getJob().getId(), jobSpec2.getJob().getId())));
    }

}