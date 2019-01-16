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
        assertThat(defaultServices.size(), is(11));

        FtepService forestChangeS2 = defaultServices.stream().filter(s -> s.getName().equals("ForestChangeS2")).findFirst().get();
        assertThat(forestChangeS2.getServiceDescriptor().getDataInputs().size(), is(5));
        assertThat(forestChangeS2.getContextFiles().size(), is(7));
        assertThat(forestChangeS2.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("Dockerfile") && !f.isExecutable()), is(true));
        assertThat(forestChangeS2.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("workflow.sh") && f.isExecutable()), is(true));

        FtepService vegInd = defaultServices.stream().filter(s -> s.getName().equals("VegetationIndices")).findFirst().get();
        assertThat(vegInd.getServiceDescriptor().getDataInputs().size(), is(5));
        assertThat(vegInd.getContextFiles().size(), is(6));
        assertThat(vegInd.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("Dockerfile") && !f.isExecutable()), is(true));
        assertThat(vegInd.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("workflow.sh") && f.isExecutable()), is(true));

        FtepService vegIndParallel = defaultServices.stream().filter(s -> s.getName().equals("VegetationIndicesParallel")).findFirst().get();
        assertThat(vegIndParallel.getServiceDescriptor().getDataInputs().size(), is(4));
        assertThat(vegIndParallel.getContextFiles().size(), is(6));
        assertThat(vegIndParallel.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("Dockerfile") && !f.isExecutable()), is(true));
        assertThat(vegIndParallel.getContextFiles().stream().anyMatch(f -> f.getFilename().equals("workflow.sh") && f.isExecutable()), is(true));
    }

}