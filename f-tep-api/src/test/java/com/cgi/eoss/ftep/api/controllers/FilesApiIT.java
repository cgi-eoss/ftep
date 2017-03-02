package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.ApiConfig;
import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class FilesApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private FtepFileDataService fileDataService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogueService catalogueService;

    private FtepFile testFile1;

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
        testFile1 = new FtepFile(URI.create("ftep://refData/2/" + fileUuid), fileUuid);
        testFile1.setOwner(ftepAdmin);
        testFile1.setFilename("testFile1");
        fileDataService.save(testFile1);
    }

    @After
    public void tearDown() throws Exception {
        fileDataService.deleteAll();
    }

    @Test
    public void saveRefData() throws Exception {
        Resource fileResource = new ClassPathResource("/testFile1", FilesApiIT.class);
        MockMultipartFile uploadFile = new MockMultipartFile("file", "testFile1", "text/plain", fileResource.getInputStream());

        mockMvc.perform(fileUpload("/api/files/refData/new").file(uploadFile).header("REMOTE_USER", ftepUser.getName()).param("geometry", "POINT(0 0)"))
                .andExpect(status().isOk());
        verify(catalogueService).ingestReferenceData(any(), any());
    }

    @Test
    public void downloadFile() throws Exception {
        Resource response = new ClassPathResource("/testFile1", FilesApiIT.class);
        when(catalogueService.getAsResource(testFile1)).thenReturn(response);

        mockMvc.perform(get("/api/files/" + testFile1.getId() + "/dl").header("REMOTE_USER", ftepUser.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/files/" + testFile1.getId() + "/dl").header("REMOTE_USER", ftepAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"testFile1\""))
                .andExpect(content().bytes(ByteStreams.toByteArray(response.getInputStream())));
    }

}