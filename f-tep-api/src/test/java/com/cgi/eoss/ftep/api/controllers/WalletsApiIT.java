package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.persistence.service.WalletDataService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
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
public class WalletsApiIT {
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

    @Test
    public void testGet() throws Exception {
        String ftepUserJson = mockMvc.perform(get("/api/users/" + ftepUser.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String walletUrl = ((String) JsonPath.compile("$._links.wallet.href").read(ftepUserJson)).replace("{?projection}", "");

        mockMvc.perform(get(walletUrl).header("REMOTE_USER", ftepGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(walletUrl).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        String walletJson = mockMvc.perform(get(walletUrl).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100))
                .andReturn().getResponse().getContentAsString();

        String transactionsUrl = ((String) JsonPath.compile("$._links.transactions.href").read(walletJson)).replace("{?projection}", "");

        mockMvc.perform(get(transactionsUrl).header("REMOTE_USER", ftepGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(transactionsUrl).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(get(transactionsUrl).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.walletTransactions").isArray())
                .andExpect(jsonPath("$._embedded.walletTransactions.length()").value(1))
                .andExpect(jsonPath("$._embedded.walletTransactions[0].balanceChange").value(100));
    }

}
