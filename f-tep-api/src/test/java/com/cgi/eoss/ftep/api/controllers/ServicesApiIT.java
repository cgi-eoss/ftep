package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.security.FtepCustomPermission;
import com.cgi.eoss.ftep.security.FtepPermission;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.ftep.api.TestUtil.userUri;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ServicesApiIT {

    private static final JsonPath OBJ_ID_JSONPATH = JsonPath.compile("$.id");

    @Autowired
    private ServiceDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private FtepSecurityService securityService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepGuest;
    private User ftepUser;
    private User ftepExpertUser;
    private User ftepContentAuthority;
    private User ftepAdmin;

    @Before
    public void setUp() {
        ftepGuest = new User("ftep-guest");
        ftepGuest.setRole(Role.GUEST);
        ftepUser = new User("ftep-user");
        ftepUser.setRole(Role.USER);
        ftepUser.getWallet().setBalance(100);
        ftepExpertUser = new User("ftep-expert-user");
        ftepExpertUser.setRole(Role.EXPERT_USER);
        ftepExpertUser.getWallet().setBalance(100);
        ftepContentAuthority = new User("ftep-content-authority");
        ftepContentAuthority.setRole(Role.CONTENT_AUTHORITY);
        ftepContentAuthority.getWallet().setBalance(100);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(ftepGuest, ftepUser, ftepExpertUser, ftepContentAuthority, ftepAdmin));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.services").exists());
    }

    @Test
    public void testGet() throws Exception {
        FtepService service = new FtepService("service-1", ftepUser, "dockerTag");
        service.setStatus(FtepService.Status.AVAILABLE);
        dataService.save(service);

        mockMvc.perform(get("/api/services").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services[0].id").value(service.getId()))
                .andExpect(jsonPath("$._embedded.services[0].name").value("service-1"))
                .andExpect(jsonPath("$._embedded.services[0].dockerTag").value("dockerTag"))
                .andExpect(jsonPath("$._embedded.services[0].owner.id").value(ftepUser.getId()))
                .andExpect(jsonPath("$._embedded.services[0].access.published").value(false))
                .andExpect(jsonPath("$._embedded.services[0].access.publishRequested").value(false))
                .andExpect(jsonPath("$._embedded.services[0].access.currentLevel").value("ADMIN"))
                .andExpect(jsonPath("$._embedded.services[0]._links.self.href").value(endsWith("/services/" + service.getId())))
                .andExpect(jsonPath("$._embedded.services[0]._links.owner.href").value(endsWith("/services/" + service.getId() + "/owner{?projection}")));
    }

    @Test
    public void testGetDetailed() throws Exception {
        FtepService service = new FtepService("service-1", ftepUser, "dockerTag");
        service.setStatus(FtepService.Status.AVAILABLE);
        service.setServiceDescriptor(FtepServiceDescriptor.builder()
                .id("service-1")
                .title("Test Service for DetailedFtepService projection")
                .description("This service tests the F-TEP automatic zcfg file generation")
                .version("1.0")
                .serviceProvider("ftep_service_wrapper")
                .serviceType("python")
                .storeSupported(false)
                .statusSupported(false)
                .dataInputs(ImmutableList.of(
                        FtepServiceDescriptor.Parameter.builder()
                                .id("inputfile")
                                .title("Input File 1")
                                .description("The input data file")
                                .minOccurs(1)
                                .maxOccurs(1)
                                .data(FtepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                                .defaultAttrs(ImmutableMap.<String, String>builder()
                                        .put("dataType", "string")
                                        .build())
                                .build()))
                .dataOutputs(ImmutableList.of(
                        FtepServiceDescriptor.Parameter.builder()
                                .id("result")
                                .title("URL to service output")
                                .description("see title")
                                .data(FtepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                                .defaultAttrs(ImmutableMap.<String, String>builder()
                                        .put("dataType", "string").build())
                                .build()))
                .build());
        service.setEasyModeServiceDescriptor(FtepServiceDescriptor.builder()
                .dataInputs(ImmutableList.of(
                        FtepServiceDescriptor.Parameter.builder()
                                .id("easyinput")
                                .title("Easy mode")
                                .description("The easy mode input string")
                                .minOccurs(1)
                                .maxOccurs(1)
                                .data(FtepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                                .defaultAttrs(ImmutableMap.<String, String>builder()
                                        .put("dataType", "string")
                                        .build())
                                .build()))
                .build());
        service.setEasyModeParameterTemplate("{{sometemplate}}");
        dataService.save(service);

        mockMvc.perform(get(getServiceUrl(service) + "?projection=detailedFtepService").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(service.getId()))
                .andExpect(jsonPath("$.name").value("service-1"))
                .andExpect(jsonPath("$.dockerTag").value("dockerTag"))
                .andExpect(jsonPath("$.owner.id").value(ftepUser.getId()))
                .andExpect(jsonPath("$.serviceDescriptor.dataInputs[0].id").value("inputfile"))
                .andExpect(jsonPath("$.easyModeServiceDescriptor.dataInputs[0].id").value("easyinput"))
                .andExpect(jsonPath("$.easyModeParameterTemplate").value("{{sometemplate}}"))
                .andExpect(jsonPath("$.access.published").value(false))
                .andExpect(jsonPath("$.access.publishRequested").value(false))
                .andExpect(jsonPath("$.access.currentLevel").value("ADMIN"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/services/" + service.getId())))
                .andExpect(jsonPath("$._links.owner.href").value(endsWith("/services/" + service.getId() + "/owner{?projection}")));
    }

    @Test
    public void testGetFilter() throws Exception {
        FtepService service = new FtepService("service-1", ftepAdmin, "dockerTag");
        service.setStatus(FtepService.Status.AVAILABLE);
        FtepService service2 = new FtepService("service-2", ftepAdmin, "dockerTag");
        service2.setStatus(FtepService.Status.IN_DEVELOPMENT);
        FtepService service3 = new FtepService("service-3", ftepAdmin, "dockerTag");
        service3.setStatus(FtepService.Status.IN_DEVELOPMENT);
        dataService.save(ImmutableSet.of(service, service2, service3));

        createAce(new ObjectIdentityImpl(FtepService.class, service.getId()), new GrantedAuthoritySid(FtepPermission.PUBLIC), BasePermission.READ);
        createAce(new ObjectIdentityImpl(FtepService.class, service.getId()), new GrantedAuthoritySid(FtepPermission.PUBLIC), FtepCustomPermission.LAUNCH);
        createReadAce(new ObjectIdentityImpl(FtepService.class, service3.getId()), ftepExpertUser.getName());

        // service1 is returned as it is AVAILABLE
        // service2 is not returned as it is IN_DEVELOPMENT and not readable by the user
        // service3 is returned as the user has been granted read permission

        mockMvc.perform(get("/api/services").header("REMOTE_USER", ftepExpertUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services.length()").value(2))
                .andExpect(jsonPath("$._embedded.services[0].id").value(service.getId()))
                .andExpect(jsonPath("$._embedded.services[0].access.published").value(true))
                .andExpect(jsonPath("$._embedded.services[0].access.currentLevel").value("SERVICE_USER"))
                .andExpect(jsonPath("$._embedded.services[1].id").value(service3.getId()))
                .andExpect(jsonPath("$._embedded.services[1].access.published").value(false))
                .andExpect(jsonPath("$._embedded.services[1].access.currentLevel").value("READ"));
    }

    @Test
    public void testCreateWithValidRole() throws Exception {
        mockMvc.perform(post("/api/services").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(mockMvc, ftepAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")));
        mockMvc.perform(post("/api/services").header("REMOTE_USER", ftepContentAuthority.getName()).content("{\"name\": \"service-2\", \"dockerTag\": \"dockerTag\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")));

        assertThat(dataService.getByName("service-1"), is(notNullValue()));
        assertThat(dataService.getByName("service-1").getOwner(), is(ftepAdmin));
        assertThat(dataService.getByName("service-2"), is(notNullValue()));
        assertThat(dataService.getByName("service-2").getOwner(), is(ftepContentAuthority));
    }

    @Test
    public void testCreateWithInvalidRole() throws Exception {
        mockMvc.perform(post("/api/services").header("REMOTE_USER", ftepUser.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(mockMvc, ftepUser) + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testWriteAccessControl() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/services").header("REMOTE_USER", ftepAdmin.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(mockMvc, ftepAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")))
                .andReturn();

        String serviceLocation = result.getResponse().getHeader("Location");

        // WARNING: The underlying object *is* modified by these calls, due to ORM state held in the mockMvc layer
        // This should not happen in production and must be verified in the full test harness

        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", ftepUser.getName()).content("{\"name\": \"service-1-user-updated\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", ftepGuest.getName()).content("{\"name\": \"service-1-guest-updated\"}"))
                .andExpect(status().isForbidden());

        // Allow the user to write to the object
        createWriteAce(new ObjectIdentityImpl(FtepService.class, getJsonObjectId(serviceLocation)), ftepUser.getName());

        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", ftepUser.getName()).content("{\"name\": \"service-1-user-updated\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", ftepGuest.getName()).content("{\"name\": \"service-1-guest-updated\"}"))
                .andExpect(status().isForbidden());
    }

    private Long getJsonObjectId(String location) throws Exception {
        String jsonResult = mockMvc.perform(get(location).header("REMOTE_USER", ftepAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return ((Number) OBJ_ID_JSONPATH.read(jsonResult)).longValue();
    }

    private void createWriteAce(ObjectIdentity oi, String principal) {
        createReadAce(oi, principal);
        createAce(oi, new PrincipalSid(principal), BasePermission.WRITE);
    }

    private void createReadAce(ObjectIdentity oi, String principal) {
        createAce(oi, new PrincipalSid(principal), BasePermission.READ);
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

    private String getServiceUrl(FtepService svc) throws Exception {
        return JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/services/" + svc.getId()).header("REMOTE_USER", ftepContentAuthority.getName()))
                        .andReturn().getResponse().getContentAsString()
        );
    }

}