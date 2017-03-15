package com.cgi.eoss.ftep.services;

import com.cgi.eoss.ftep.model.FtepService;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DefaultFtepServicesTest {

    @Test
    public void getDefaultServices() throws Exception {
        Set<FtepService> defaultServices = DefaultFtepServices.getDefaultServices();
        assertThat(defaultServices.size(), is(8));

        FtepService landCoverS2 = defaultServices.stream().filter(s -> s.getName().equals("LandCoverS2")).findFirst().get();
        assertThat(landCoverS2.getServiceDescriptor().getDataInputs().size(), is(6));
        assertThat(landCoverS2.getContextFiles().size(), is(8));
        assertThat(landCoverS2.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("Dockerfile") && !f.isExecutable()), is(true));
        assertThat(landCoverS2.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("workflow.sh") && f.isExecutable()), is(true));
    }

}