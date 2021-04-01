package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.GroupDataService;
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class GroupsApiIT {

    @Autowired
    private UserDataService userDataService;
    @Autowired
    private GroupDataService groupDataService;

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
        ftepUser.getWallet().setBalance(100);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(ftepGuest, ftepUser, ftepAdmin));
    }

    @Test
    public void testCreate() throws Exception {
        mockMvc.perform(post("/api/groups").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"New Group\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/groups/\\d+$")));
        mockMvc.perform(post("/api/groups").header("REMOTE_USER", ftepUser.getName()).content("{\"name\": \"New Group 2\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/groups/\\d+$")));

        Group newGroup = new Group("New Group", ftepAdmin);
        newGroup.setMembers(ImmutableSet.of(ftepAdmin));
        Group newGroup2 = new Group("New Group 2", ftepUser);
        newGroup2.setMembers(ImmutableSet.of(ftepUser));

        assertThat(groupDataService.findGroupMemberships(ftepAdmin), contains(newGroup));
        assertThat(groupDataService.findGroupMemberships(ftepUser), contains(newGroup2));
    }

}