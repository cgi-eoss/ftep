package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ContentAuthorityApiIT {
    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepUser;
    private User ftepAdmin;

    @Before
    public void setUp() {
        ftepUser = new User("ftep-user");
        ftepUser.setRole(Role.USER);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(ftepUser, ftepAdmin));
    }

    @Test
    public void restoreDefaultServices() throws Exception {
        String testServiceName = "SNAP";
        int defaultServiceCount = DefaultFtepServices.getDefaultServices().size();

        // Check nothing exists already
        assertThat(serviceDataService.getAll().size(), is(0));

        // Restore default services by HTTP POST
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        assertThat(serviceDataService.getAll().size(), is(defaultServiceCount));

        // Remove one of the default services
        serviceDataService.delete(serviceDataService.getByName(testServiceName));
        assertThat(serviceDataService.getByName(testServiceName), is(nullValue()));

        // Restore default services again
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        // Assert the deleted service has been recovered
        assertThat(serviceDataService.getAll().size(), is(defaultServiceCount));
        assertThat(serviceDataService.getByName(testServiceName), is(notNullValue()));

        // Assert the default services are visible to the public
        mockMvc.perform(get("/api/services").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services.length()").value(defaultServiceCount))
                .andExpect(jsonPath("$._embedded.services[?(@.name == '" + testServiceName + "')]").exists());
    }

}