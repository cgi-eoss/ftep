package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.JobConfigDataService;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import org.junit.After;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(value = "classpath:test-api.properties", properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@Transactional
public class SystematicProcessingsApiIT {
    @Autowired
    private UserDataService dataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private JobConfigDataService jobConfigDataService;

    @Autowired
    private JobDataService jobDataService;

    @Autowired
    private SystematicProcessingDataService systematicProcessingDataService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepAdmin;
    private FtepService svc;
    private JobConfig jobConfig;
    private Job job;

    @Before
    public void setUp() {
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        dataService.save(ImmutableSet.of(ftepAdmin));

        svc = new FtepService("ftepService", ftepAdmin, "dockerTag");
        serviceDataService.save(svc);
        jobConfig = new JobConfig();
        jobConfig.setOwner(ftepAdmin);
        jobConfig.setService(svc);
        jobConfigDataService.save(jobConfig);
        job = jobDataService.save(new Job(jobConfig, UUID.randomUUID().toString(), ftepAdmin));
    }

    @After
    public void tearDown() {
        systematicProcessingDataService.deleteAll();
        jobDataService.deleteAll();
        jobConfigDataService.deleteAll();
        serviceDataService.deleteAll();
    }

    @Test
    public void testSave() throws Exception {
        mockMvc.perform(get("/api/systematicProcessings").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.systematicProcessings.length()").value(0));

        // Systematic Processings cannot be created via the API
        mockMvc.perform(post("/api/systematicProcessings").header("REMOTE_USER", ftepAdmin.getName())
                .content("{\n" +
                        "  \"job\" : \"" + getJobUrl(job) + "\",\n" +
                        "  \"searchParameters\" : {}\n" +
                        "}"))
                .andExpect(status().isMethodNotAllowed());
    }

    private String getJobUrl(Job job) throws Exception {
        return ((String) JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/jobs/" + job.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
        ).replace("{?projection}", "");
    }

}
