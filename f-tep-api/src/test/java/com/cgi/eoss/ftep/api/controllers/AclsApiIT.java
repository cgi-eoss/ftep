package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.ServiceStatus;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.GroupDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class AclsApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private GroupDataService groupDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepUser;
    private User ftepAdmin;
    private Group defaultGroup;
    private FtepService service1;

    @Before
    public void setUp() {
        ftepUser = new User("ftep-user");
        ftepUser.setRole(Role.USER);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);
        userDataService.save(ImmutableSet.of(ftepUser, ftepAdmin));

        defaultGroup = new Group("defaultGroup", ftepAdmin);
        defaultGroup.setMembers(ImmutableSet.of(ftepUser));
        groupDataService.save(defaultGroup);

        service1 = new FtepService("service-1", ftepAdmin, "dockerTag");
        service1.setStatus(ServiceStatus.AVAILABLE);
        serviceDataService.save(service1);
    }

    @After
    public void tearDown() {
        serviceDataService.getAll().stream()
                .map(FtepService::getId)
                .map(id -> new ObjectIdentityImpl(FtepService.class, id))
                .forEach(oid -> aclService.deleteAcl(oid, true));
        serviceDataService.deleteAll();
    }

    @Test
    public void testGetServiceAcl() throws Exception {
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(service1.getId()))
                .andExpect(jsonPath("$.permissions").isEmpty());
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isForbidden());

        // Grant ADMIN to ftepUser
        MutableAcl acl = getAcl(new ObjectIdentityImpl(FtepService.class, service1.getId()));
        FtepPermission.ADMIN.getAclPermissions()
                .forEach(p -> acl.insertAce(acl.getEntries().size(), p, new GrantedAuthoritySid(defaultGroup), true));
        aclService.updateAcl(acl);

        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(service1.getId()))
                .andExpect(jsonPath("$.permissions[0].group.name").value(defaultGroup.getName()))
                .andExpect(jsonPath("$.permissions[0].permission").value("ADMIN"));
        // And now the user can read the ACL
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk());
    }

    @Test
    public void testSetServiceAcl() throws Exception {
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(service1.getId()))
                .andExpect(jsonPath("$.permissions").isEmpty());

        // Grant ADMIN to ftepUser by HTTP POST
        mockMvc.perform(post("/api/acls/service/" + service1.getId()).header("REMOTE_USER", ftepAdmin.getName()).contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"entityId\":" + service1.getId() + ", \"permissions\":[{\"group\":{\"id\":" + defaultGroup.getId() + ",\"name\":\"" + defaultGroup.getName() + "\"},\"permission\":\"ADMIN\"}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(service1.getId()))
                .andExpect(jsonPath("$.permissions[0].group.name").value(defaultGroup.getName()))
                .andExpect(jsonPath("$.permissions[0].permission").value("ADMIN"));
        // And now the user can read the ACL
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk());
    }

    private MutableAcl getAcl(ObjectIdentity objectIdentity) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(ftepAdmin.getName(), "N/A", "ROLE_ADMIN"));
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity);
        } catch (NotFoundException nfe) {
            return aclService.createAcl(objectIdentity);
        }
    }

}