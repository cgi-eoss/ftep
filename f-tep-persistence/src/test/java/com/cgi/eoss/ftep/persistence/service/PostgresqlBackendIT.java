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
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@Transactional
@AutoConfigureCache
@AutoConfigureDataJpa
@AutoConfigureEmbeddedDatabase
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureTestEntityManager
@TestPropertySource(properties = {
        "spring.flyway.locations=db/migration/postgresql",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.hikari.autoCommit=false"
})
public class PostgresqlBackendIT {

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