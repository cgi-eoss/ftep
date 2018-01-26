package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
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
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
    private UserDataService dataService;

    @Test
    public void testPostgresSchemaAndWiring() throws Exception {
        User user = new User("user-id");
        User user2 = new User("user-uid2");
        dataService.save(ImmutableSet.of(user, user2));

        assertThat(dataService.getAll(), containsInAnyOrder(User.DEFAULT, user, user2));
    }

}