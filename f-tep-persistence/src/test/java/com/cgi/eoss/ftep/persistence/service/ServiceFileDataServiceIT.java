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

        FtepService svc = new FtepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svc.setDockerTag("dockerTag");
        serviceDataService.save(svc);

        byte[] fileBytes = Files.readAllBytes(Paths.get(getClass().getResource("/testService/Dockerfile").toURI()));
        FtepServiceContextFile serviceFile = new FtepServiceContextFile();
        serviceFile.setService(svc);
        serviceFile.setFilename("Dockerfile");
        serviceFile.setContent(new String(fileBytes));
        dataService.save(serviceFile);

        assertThat(dataService.getAll(), is(ImmutableList.of(serviceFile)));
        assertThat(dataService.getById(serviceFile.getId()), is(serviceFile));
        assertThat(dataService.getByIds(ImmutableSet.of(serviceFile.getId())), is(ImmutableList.of(serviceFile)));
        assertThat(dataService.isUniqueAndValid(new FtepServiceContextFile(svc, "Dockerfile")), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepServiceContextFile(svc, "Dockerfile2")), is(true));

        assertThat(dataService.findByService(svc), is(ImmutableList.of(serviceFile)));

        // Verify text file recovery
        assertThat(dataService.getById(serviceFile.getId()).getContent().getBytes(), is(fileBytes));
    }

}