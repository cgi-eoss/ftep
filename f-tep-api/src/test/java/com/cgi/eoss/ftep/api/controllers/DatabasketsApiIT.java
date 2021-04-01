package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.DatabasketDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
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
public class DatabasketsApiIT {

    @Autowired
    private DatabasketDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepUser;
    private User ftepAdmin;

    private Databasket databasket1;
    private Databasket databasket2;
    private Databasket databasket3;
    private Databasket databasket4;

    @Before
    public void setUp() {
        ftepUser = new User("ftep-user");
        ftepUser.setRole(Role.USER);
        ftepUser.getWallet().setBalance(100);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(ftepUser, ftepAdmin));

        databasket1 = new Databasket("Basket1", ftepUser);
        databasket1.setDescription("test basket 1");
        databasket2 = new Databasket("Basket2", ftepUser);
        databasket2.setDescription("test basket 2");
        databasket3 = new Databasket("Basket3", ftepUser);
        databasket3.setDescription("my data collection");
        databasket4 = new Databasket("123056709012305670901230567090", ftepUser);
        databasket4.setDescription("DB with long numerical name");

        dataService.save(ImmutableSet.of(databasket1, databasket2, databasket3, databasket4));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testFindByFilterOnly() throws Exception {
        mockMvc.perform(get("/api/databaskets/search/findByFilterOnly?filter=Basket1").header("REMOTE_USER",
                ftepUser.getName())).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(1))
                .andExpect(jsonPath("$._embedded.databaskets[0].name").value("Basket1"));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=Basket").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));

        mockMvc.perform(get("/api/databaskets/search/findByFilterOnly?filter=Basket4").header("REMOTE_USER",
                ftepUser.getName())).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(0));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=basket").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=BASKET").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=Test").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(2));

        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=coll").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(1))
                .andExpect(jsonPath("$._embedded.databaskets[0].name").value("Basket3"));
        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=sket").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(3));
        mockMvc.perform(
                get("/api/databaskets/search/findByFilterOnly?filter=0567090123056709012305").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(1))
                .andExpect(jsonPath("$._embedded.databaskets[0].name").value("123056709012305670901230567090"));
    }

    @Test
    public void testFindByFilterAndOwner() throws Exception {
        String ftepUserUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + ftepUser.getId()).header("REMOTE_USER", ftepUser.getName()))
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/databaskets/search/findByFilterAndOwner?filter=SKet1&owner=" + ftepUserUrl)
                .header("REMOTE_USER", ftepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(1))
                .andExpect(jsonPath("$._embedded.databaskets[0].name").value("Basket1"));
    }

    @Test
    public void testFindByFilterAndNotOwner() throws Exception {
        String ftepUserUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + ftepUser.getId()).header("REMOTE_USER", ftepUser.getName()))
                        .andReturn().getResponse().getContentAsString());

        String ftepAdminUrl = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/users/" + ftepAdmin.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/databaskets/search/findByFilterAndNotOwner?filter=Basket1&owner=" + ftepUserUrl)
                .header("REMOTE_USER", ftepUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(0));

        mockMvc.perform(get("/api/databaskets/search/findByFilterAndNotOwner?filter=&owner=" + ftepAdminUrl)
                .header("REMOTE_USER", ftepAdmin.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.databaskets").isArray())
                .andExpect(jsonPath("$._embedded.databaskets.length()").value(4));
    }

}
