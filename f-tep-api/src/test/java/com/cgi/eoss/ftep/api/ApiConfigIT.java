package com.cgi.eoss.ftep.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
// TODO Remove with Spring-Boot 1.5.0, when the Mockito test listener is no longer broken
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class, DirtiesContextTestExecutionListener.class})
public class ApiConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.databaskets").exists())
                .andExpect(jsonPath("$._links.groups").exists())
                .andExpect(jsonPath("$._links.jobs").exists())
                .andExpect(jsonPath("$._links.projects").exists())
                .andExpect(jsonPath("$._links.users").exists())
                .andExpect(jsonPath("$._links.services").exists());
    }
}