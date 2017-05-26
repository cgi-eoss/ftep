package com.cgi.eoss.ftep.catalogue.geoserver;

import com.cgi.eoss.ftep.catalogue.CatalogueConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CatalogueConfig.class})
@TestPropertySource("classpath:test-catalogue.properties")
public class GeoserverServiceImplTest {

    @Autowired
    private GeoserverService geoserverService;

    @Test
    public void isIngestibleFile() throws Exception {
        assertThat(geoserverService.isIngestibleFile("somefile.tif"), is(true));
        assertThat(geoserverService.isIngestibleFile("somefile.tiff"), is(true));
        assertThat(geoserverService.isIngestibleFile("somefile.toff"), is(false));
    }

}