package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInRelativeOrder;
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

    private User ftepGuest;
    private User ftepUser;
    private User ftepAdmin;

    @Before
    public void setUp() {
        ftepGuest = new User("ftep-guest");
        ftepGuest.setRole(Role.GUEST);
        ftepUser = new User("ftep-user");
        ftepUser.setRole(Role.USER);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        dataService.save(ImmutableSet.of(ftepGuest, ftepUser, ftepAdmin));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.users").exists());
    }

    @Test
    public void testGet() throws Exception {
        User owner = dataService.save(new User("owner-uid"));
        User owner2 = dataService.save(new User("owner-uid2"));
        owner.setEmail("owner@example.com");
        owner2.setEmail("owner2@example.com");

        mockMvc.perform(get("/api/users").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users").isArray())
                .andExpect(jsonPath("$._embedded.users[4].name").value("owner-uid2"))
                .andExpect(jsonPath("$._embedded.users[4].email").value("owner2@example.com"))
                .andExpect(jsonPath("$._embedded.users[4]._links.self.href").value(endsWith("/users/" + owner2.getId())));

        mockMvc.perform(get("/api/users/" + owner.getId()).header("REMOTE_USER", "ftep-new-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("owner-uid"))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/users/" + owner.getId())));

        // The unknown user "ftep-new-user" was created automatically
        assertThat(dataService.getByName("ftep-new-user"), is(notNullValue()));
        assertThat(dataService.getByName("ftep-new-user").getRole(), is(Role.GUEST));
    }

    @Test
    public void testGetWithoutAuthRequestAttribute() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreate() throws Exception {
        mockMvc.perform(post("/api/users").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"Ftep User\", \"email\":\"ftep.user@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")));
        mockMvc.perform(post("/api/users").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"Ftep User 2\", \"email\":\"ftep.user.2@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")));

        User user = new User("Ftep User");
        User user2 = new User("Ftep User 2");
        user.setEmail("ftep.user@example.com");
        user2.setEmail("ftep.user.2@example.com");

        assertThat(dataService.getAll(), containsInRelativeOrder(user, user2));
    }

    @Test
    public void testUpdate() throws Exception {
        MvcResult result =
                mockMvc.perform(post("/api/users").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"Ftep User\", \"email\":\"ftep.user@example.com\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                        .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(put(location).header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"New Name\", \"email\":\"new.ftep.email@example.com\"}")).andExpect(
                status().isNoContent());

        User expected = new User("New Name");
        expected.setEmail("new.ftep.email@example.com");
        assertThat(dataService.getByName("New Name"), is(expected));
    }

    @Test
    public void testPartialUpdate() throws Exception {
        MvcResult result =
                mockMvc.perform(post("/api/users").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"Ftep User\", \"email\":\"ftep.user@example.com\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                        .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(patch(location).header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"New Name\"}"))
                .andExpect(status().isNoContent());

        User expected = new User("New Name");
        expected.setEmail("ftep.user@example.com");
        assertThat(dataService.getByName("New Name"), is(expected));
    }

    @Test
    public void testDelete() throws Exception {
        MvcResult result =
                mockMvc.perform(post("/api/users").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"Ftep User\", \"email\":\"ftep.user@example.com\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                        .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(delete(location).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isMethodNotAllowed());

        User expected = new User("Ftep User");
        expected.setEmail("ftep.user@example.com");
        assertThat(dataService.getByName("Ftep User"), is(notNullValue()));
    }

}