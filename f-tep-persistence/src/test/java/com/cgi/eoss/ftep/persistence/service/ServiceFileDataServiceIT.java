package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class ServiceFileDataServiceIT {

    @Autowired
    private ServiceFileDataService dataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private UserDataService userService;

    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FtepService svc1 = new FtepService();
        svc1.setName("Test Service 1");
        svc1.setOwner(owner);
        svc1.setDockerTag("dockerTag");
        FtepService svc2 = new FtepService();
        svc2.setName("Test Service 2");
        svc2.setOwner(owner);
        svc2.setDockerTag("dockerTag");

        serviceDataService.save(svc1);
        serviceDataService.save(svc2);

        String serviceFingerprint = serviceDataService.computeServiceFingerprint(svc1);
        assertThat(serviceFingerprint, is(notNullValue()));

        byte[] fileBytes = Files.readAllBytes(Paths.get(getClass().getResource("/testService/Dockerfile").toURI()));
        FtepServiceContextFile serviceFile = new FtepServiceContextFile();
        serviceFile.setService(svc1);
        serviceFile.setFilename("Dockerfile");
        serviceFile.setContent(new String(fileBytes));
        dataService.save(serviceFile);

        assertThat(dataService.getAll(), is(ImmutableList.of(serviceFile)));
        assertThat(dataService.getById(serviceFile.getId()), is(serviceFile));
        assertThat(dataService.getByIds(ImmutableSet.of(serviceFile.getId())), is(ImmutableList.of(serviceFile)));
        assertThat(dataService.isUniqueAndValid(new FtepServiceContextFile(svc1, "Dockerfile")), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepServiceContextFile(svc1, "Dockerfile2")), is(true));
        assertThat(dataService.findByService(svc1), is(ImmutableList.of(serviceFile)));

        // Verify text file recovery
        assertThat(dataService.getById(serviceFile.getId()).getContent().getBytes(), is(fileBytes));

        // Save, change the fingerprint and re-check
        serviceDataService.save(svc1);
        String newFingerprint = serviceDataService.computeServiceFingerprint(svc1);
        assertThat(serviceFingerprint, is(not(newFingerprint)));

        serviceFile.setService(svc2);
        dataService.save(serviceFile);
        serviceDataService.save(svc1);
        String shouldBeRestoredFingerprint = serviceDataService.computeServiceFingerprint(svc1);
        assertThat(serviceFingerprint, is(shouldBeRestoredFingerprint));
    }
}
