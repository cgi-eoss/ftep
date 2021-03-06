package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.api.controllers.AclsApi;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.GroupDataService;
import com.cgi.eoss.ftep.persistence.service.JobConfigDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceFileDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.security.FtepCustomPermission;
import com.cgi.eoss.ftep.security.FtepPermission;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private ServiceFileDataService serviceFileDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private GroupDataService groupDataService;

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private AclsApi aclsApi;

    @Autowired
    private FtepSecurityService securityService;

    @Autowired
    private JobConfigDataService jobConfigDataService;

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

    private FtepServiceContextFile file1;

    private JobConfig service2config;

    @Before
    public void setUp() {
        alice = new User("alice");
        alice.setRole(Role.USER);
        alice.getWallet().setBalance(100);
        bob = new User("bob");
        bob.setRole(Role.USER);
        bob.getWallet().setBalance(100);
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
        service1.setStatus(FtepService.Status.AVAILABLE);
        service2 = new FtepService("service-2", ftepAdmin, "dockerTag");
        service2.setStatus(FtepService.Status.IN_DEVELOPMENT);
        service3 = new FtepService("service-3", ftepAdmin, "dockerTag");
        service3.setStatus(FtepService.Status.IN_DEVELOPMENT);
        serviceDataService.save(ImmutableSet.of(service1, service2, service3));

        file1 = new FtepServiceContextFile(service2, "file1");
        serviceFileDataService.save(file1);

        service2config = jobConfigDataService.save(new JobConfig(ftepAdmin, service2));
        createGroupAce(service2config.getId(), JobConfig.class, beta, BasePermission.READ);
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
        getService(service1.getId(), alice).andExpect(status().isOk());
        getService(service1.getId(), bob).andExpect(status().isOk());
        getService(service1.getId(), chuck).andExpect(status().isForbidden());
        getService(service1.getId(), ftepAdmin).andExpect(status().isOk());

        getService(service2.getId(), alice).andExpect(status().isForbidden());
        getService(service2.getId(), bob).andExpect(status().isForbidden());
        getService(service2.getId(), chuck).andExpect(status().isForbidden());
        getService(service2.getId(), ftepAdmin).andExpect(status().isOk());

        getService(service3.getId(), alice).andExpect(status().isForbidden());
        getService(service3.getId(), bob).andExpect(status().isForbidden());
        getService(service3.getId(), chuck).andExpect(status().isForbidden());
        getService(service3.getId(), ftepAdmin).andExpect(status().isOk());
    }

    @Test
    public void testUserAclAccess() throws Exception {
        // Grant read access for service-1 to alice - but unused as it is already AVAILABLE
        createAce(service1.getId(), FtepService.class, alice.getName(), BasePermission.READ);
        // Grant read access for service-2 to bob
        createAce(service2.getId(), FtepService.class, bob.getName(), BasePermission.READ);
        // Grant read access for service-3 to alice and bob
        createAce(service3.getId(), FtepService.class, alice.getName(), BasePermission.READ);
        createAce(service3.getId(), FtepService.class, bob.getName(), BasePermission.READ);

        getService(service1.getId(), alice).andExpect(status().isOk());
        getService(service1.getId(), bob).andExpect(status().isOk());
        getService(service1.getId(), chuck).andExpect(status().isForbidden());
        getService(service1.getId(), ftepAdmin).andExpect(status().isOk());

        getService(service2.getId(), alice).andExpect(status().isForbidden());
        getService(service2.getId(), bob).andExpect(status().isOk());
        getService(service2.getId(), chuck).andExpect(status().isForbidden());
        getService(service2.getId(), ftepAdmin).andExpect(status().isOk());

        getService(service3.getId(), alice).andExpect(status().isOk());
        getService(service3.getId(), bob).andExpect(status().isOk());
        getService(service3.getId(), chuck).andExpect(status().isForbidden());
        getService(service3.getId(), ftepAdmin).andExpect(status().isOk());
    }

    @Test
    public void testGroupAclAccess() throws Exception {
        // Verify groups can read themselves
        getGroupFromApi(alpha.getId(), alice).andExpect(status().isOk());
        getGroupFromApi(alpha.getId(), bob).andExpect(status().isOk());
        getGroupFromApi(beta.getId(), alice).andExpect(status().isOk());
        getGroupFromApi(beta.getId(), bob).andExpect(status().isForbidden());

        // Grant read access for service-2 to alpha (alice & bob)
        createGroupAce(service2.getId(), FtepService.class, alpha, BasePermission.READ);
        // Grant read access for service-3 to beta (alice)
        createGroupAce(service3.getId(), FtepService.class, beta, BasePermission.READ);

        getService(service1.getId(), alice).andExpect(status().isOk());
        getService(service1.getId(), bob).andExpect(status().isOk());
        getService(service1.getId(), chuck).andExpect(status().isForbidden());
        getService(service1.getId(), ftepAdmin).andExpect(status().isOk());

        getService(service2.getId(), alice).andExpect(status().isOk());
        getService(service2.getId(), bob).andExpect(status().isOk());
        getService(service2.getId(), chuck).andExpect(status().isForbidden());
        getService(service2.getId(), ftepAdmin).andExpect(status().isOk());

        getService(service3.getId(), alice).andExpect(status().isOk());
        getService(service3.getId(), bob).andExpect(status().isForbidden());
        getService(service3.getId(), chuck).andExpect(status().isForbidden());
        getService(service3.getId(), ftepAdmin).andExpect(status().isOk());
    }

    @Test
    public void testUserGrantedAuthorities() throws Exception {
        mockMvc.perform(get("/api/currentUser/grantedAuthorities").header("REMOTE_USER", alice.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsInAnyOrder(authoritiesToStrings(alpha, beta, FtepPermission.PUBLIC, Role.USER))));
        mockMvc.perform(get("/api/currentUser/grantedAuthorities").header("REMOTE_USER", bob.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsInAnyOrder(authoritiesToStrings(alpha, FtepPermission.PUBLIC, Role.USER))));
        MockHttpSession chuckSession = (MockHttpSession) mockMvc.perform(get("/api/currentUser/grantedAuthorities").header("REMOTE_USER", chuck.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsInAnyOrder(authoritiesToStrings(FtepPermission.PUBLIC, Role.GUEST))))
                .andReturn().getRequest().getSession();

        mockMvc.perform(patch("/api/groups/" + beta.getId()).content("{\"members\":[\"" +
                getUserUrl(alice) + getUserUrl(chuck) + "\"]}").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isNoContent());

        // Prove the existing session is updated
        mockMvc.perform(get("/api/currentUser/grantedAuthorities").header("REMOTE_USER", chuck.getName()).session(chuckSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsInAnyOrder(authoritiesToStrings(beta, FtepPermission.PUBLIC, Role.GUEST))));
    }

    @Test
    public void testAclInheritance() throws Exception {
        // bob is in alpha but not beta -> bob cannot read beta
        getGroupFromApi(beta.getId(), bob).andExpect(status().isForbidden());

        // Set beta's ACL to inherit from alpha's ACL, i.e. letting members of alpha also see beta
        setParentAcl(Group.class, alpha.getId(), Group.class, ImmutableSet.of(beta.getId()));

        // bob can now read beta
        getGroupFromApi(beta.getId(), bob).andExpect(status().isOk());
    }

    @Test
    public void testNoPermissions() throws Exception {
        // No permissions - alice cannot view or update the service files or launch the service
        getServiceFile(file1.getId(), alice).andExpect(status().isForbidden());
        patchServiceFile(file1.getId(), alice).andExpect(status().isForbidden());
        launchService(service2config, alice).andExpect(status().isForbidden());
    }

    @Test
    public void testServiceWithGroupRead() throws Exception {
        createGroupAce(service2config.getId(), JobConfig.class, beta, BasePermission.READ);
        // READ permissions to a service-2 for group beta - alice cannot view or update the service files or launch the service
        createGroupAce(service2.getId(), FtepService.class, beta, BasePermission.READ);
        getServiceFile(file1.getId(), alice).andExpect(status().isForbidden());
        patchServiceFile(file1.getId(), alice).andExpect(status().isForbidden());
        launchService(service2config, alice).andExpect(status().isForbidden());
    }

    @Test
    public void testServiceWithGroupUser() throws Exception {
        // LAUNCH & READ permissions to service-2 for group beta - alice can launch the service, but not view/update files
        createGroupAce(service2.getId(), FtepService.class, beta, FtepCustomPermission.LAUNCH);
        createGroupAce(service2.getId(), FtepService.class, beta, BasePermission.READ);
        getServiceFile(file1.getId(), alice).andExpect(status().isForbidden());
        patchServiceFile(file1.getId(), alice).andExpect(status().isForbidden());
        // TODO: launchService(service2config, alice).andExpect(status().isOk());
    }

    @Test
    public void testServiceWithGroupReadonlyDeveloper() throws Exception {
        // SERVICE_READONLY_DEVELOPER permissions to service-2 for group beta - alice can view the service files, but not update them or launch the service
        createGroupAce(service2.getId(), FtepService.class, beta, FtepCustomPermission.READ_SERVICE_FILES);
        createGroupAce(service2.getId(), FtepService.class, beta, BasePermission.READ);
        getServiceFile(file1.getId(), alice).andExpect(status().isOk());
        patchServiceFile(file1.getId(), alice).andExpect(status().isForbidden());
        launchService(service2config, alice).andExpect(status().isForbidden());
    }

    @Test
    public void testServiceWithGroupDeveloper() throws Exception {
        // SERVICE_DEVELOPER permissions to service-2 for group beta - alice can view and update the service files, but not launch the service
        createGroupAce(service2.getId(), FtepService.class, beta, FtepCustomPermission.READ_SERVICE_FILES);
        createGroupAce(service2.getId(), FtepService.class, beta, BasePermission.WRITE);
        createGroupAce(service2.getId(), FtepService.class, beta, BasePermission.READ);
        getServiceFile(file1.getId(), alice).andExpect(status().isOk());
        patchServiceFile(file1.getId(), alice).andExpect(status().isNoContent());
        launchService(service2config, alice).andExpect(status().isForbidden());
    }

    @Test
    public void testServiceWithGroupOperator() throws Exception {
        // SERVICE_OPERATOR permissions to service-2 for group beta - alice can view and update the service files and launch the service
        createGroupAce(service2.getId(), FtepService.class, beta, FtepCustomPermission.LAUNCH);
        createGroupAce(service2.getId(), FtepService.class, beta, FtepCustomPermission.READ_SERVICE_FILES);
        createGroupAce(service2.getId(), FtepService.class, beta, BasePermission.WRITE);
        createGroupAce(service2.getId(), FtepService.class, beta, BasePermission.READ);
        getServiceFile(file1.getId(), alice).andExpect(status().isOk());
        patchServiceFile(file1.getId(), alice).andExpect(status().isNoContent());
        // TODO: launchService(service2config, alice).andExpect(status().isOk());
    }

    private String getUserUrl(User user) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/users/" + user.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.compile("$._links.self.href").read(jsonResult);
    }

    private String[] authoritiesToStrings(GrantedAuthority... authorities) {
        return Arrays.stream(authorities)
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()).toArray(new String[]{});
    }

    private ResultActions getService(Long serviceId, User user) throws Exception {
        return mockMvc.perform(get("/api/services/" + serviceId).header("REMOTE_USER", user.getName()));
    }

    private ResultActions getServiceFile(Long serviceFileId, User user) throws Exception {
        return mockMvc.perform(get("/api/serviceFiles/" + serviceFileId).header("REMOTE_USER", user.getName()));
    }

    private ResultActions patchServiceFile(Long serviceFileId, User user) throws Exception {
        return mockMvc.perform(patch("/api/serviceFiles/" + serviceFileId)
                .content("{\"filename\": \"ServiceFile" + serviceFileId.toString() + "\", \"content\": \"NewContent\"}")
                .header("REMOTE_USER", user.getName()));
    }

    private ResultActions createJobConfig(Long serviceId, User user) throws Exception {
        String serviceUrl = "/api/services/" + serviceId;
        return mockMvc.perform(post("/api/jobConfigs").header("REMOTE_USER", user.getName())
                .content("{\n" +
                        "  \"inputs\" : {\n" +
                        "    \"input1\" : [ \"foo\" ],\n" +
                        "    \"input2\" : [ \"bar1\", \"bar2\" ],\n" +
                        "    \"input3\" : [ \"http://baz/?q=x,y&z={}\" ]\n" +
                        "  },\n" +
                        "  \"label\" : null,\n" +
                        "  \"service\" : \"" + serviceUrl + "\",\n" +
                        "  \"parallelParameters\" : [\"input2\"]\n" +
                        "}"));
    }

    private ResultActions launchService(JobConfig jobConfig, User user) throws Exception {
        return mockMvc.perform(post("/api/jobConfigs/" + jobConfig.getId() + "/launch")
                .header("REMOTE_USER", user.getName()));
    }

    private ResultActions getGroupFromApi(Long groupId, User user) throws Exception {
        return mockMvc.perform(get("/api/groups/" + groupId).header("REMOTE_USER", user.getName()));
    }

    private void createAce(Long id, Class<?> entityClass, String principal, Permission permission) {
        ObjectIdentity oi = new ObjectIdentityImpl(entityClass, id);
        createAce(oi, new PrincipalSid(principal), permission);
    }

    private void createGroupAce(Long id, Class<?> entityClass, GrantedAuthority group, Permission permission) {
        ObjectIdentity oi = new ObjectIdentityImpl(entityClass, id);
        createAce(oi, new GrantedAuthoritySid(group), permission);
    }

    private void createAce(ObjectIdentity oi, Sid sid, Permission p) {
        SecurityContextHolder.getContext().setAuthentication(FtepSecurityService.PUBLIC_AUTHENTICATION);

        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }

        acl.insertAce(acl.getEntries().size(), p, sid, true);
        aclService.updateAcl(acl);
    }

    private void setParentAcl(Class<?> parentObjectClass, Long parentObjectId, Class<?> childObjectClass, Set<Long> childObjectIds) {
        SecurityContextHolder.getContext().setAuthentication(FtepSecurityService.PUBLIC_AUTHENTICATION);
        securityService.setParentAcl(parentObjectClass, parentObjectId, childObjectClass, childObjectIds);
    }
}