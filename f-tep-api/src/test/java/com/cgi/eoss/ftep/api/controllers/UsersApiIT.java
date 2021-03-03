package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInRelativeOrder.containsInRelativeOrder;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
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
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
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
        ftepGuest.setEmail("ftepGuest@example.com");
        ftepGuest.setRole(Role.GUEST);
        ftepUser = new User("ftep-user");
        ftepUser.setEmail("ftepUser@example.com");
        ftepUser.setRole(Role.USER);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setEmail("ftepAdmin@example.com");
        ftepAdmin.setRole(Role.ADMIN);

        dataService.save(ImmutableSet.of(ftepGuest, ftepUser, ftepAdmin));
    }

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.users").exists());
    }

    @Test
    public void testGetAllUsersAsAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users[*].email",
                        Matchers.hasItems(ftepAdmin.getEmail(), ftepUser.getEmail(), ftepGuest.getEmail())));
    }

    @Test
    public void testGetAllUsersAsUser() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetSelfAsUser() throws Exception {
        mockMvc.perform(get("/api/users/" + ftepUser.getId())
                .header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ftepUser.getEmail()));
    }

    @Test
    public void testGetOtherUserAsAdmin() throws Exception {
        mockMvc.perform(get("/api/users/" + ftepGuest.getId())
                .header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ftepGuest.getEmail()));
    }

    @Test
    public void testGetOtherUserAsUser() throws Exception {
        mockMvc.perform(get("/api/users/" + ftepGuest.getId())
                .header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetInvalidUserAsAdmin() throws Exception {
        mockMvc.perform(get("/api/users/789")
                .header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetInvalidUserAsUser() throws Exception {
        mockMvc.perform(get("/api/users/789")
                .header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSaveAsAdmin() throws Exception {
        mockMvc.perform(put("/api/users/" + ftepUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ftep-user-new\"}")
                .header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().is(204));
    }

    @Test
    public void testSaveAsUser() throws Exception {
        mockMvc.perform(put("/api/users/" + ftepUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ftep-user-new\"}")
                .header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().is(403));
    }

    @Test
    public void testFindByFilter() throws Exception {
        User owner = dataService.save(new User("owner-uid"));
        User owner2 = dataService.save(new User("owner-uid2"));
        owner.setEmail("owner@example.com");
        owner2.setEmail("owner2@example.com");

        // Seach by name
        mockMvc.perform(get("/api/users/search/byFilter?filter=owner-uid2").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users").isArray())
                .andExpect(jsonPath("$._embedded.users.length()").value(1))
                .andExpect(jsonPath("$._embedded.users[0].name").value("owner-uid2"));

        // Search by email
        mockMvc.perform(get("/api/users/search/byFilter?filter=owner@example.com&sort=name,asc").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users").isArray())
                .andExpect(jsonPath("$._embedded.users.length()").value(1))
                .andExpect(jsonPath("$._embedded.users[0].name").value("owner-uid"));
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