package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.ServiceStatus;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceFileDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ServiceFilesApiIT {

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private ServiceFileDataService serviceFileDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepExpertUser;
    private User ftepContentAuthority;
    private FtepService svc;

    @Before
    public void setUp() {
        ftepExpertUser = new User("ftep-expert-user");
        ftepExpertUser.setRole(Role.EXPERT_USER);
        ftepContentAuthority = new User("ftep-content-authority");
        ftepContentAuthority.setRole(Role.CONTENT_AUTHORITY);

        userDataService.save(ImmutableSet.of(ftepExpertUser, ftepContentAuthority));

        svc = new FtepService("service-1", ftepExpertUser, "dockerTag");
        svc.setStatus(ServiceStatus.AVAILABLE);
        serviceDataService.save(svc);
    }

    @Test
    public void testSave() throws Exception {
        String svcUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/services/" + svc.getId()).header("REMOTE_USER", ftepContentAuthority.getName()))
                        .andReturn().getResponse().getContentAsString()
        );

        byte[] testFile1Bytes = Files.readAllBytes(Paths.get(ServiceFilesApiIT.class.getResource("/testFile1").toURI()));
        String testFile1b64 = BaseEncoding.base64().encode(testFile1Bytes);
        byte[] testFile2Bytes = Files.readAllBytes(Paths.get(ServiceFilesApiIT.class.getResource("/testFile2").toURI()));
        String testFile2b64 = BaseEncoding.base64().encode(testFile2Bytes);

        String newFile1 = "{\"filename\": \"testFile1\", \"content\": \"" + testFile1b64 + "\", \"service\": \"" + svcUrl + "\"}";
        String newFile2 = "{\"filename\": \"testFile2\", \"content\": \"" + testFile2b64 + "\", \"service\": \"" + svcUrl + "\"}";

        mockMvc.perform(post("/api/serviceFiles").header("REMOTE_USER", ftepExpertUser.getName()).content(newFile1))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/serviceFiles/\\d+$")));
        mockMvc.perform(post("/api/serviceFiles").header("REMOTE_USER", ftepExpertUser.getName()).content(newFile2))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/serviceFiles/\\d+$")));

        List<FtepServiceContextFile> serviceFiles = serviceFileDataService.findByService(svc);
        assertThat(serviceFiles.size(), is(2));
        assertThat(serviceFiles.get(0).getContent(), is(new String(testFile1Bytes)));
        assertThat(serviceFiles.get(1).getContent(), is(new String(testFile2Bytes)));
    }

}