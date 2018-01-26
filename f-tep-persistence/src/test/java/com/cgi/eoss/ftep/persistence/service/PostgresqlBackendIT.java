package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class}, initializers = {PostgresqlBackendIT.Initializer.class})
@TestPropertySource("classpath:test-persistence-postgresql.properties")
@Transactional
public class PostgresqlBackendIT {

    @BeforeClass
    public static void precondition() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling Docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use
    }

    @ClassRule
    public static PostgreSQLContainer postgres = new PostgreSQLContainer()
            .withDatabaseName("ftep_v2")
            .withUsername("ftepdb")
            .withPassword("ftepdb");

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            EnvironmentTestUtils.addEnvironment("testcontainers", configurableApplicationContext.getEnvironment(),
                    "spring.datasource.url=jdbc:postgresql://" + postgres.getContainerIpAddress() + ":" + postgres.getMappedPort(5432) + "/ftep_v2?stringtype=unspecified",
                    "spring.datasource.username=ftepdb",
                    "spring.datasource.password=ftepdb"
            );
        }
    }

    @Autowired
    private ServiceDataService serviceDataService;
    @Autowired
    private UserDataService userDataService;
    @Autowired
    private JobConfigDataService jobConfigDataService;

    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userDataService.save(ImmutableSet.of(owner, owner2));
        assertThat(userDataService.getAll(), is(ImmutableList.of(User.DEFAULT, owner, owner2)));

        FtepService svc = new FtepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svc.setDockerTag("dockerTag");
        svc.setServiceDescriptor(FtepServiceDescriptor.builder()
                .id("TestService1")
                .title("Test Service for ZCFG Generation")
                .description("This service tests the F-TEP automatic zcfg file generation")
                .version("1.0")
                .serviceProvider("ftep_service_wrapper")
                .serviceType("python")
                .storeSupported(false)
                .statusSupported(false)
                .dataInputs(ImmutableList.of(
                        FtepServiceDescriptor.Parameter.builder()
                                .id("inputfile")
                                .title("Input File 1")
                                .description("The input data file")
                                .minOccurs(1)
                                .maxOccurs(1)
                                .data(FtepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                                .defaultAttrs(ImmutableMap.<String, String>builder()
                                        .put("dataType", "string")
                                        .build())
                                .build()))
                .dataOutputs(ImmutableList.of(
                        FtepServiceDescriptor.Parameter.builder()
                                .id("result")
                                .title("URL to service output")
                                .description("see title")
                                .data(FtepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                                .defaultAttrs(ImmutableMap.<String, String>builder()
                                        .put("dataType", "string").build())
                                .build()))
                .build());
        serviceDataService.save(svc);
        assertThat(serviceDataService.getAll(), is(ImmutableList.of(svc)));

        Multimap<String, String> job1Inputs = ImmutableMultimap.of(
                "input1", "foo",
                "input2", "bar1",
                "input2", "bar2",
                "input3", "http://baz/?q=x,y&z={}"
        );
        JobConfig jobConfig = new JobConfig(owner, svc);
        jobConfig.setInputs(job1Inputs);
        jobConfigDataService.save(ImmutableList.of(jobConfig));
        assertThat(jobConfigDataService.getAll(), is(ImmutableList.of(jobConfig)));
    }
}