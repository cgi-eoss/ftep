package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
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

import static org.hamcrest.CoreMatchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class PublishingRequestsApiIT {
    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    private FtepService service1;
    private FtepService service2;
    private FtepService service3;

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

        service1 = new FtepService("service-1", ftepAdmin, "dockerTag");
        service1.setStatus(FtepService.Status.AVAILABLE);
        service2 = new FtepService("service-2", ftepUser, "dockerTag");
        service2.setStatus(FtepService.Status.IN_DEVELOPMENT);
        service3 = new FtepService("service-3", ftepGuest, "dockerTag");
        service3.setStatus(FtepService.Status.IN_DEVELOPMENT);
        serviceDataService.save(ImmutableSet.of(service1, service2, service3));
    }

    @Test
    public void testRequestPublishService() throws Exception {
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service1.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testGet() throws Exception {
        String svc2Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", ftepGuest.getName()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(svc2Url).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$._links.associated.href").value(endsWith("/api/services/" + service2.getId() + "{?projection}")));
        mockMvc.perform(get(svc2Url).header("REMOTE_USER", ftepGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(svc2Url).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", ftepGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
    }

    @Test
    public void testFindByStatusAndPublish() throws Exception {
        String svc2Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", ftepGuest.getName()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", ftepGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));

        mockMvc.perform(post("/api/contentAuthority/services/publish/" + service2.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED,NEEDS_INFO,REJECTED").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(0));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=GRANTED").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(0));
    }

}
