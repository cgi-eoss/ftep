package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.ftep.persistence.service.JobConfigDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
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

import static com.cgi.eoss.ftep.api.TestUtil.userUri;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class JobConfigsApiIT {
    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private JobConfigDataService jobConfigDataService;

    @Autowired
    private CostingExpressionDataService costingExpressionDataService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepAdmin;

    @Before
    public void setUp() {
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(ftepAdmin));
    }

    @Test
    public void testCreateAndGet() throws Exception {
        String serviceUrl = mockMvc.perform(post("/api/services").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(mockMvc, ftepAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")))
                .andReturn().getResponse().getHeader("Location").replaceFirst("\\{.*}", "");

        String jobConfigUrl = mockMvc.perform(post("/api/jobConfigs").header("REMOTE_USER", ftepAdmin.getName())
                .content("{\n" +
                        "  \"inputs\" : {\n" +
                        "    \"input1\" : [ \"foo\" ],\n" +
                        "    \"input2\" : [ \"bar1\", \"bar2\" ],\n" +
                        "    \"input3\" : [ \"http://baz/?q=x,y&z={}\" ]\n" +
                        "  },\n" +
                        "  \"label\" : null,\n" +
                        "  \"service\" : \"" + serviceUrl + "\"\n" +
                        "}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/jobConfigs/\\d+$")))
                .andReturn().getResponse().getHeader("Location").replaceFirst("\\{.*}", "");

        mockMvc.perform(get(jobConfigUrl).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputs.input1[0]").value("foo"))
                .andExpect(jsonPath("$.inputs.input2[0]").value("bar1"))
                .andExpect(jsonPath("$.inputs.input2[1]").value("bar2"))
                .andExpect(jsonPath("$.inputs.input3[0]").value("http://baz/?q=x,y&z={}"));
    }

}
