package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.api.ApiTestConfig;
import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class FtepFilesApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private FtepFileDataService fileDataService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogueService catalogueService;

    private FtepFile testFile1;
    private FtepFile testFile2;

    private User ftepUser;
    private User ftepAdmin;

    @Before
    public void setUp() throws Exception {
        ftepUser = new User("ftep-user");
        ftepUser.setRole(Role.USER);
        ftepAdmin = new User("ftep-admin");
        ftepAdmin.setRole(Role.ADMIN);
        userDataService.save(ImmutableSet.of(ftepUser, ftepAdmin));

        UUID fileUuid = UUID.randomUUID();
        testFile1 = new FtepFile(URI.create("ftep://refData/2/testFile1"), fileUuid);
        testFile1.setOwner(ftepAdmin);
        testFile1.setFilename("testFile1");
        testFile1.setType(FtepFile.Type.REFERENCE_DATA);

        UUID file2Uuid = UUID.randomUUID();
        testFile2 = new FtepFile(URI.create("ftep://outputProduct/job1/testFile2"), file2Uuid);
        testFile2.setOwner(ftepAdmin);
        testFile2.setFilename("testFile2");
        testFile2.setType(FtepFile.Type.OUTPUT_PRODUCT);

        fileDataService.save(ImmutableSet.of(testFile1, testFile2));
    }

    @After
    public void tearDown() throws Exception {
        fileDataService.deleteAll();
    }

    @Test
    public void testGet() throws Exception {
        mockMvc.perform(get("/api/ftepFiles/" + testFile1.getId()).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/ftepFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/ftepFiles/" + testFile1.getId() + "/dl")));

        mockMvc.perform(get("/api/ftepFiles/search/findByType?type=REFERENCE_DATA").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.ftepFiles").isArray())
                .andExpect(jsonPath("$._embedded.ftepFiles.length()").value(1))
                .andExpect(jsonPath("$._embedded.ftepFiles[0].filename").value("testFile1"));

        // Results are filtered by ACL
        mockMvc.perform(get("/api/ftepFiles/search/findByType?type=REFERENCE_DATA").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.ftepFiles").isArray())
                .andExpect(jsonPath("$._embedded.ftepFiles.length()").value(0));

        mockMvc.perform(get("/api/ftepFiles/search/findByType?type=OUTPUT_PRODUCT").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.ftepFiles").isArray())
                .andExpect(jsonPath("$._embedded.ftepFiles.length()").value(1))
                .andExpect(jsonPath("$._embedded.ftepFiles[0].filename").value("testFile2"));
    }

    @Test
    public void testFindByOwner() throws Exception {
        String ftepUserUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + ftepUser.getId()).header("REMOTE_USER", ftepAdmin.getName())).andReturn().getResponse().getContentAsString()
        );
        String ftepAdminUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + ftepAdmin.getId()).header("REMOTE_USER", ftepAdmin.getName())).andReturn().getResponse().getContentAsString()
        );

        mockMvc.perform(get("/api/ftepFiles/search/findByOwner?owner="+ftepUserUrl).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.ftepFiles").isArray())
                .andExpect(jsonPath("$._embedded.ftepFiles.length()").value(0));

        mockMvc.perform(get("/api/ftepFiles/search/findByOwner?owner="+ftepAdminUrl).header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.ftepFiles").isArray())
                .andExpect(jsonPath("$._embedded.ftepFiles.length()").value(2));
    }

    @Test
    public void testSaveRefData() throws Exception {
        Resource fileResource = new ClassPathResource("/testFile1", FtepFilesApiIT.class);
        MockMultipartFile uploadFile = new MockMultipartFile("file", "testFile1", "text/plain", fileResource.getInputStream());

        when(catalogueService.ingestReferenceData(any(), any())).thenReturn(testFile1);
        mockMvc.perform(fileUpload("/api/ftepFiles/refData").file(uploadFile).header("REMOTE_USER", ftepUser.getName()).param("geometry", "POINT(0 0)"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/ftepFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/ftepFiles/" + testFile1.getId() + "/dl")));
        verify(catalogueService).ingestReferenceData(any(), any());
    }

    @Test
    public void testDownloadFile() throws Exception {
        Resource response = new ClassPathResource("/testFile1", FtepFilesApiIT.class);
        when(catalogueService.getAsResource(testFile1)).thenReturn(response);

        mockMvc.perform(get("/api/ftepFiles/" + testFile1.getId() + "/dl").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/ftepFiles/" + testFile1.getId() + "/dl").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"testFile1\""))
                .andExpect(content().bytes(ByteStreams.toByteArray(response.getInputStream())));
    }

}