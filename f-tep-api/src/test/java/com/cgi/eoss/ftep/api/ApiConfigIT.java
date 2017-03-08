package com.cgi.eoss.ftep.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ApiConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", "ftep-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.databaskets").exists())
                .andExpect(jsonPath("$._links.ftepFiles").exists())
                .andExpect(jsonPath("$._links.groups").exists())
                .andExpect(jsonPath("$._links.jobs").exists())
                .andExpect(jsonPath("$._links.jobConfigs").exists())
                .andExpect(jsonPath("$._links.projects").exists())
                .andExpect(jsonPath("$._links.services").exists())
                .andExpect(jsonPath("$._links.users").exists());
    }

}
