package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
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
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ApiSecurityConfigIT {

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private GroupDataService groupDataService;

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private MockMvc mockMvc;

    private User alice;
    private User bob;
    private User chuck;
    private User ftepAdmin;

    private Group alpha;
    private Group beta;

    private FtepService service1;
    private FtepService service2;
    private FtepService service3;

    @Before
    public void setUp() {
        alice = new User("alice");
        alice.setRole(Role.USER);
        bob = new User("bob");
        bob.setRole(Role.USER);
        chuck = new User("chuck");
        chuck.setRole(Role.GUEST);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(alice, bob, chuck, ftepAdmin));

        alpha = new Group("alpha", ftepAdmin);
        alpha.setMembers(ImmutableSet.of(alice, bob));
        beta = new Group("beta", ftepAdmin);
        beta.setMembers(ImmutableSet.of(alice));

        groupDataService.save(ImmutableSet.of(alpha, beta));

        service1 = new FtepService("service-1", ftepAdmin, "dockerTag");
        service1.setStatus(ServiceStatus.AVAILABLE);
        service2 = new FtepService("service-2", ftepAdmin, "dockerTag");
        service2.setStatus(ServiceStatus.IN_DEVELOPMENT);
        service3 = new FtepService("service-3", ftepAdmin, "dockerTag");
        service3.setStatus(ServiceStatus.IN_DEVELOPMENT);
        serviceDataService.save(ImmutableSet.of(service1, service2, service3));
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
    public void testUserDetails() throws Exception {
        mockMvc.perform(get("/api/currentUser").header("REMOTE_USER", alice.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alice.getId()))
                .andExpect(jsonPath("$.name").value(alice.getName()))
                .andExpect(jsonPath("$.email").doesNotExist());

        // Make sure the SSO header updates the email attribute
        assertThat(alice.getEmail(), is(nullValue()));
        String aliceEmail = "alice@example.com";
        mockMvc.perform(get("/api/currentUser").header("REMOTE_USER", alice.getName()).header("REMOTE_EMAIL", aliceEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alice.getId()))
                .andExpect(jsonPath("$.name").value(alice.getName()))
                .andExpect(jsonPath("$.email").value(aliceEmail));
        assertThat(userDataService.getByName(alice.getName()).getEmail(), is(aliceEmail));
    }

    @Test
    public void testRoleAccess() throws Exception {
        getServiceFromApi(service1.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), chuck).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), ftepAdmin).andExpect(status().isOk());

        getServiceFromApi(service2.getId(), alice).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), bob).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), ftepAdmin).andExpect(status().isOk());

        getServiceFromApi(service3.getId(), alice).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), bob).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), ftepAdmin).andExpect(status().isOk());
    }

    @Test
    public void testUserAclAccess() throws Exception {
        // Grant read access for service-1 to alice - but unused as it is already AVAILABLE
        createReadAce(service1.getId(), alice.getName());
        // Grant read access for service-2 to bob
        createReadAce(service2.getId(), bob.getName());
        // Grant read access for service-3 to alice and bob
        createReadAce(service3.getId(), alice.getName());
        createReadAce(service3.getId(), bob.getName());

        getServiceFromApi(service1.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), chuck).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), ftepAdmin).andExpect(status().isOk());

        getServiceFromApi(service2.getId(), alice).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service2.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), ftepAdmin).andExpect(status().isOk());

        getServiceFromApi(service3.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service3.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service3.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), ftepAdmin).andExpect(status().isOk());
    }

    @Test
    public void testGroupAclAccess() throws Exception {
        // Grant read access for service-2 to alpha (alice & bob)
        createGroupReadAce(service2.getId(), alpha);
        // Grant read access for service-3 to beta (alice)
        createGroupReadAce(service3.getId(), beta);

        getServiceFromApi(service1.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), chuck).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), ftepAdmin).andExpect(status().isOk());

        getServiceFromApi(service2.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service2.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service2.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), ftepAdmin).andExpect(status().isOk());

        getServiceFromApi(service3.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service3.getId(), bob).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), ftepAdmin).andExpect(status().isOk());
    }

    private ResultActions getServiceFromApi(Long serviceId, User user) throws Exception {
        return mockMvc.perform(get("/api/services/" + serviceId).header("REMOTE_USER", user.getName()));
    }

    private void createReadAce(Long id, String principal) {
        ObjectIdentity oi = new ObjectIdentityImpl(FtepService.class, id);
        createAce(oi, new PrincipalSid(principal), BasePermission.READ);
    }

    private void createGroupReadAce(Long id, GrantedAuthority group) {
        ObjectIdentity oi = new ObjectIdentityImpl(FtepService.class, id);
        createAce(oi, new GrantedAuthoritySid(group), BasePermission.READ);
    }

    private void createAce(ObjectIdentity oi, Sid sid, Permission p) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(ftepAdmin.getName(), "N/A", "ROLE_ADMIN"));

        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }

        acl.insertAce(acl.getEntries().size(), p, sid, true);
        aclService.updateAcl(acl);
    }

}