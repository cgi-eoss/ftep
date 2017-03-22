package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.ServiceStatus;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.orchestrator.zoo.ZooManagerClient;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ContentAuthorityApiIT {

    private static final String TEST_SERVICE_NAME = "SNAP";
    private static final int DEFAULT_SERVICE_COUNT = DefaultFtepServices.getDefaultServices().size();

    @MockBean
    private ZooManagerClient zooManagerClient;

    @Captor
    private ArgumentCaptor<List<FtepService>> argumentCaptor;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private JobDataService jobDataService;

    @Autowired
    private MockMvc mockMvc;

    private User ftepUser;
    private User ftepAdmin;

    @Before
    public void setUp() {
        ftepUser = new User("ftep-user");
        ftepUser.setRole(Role.USER);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(ftepUser, ftepAdmin));
    }

    @Test
    public void testRestoreDefaultServices() throws Exception {
        // Check nothing exists already
        assertThat(serviceDataService.getAll().size(), is(0));

        // Restore default services by HTTP POST
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        assertThat(serviceDataService.getAll().size(), is(DEFAULT_SERVICE_COUNT));

        // Remove one of the default services
        serviceDataService.delete(serviceDataService.getByName(TEST_SERVICE_NAME));
        assertThat(serviceDataService.getByName(TEST_SERVICE_NAME), is(nullValue()));

        // Restore default services again
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        // Assert the deleted service has been recovered
        assertThat(serviceDataService.getAll().size(), is(DEFAULT_SERVICE_COUNT));
        assertThat(serviceDataService.getByName(TEST_SERVICE_NAME), is(notNullValue()));

        // Assert the default services are visible to the public
        mockMvc.perform(get("/api/services").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services.length()").value(DEFAULT_SERVICE_COUNT))
                .andExpect(jsonPath("$._embedded.services[?(@.name == '" + TEST_SERVICE_NAME + "')]").exists());
    }

    @Test
    public void testWpsSyncAllPublic() throws Exception {
        // Ensure default services are available
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        verify(zooManagerClient).updateActiveZooServices(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().size(), is(DEFAULT_SERVICE_COUNT));
    }

    @Test
    public void testWpsSyncAllPublicInDevelopment() throws Exception {
        // Ensure default services are available
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        // Set one service to IN_DEVELOPMENT (i.e. not in the "syncAllPublic" collection)
        FtepService snapService = serviceDataService.getByName(TEST_SERVICE_NAME);
        snapService.setStatus(ServiceStatus.IN_DEVELOPMENT);
        serviceDataService.save(snapService);

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        verify(zooManagerClient).updateActiveZooServices(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().size(), is(DEFAULT_SERVICE_COUNT - 1));
    }

    @Test
    public void testUnpublishPublish() throws Exception {
        // Ensure default services are available
        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/unpublish/" + serviceDataService.getByName(TEST_SERVICE_NAME).getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/publish/" + serviceDataService.getByName(TEST_SERVICE_NAME).getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contentAuthority/services/wps/syncAllPublic").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        verify(zooManagerClient, times(3)).updateActiveZooServices(argumentCaptor.capture());

        assertThat(argumentCaptor.getAllValues().get(0).size(), is(DEFAULT_SERVICE_COUNT));
        assertThat(argumentCaptor.getAllValues().get(1).size(), is(DEFAULT_SERVICE_COUNT - 1));
        assertThat(argumentCaptor.getAllValues().get(2).size(), is(DEFAULT_SERVICE_COUNT));
    }

    @Test
    public void test() throws Exception {

        mockMvc.perform(post("/api/contentAuthority/services/restoreDefaults").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk());

        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("inputfile", "ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/L1C_MSI/2016/10/30/S2A_OPER_PRD_MSIL1C_PDMC_20161030T223944_R083_V20161030T164852_20161030T164852.zip")
                .put("vegIndex", "GEMI")
                .put("crs", "EPSG:32615")
                .put("aoi", "POLYGON((-93.17882537841795 16.29905101458182,-93.17882537841795 16.20082812440876,-92.89867401123045 16.20082812440876,-92.89867401123045 16.29905101458182,-93.17882537841795 16.29905101458182))")
                .put("targetResolution", "20")
                .build();

        Job job = jobDataService.buildNew(UUID.randomUUID().toString(), ftepAdmin.getName(), "VegetationIndices", inputs);
        System.out.println(job.getConfig().getInputs());
    }

}