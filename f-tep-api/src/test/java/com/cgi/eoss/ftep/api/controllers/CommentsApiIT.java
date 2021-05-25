package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class CommentsApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepAdmin;

    @Before
    public void setUp() {
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setEmail("ftepAdmin@example.com");
        ftepAdmin.setRole(Role.ADMIN);

        userDataService.save(ftepAdmin);
    }

    @Test
    public void testCreateComment() throws Exception {
        mockMvc.perform(post("/api/comments").header("REMOTE_USER", ftepAdmin.getName())
                .content("{\n" +
                        "    \"owner\": \"" + userUri(mockMvc, ftepAdmin) + "\",\n" +
                        "    \"commentText\": \"New comment.\"\n" +
                        "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/comments").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.comments").isArray())
                .andExpect(jsonPath("$._embedded.comments.length()").value(1))
                .andExpect(jsonPath("$._embedded.comments[0].commentText").value("New comment."));
    }
}
