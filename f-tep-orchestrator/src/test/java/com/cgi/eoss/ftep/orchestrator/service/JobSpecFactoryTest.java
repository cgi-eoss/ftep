package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.orchestrator.OrchestratorConfig;
import com.cgi.eoss.ftep.orchestrator.OrchestratorTestConfig;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
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

import java.net.URI;
import java.util.Collection;
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
    private static final String FILE_11_URI = "ftep://ftepFile11";
    private static final String FILE_12_URI = "ftep://ftepFile12";
    private static final String FILE_21_URI = "ftep://ftepFile21";
    private static final String FILE_22_URI = "ftep://ftepFile22";
    private static final String FILE_31_URI = "ftep://ftepFile31";
    private static final String FILE_32_URI = "ftep://ftepFile32";

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
    @Autowired
    private FtepFileDataService fileDataService;
    @Autowired
    private DatabasketDataService databasketDataService;

    private User ftepAdmin;
    private FtepService ftepService;
    private Databasket databasket1;
    private Databasket databasket2;

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

        FtepFile ftepFile11 = new FtepFile(URI.create(FILE_11_URI), UUID.randomUUID());
        ftepFile11.setOwner(ftepAdmin);

        FtepFile ftepFile12 = new FtepFile(URI.create(FILE_12_URI), UUID.randomUUID());
        ftepFile12.setOwner(ftepAdmin);

        FtepFile ftepFile21 = new FtepFile(URI.create(FILE_21_URI), UUID.randomUUID());
        ftepFile21.setOwner(ftepAdmin);

        FtepFile ftepFile22 = new FtepFile(URI.create(FILE_22_URI), UUID.randomUUID());
        ftepFile22.setOwner(ftepAdmin);

        FtepFile ftepFile31 = new FtepFile(URI.create(FILE_31_URI), UUID.randomUUID());
        ftepFile31.setOwner(ftepAdmin);

        FtepFile ftepFile32 = new FtepFile(URI.create(FILE_32_URI), UUID.randomUUID());
        ftepFile32.setOwner(ftepAdmin);

        fileDataService.save(ImmutableSet.of(ftepFile11, ftepFile12, ftepFile21, ftepFile22, ftepFile31, ftepFile32));

        databasket1 = new Databasket("Databasket1", ftepAdmin);
        databasket1.setFiles(ImmutableSet.of(ftepFile11, ftepFile12));

        databasket2 = new Databasket("Databasket2", ftepAdmin);
        databasket2.setFiles(ImmutableSet.of(ftepFile21, ftepFile22));

        databasketDataService.save(ImmutableSet.of(databasket1, databasket2));

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

    @Test
    public void testEvaluateDatabasketSingle() throws Exception {
        Long databasketId = databasket1.getId();
        String jobId = UUID.randomUUID().toString();

        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(ftepAdmin.getName())
                .setServiceId(ftepService.getName())
                .addInputs(JobParam.newBuilder()
                        .setParamName("input")
                        .setParallelParameter(true)
                        .setSearchParameter(false)
                        .addParamValue("ftep://databasket/" + databasketId)
                        .build())
                .build();

        List<JobSpec> jobSpecs = jobSpecFactory.expandJobParams(request);
        assertThat(jobSpecs, hasSize(2));

        for (JobSpec jobSpec : jobSpecs) {
            assertThat(jobSpec.getJob().getId(), is(not(jobId)));
            assertThat(jobSpec.getJob().getUserId(), is(ftepAdmin.getName()));
            assertThat(jobSpec.getJob().getServiceId(), is(ftepService.getName()));
            assertThat(jobSpec.getInputsCount(), is(1));

            JobParam jobParam = jobSpec.getInputsList().get(0);
            assertThat(jobParam.getParamName(), is("input"));
            assertThat(jobParam.getParamValueList(), hasSize(1));
        }

        Set<String> paramValueSet = jobSpecs.stream()
                .map(jobSpec -> GrpcUtil.paramsListToMap(jobSpec.getInputsList()).get("input"))
                .flatMap(Collection::stream)
                .collect(toSet());

        assertThat(paramValueSet, hasSize(2));
        assertThat(paramValueSet, is(ImmutableSet.of(FILE_11_URI, FILE_12_URI)));
    }

    @Test
    public void testEvaluateDatabasketMultiple() throws Exception {
        Long databasket1Id = databasket1.getId();
        Long databasket2Id = databasket2.getId();
        String jobId = UUID.randomUUID().toString();

        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(ftepAdmin.getName())
                .setServiceId(ftepService.getName())
                .addInputs(JobParam.newBuilder()
                        .setParamName("input")
                        .setParallelParameter(true)
                        .setSearchParameter(false)
                        .addParamValue("ftep://databasket/" + databasket1Id)
                        .addParamValue("ftep://databasket/" + databasket2Id)
                        .build())
                .build();

        List<JobSpec> jobSpecs = jobSpecFactory.expandJobParams(request);
        assertThat(jobSpecs, hasSize(4));

        for (JobSpec jobSpec : jobSpecs) {
            assertThat(jobSpec.getJob().getId(), is(not(jobId)));
            assertThat(jobSpec.getJob().getUserId(), is(ftepAdmin.getName()));
            assertThat(jobSpec.getJob().getServiceId(), is(ftepService.getName()));
            assertThat(jobSpec.getInputsCount(), is(1));

            JobParam jobParam = jobSpec.getInputsList().get(0);
            assertThat(jobParam.getParamName(), is("input"));
            assertThat(jobParam.getParamValueList(), hasSize(1));
        }

        Set<String> paramValueSet = jobSpecs.stream()
                .map(jobSpec -> GrpcUtil.paramsListToMap(jobSpec.getInputsList()).get("input"))
                .flatMap(params -> params.stream())
                .collect(toSet());

        assertThat(paramValueSet, hasSize(4));
        assertThat(paramValueSet, is(ImmutableSet.of(FILE_11_URI, FILE_12_URI, FILE_21_URI, FILE_22_URI)));
    }

    @Test
    public void testEvaluateDatabasketNoParallel() throws Exception {
        Long databasketId = databasket1.getId();
        String jobId = UUID.randomUUID().toString();

        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(ftepAdmin.getName())
                .setServiceId(ftepService.getName())
                .addInputs(JobParam.newBuilder()
                        .setParamName("input")
                        .setParallelParameter(false)
                        .setSearchParameter(false)
                        .addParamValue("ftep://databasket/" + databasketId)
                        .build())
                .build();

        List<JobSpec> jobSpecs = jobSpecFactory.expandJobParams(request);
        assertThat(jobSpecs, hasSize(1));

        JobSpec jobSpec = jobSpecs.get(0);
        assertThat(jobSpec.getJob().getId(), is(jobId));
        assertThat(jobSpec.getJob().getUserId(), is(ftepAdmin.getName()));
        assertThat(jobSpec.getJob().getServiceId(), is(ftepService.getName()));
        assertThat(jobSpec.getInputsCount(), is(1));

        JobParam jobParam = jobSpec.getInputsList().get(0);
        assertThat(jobParam.getParamName(), is("input"));
        assertThat(jobParam.getParamValueList(), hasSize(1));
        Multimap<String, String> params = GrpcUtil.paramsListToMap(jobSpec.getInputsList());
        assertThat(params.get("input"), is(ImmutableList.of("ftep://databasket/" + databasketId)));
    }

    @Test
    public void testEvaluateDatabasketUriMixture() throws Exception {
        Long databasket1Id = databasket1.getId();
        String jobId = UUID.randomUUID().toString();

        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(ftepAdmin.getName())
                .setServiceId(ftepService.getName())
                .addInputs(JobParam.newBuilder()
                        .setParamName("input1")
                        .setParallelParameter(true)
                        .setSearchParameter(false)
                        .addParamValue("ftep://databasket/" + databasket1Id)
                        .addParamValue(FILE_31_URI)
                        .addParamValue(FILE_32_URI)
                        .build())
                .build();

        List<JobSpec> jobSpecs = jobSpecFactory.expandJobParams(request);
        assertThat(jobSpecs, hasSize(4));

        for (JobSpec jobSpec : jobSpecs) {
            assertThat(jobSpec.getJob().getId(), is(not(jobId)));
            assertThat(jobSpec.getJob().getUserId(), is(ftepAdmin.getName()));
            assertThat(jobSpec.getJob().getServiceId(), is(ftepService.getName()));
            assertThat(jobSpec.getInputsCount(), is(1));

            JobParam jobParam = jobSpec.getInputsList().get(0);
            assertThat(jobParam.getParamName(), is("input1"));
            assertThat(jobParam.getParamValueList(), hasSize(1));
        }

        Set<String> paramValueSet = jobSpecs.stream()
                .map(jobSpec -> GrpcUtil.paramsListToMap(jobSpec.getInputsList()).get("input1"))
                .flatMap(params -> params.stream())
                .collect(toSet());

        assertThat(paramValueSet, hasSize(4));
        assertThat(paramValueSet, is(ImmutableSet.of(FILE_11_URI, FILE_12_URI, FILE_31_URI, FILE_32_URI)));
    }

    @Test
    public void testEvaluateDatabasketMultipleParams() throws Exception {
        Long databasket1Id = databasket1.getId();
        Long databasket2Id = databasket2.getId();
        String jobId = UUID.randomUUID().toString();

        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(ftepAdmin.getName())
                .setServiceId(ftepService.getName())
                .addInputs(JobParam.newBuilder()
                        .setParamName("input1")
                        .setParallelParameter(true)
                        .setSearchParameter(false)
                        .addParamValue("ftep://databasket/" + databasket1Id)
                        .build())
                .addInputs(JobParam.newBuilder()
                        .setParamName("input2")
                        .setParallelParameter(true)
                        .setSearchParameter(false)
                        .addParamValue("ftep://databasket/" + databasket2Id)
                        .build())
                .addInputs(JobParam.newBuilder()
                        .setParamName("input3")
                        .setParallelParameter(true)
                        .setSearchParameter(false)
                        .addParamValue(FILE_31_URI)
                        .addParamValue(FILE_32_URI)
                        .build())
                .build();

        List<JobSpec> jobSpecs = jobSpecFactory.expandJobParams(request);
        assertThat(jobSpecs, hasSize(6));

        for (JobSpec jobSpec : jobSpecs) {
            assertThat(jobSpec.getJob().getId(), is(not(jobId)));
            assertThat(jobSpec.getJob().getUserId(), is(ftepAdmin.getName()));
            assertThat(jobSpec.getJob().getServiceId(), is(ftepService.getName()));
            assertThat(jobSpec.getInputsCount(), is(1));

            JobParam jobParam = jobSpec.getInputsList().get(0);
            assertThat(jobParam.getParamValueList(), hasSize(1));
        }

        Set<String> paramValueSet1 = jobSpecs.stream()
                .map(jobSpec -> GrpcUtil.paramsListToMap(jobSpec.getInputsList()).get("input1"))
                .flatMap(params -> params.stream())
                .collect(toSet());

        assertThat(paramValueSet1, hasSize(2));
        assertThat(paramValueSet1, is(ImmutableSet.of(FILE_11_URI, FILE_12_URI)));

        Set<String> paramValueSet2 = jobSpecs.stream()
                .map(jobSpec -> GrpcUtil.paramsListToMap(jobSpec.getInputsList()).get("input2"))
                .flatMap(params -> params.stream())
                .collect(toSet());

        assertThat(paramValueSet2, hasSize(2));
        assertThat(paramValueSet2, is(ImmutableSet.of(FILE_21_URI, FILE_22_URI)));

        Set<String> paramValueSet3 = jobSpecs.stream()
                .map(jobSpec -> GrpcUtil.paramsListToMap(jobSpec.getInputsList()).get("input3"))
                .flatMap(params -> params.stream())
                .collect(toSet());

        assertThat(paramValueSet3, hasSize(2));
        assertThat(paramValueSet3, is(ImmutableSet.of(FILE_31_URI, FILE_32_URI)));
    }
}


