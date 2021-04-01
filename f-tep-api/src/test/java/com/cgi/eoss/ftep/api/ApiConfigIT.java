package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ApiConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDataService userDataService;

    private User ftepUser;

    @Before
    public void setUp() {
        ftepUser = new User("ftep-user");
        ftepUser.setRole(Role.USER);
        ftepUser.getWallet().setBalance(100);
        userDataService.save(ftepUser);
    }

    @After
    public void tearDown() {
        userDataService.deleteAll();
    }

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", ftepUser.getName()))
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
