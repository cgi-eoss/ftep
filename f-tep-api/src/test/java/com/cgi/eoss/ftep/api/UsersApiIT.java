package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class UsersApiIT {
    @Autowired
    private UserDataService dataService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.users").exists());
    }

    @Test
    public void testGet() throws Exception {
        FtepUser owner = dataService.save(new FtepUser("owner-uid"));
        FtepUser owner2 = dataService.save(new FtepUser("owner-uid2"));
        owner.setEmail("owner@example.com");
        owner2.setEmail("owner2@example.com");

        assertThat(dataService.getAll(), is(ImmutableList.of(owner, owner2)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users").isArray())
                .andExpect(jsonPath("$._embedded.users[1].name").value("owner-uid2"))
                .andExpect(jsonPath("$._embedded.users[1].email").value("owner2@example.com"))
                .andExpect(jsonPath("$._embedded.users[1]._links.self.href").value(endsWith("/users/" + owner2.getId())));

        mockMvc.perform(get("/api/users/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("owner-uid"))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/users/" + owner.getId())));
    }

    @Test
    public void testCreate() throws Exception {
        mockMvc.perform(post("/api/users").content("{\"name\": \"Ftep User\", \"email\":\"ftep.user@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")));
        mockMvc.perform(post("/api/users").content("{\"name\": \"Ftep User 2\", \"email\":\"ftep.user.2@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")));

        FtepUser ftepUser = new FtepUser("Ftep User");
        FtepUser ftepUser2 = new FtepUser("Ftep User 2");
        ftepUser.setEmail("ftep.user@example.com");
        ftepUser2.setEmail("ftep.user.2@example.com");

        assertThat(dataService.getAll(), is(ImmutableList.of(ftepUser, ftepUser2)));
    }

    @Test
    public void testUpdate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users").content("{\"name\": \"Ftep User\", \"email\":\"ftep.user@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(put(location).content(
                "{\"name\": \"New Name\", \"email\":\"new.ftep.email@example.com\"}")).andExpect(
                status().isNoContent());

        FtepUser expected = new FtepUser("New Name");
        expected.setEmail("new.ftep.email@example.com");
        assertThat(dataService.getByName("New Name"), is(expected));
    }

    @Test
    public void testPartialUpdate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users").content("{\"name\": \"Ftep User\", \"email\":\"ftep.user@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(patch(location).content(
                "{\"name\": \"New Name\"}")).andExpect(
                status().isNoContent());

        FtepUser expected = new FtepUser("New Name");
        expected.setEmail("ftep.user@example.com");
        assertThat(dataService.getByName("New Name"), is(expected));
    }

    @Test
    public void testDelete() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users").content("{\"name\": \"Ftep User\", \"email\":\"ftep.user@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(delete(location)).andExpect(status().isMethodNotAllowed());

        FtepUser expected = new FtepUser("Ftep User");
        expected.setEmail("ftep.user@example.com");
        assertThat(dataService.getAll(), is(ImmutableList.of(expected)));
    }

}