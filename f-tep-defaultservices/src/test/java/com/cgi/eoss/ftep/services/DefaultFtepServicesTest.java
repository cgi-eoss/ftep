package com.cgi.eoss.ftep.services;

import com.cgi.eoss.ftep.model.internal.CompleteFtepService;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DefaultFtepServicesTest {

    @Test
    public void getDefaultServices() throws Exception {
        Set<CompleteFtepService> defaultServices = DefaultFtepServices.getDefaultServices();
        assertThat(defaultServices.size(), is(7));

        CompleteFtepService landCoverS2 = defaultServices.stream().filter(s -> s.getService().getName().equals("LandCoverS2")).findFirst().get();
        assertThat(landCoverS2.getService().getServiceDescriptor().getDataInputs().size(), is(7));
        assertThat(landCoverS2.getFiles().size(), is(8));
    }

}