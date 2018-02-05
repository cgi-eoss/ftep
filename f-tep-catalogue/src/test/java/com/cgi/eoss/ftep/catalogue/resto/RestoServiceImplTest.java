package com.cgi.eoss.ftep.catalogue.resto;

import com.cgi.eoss.ftep.model.FtepFile;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 */
public class RestoServiceImplTest {

    private MockWebServer webServer;

    private RestoServiceImpl restoService;

    @Before
    public void setUp() throws Exception {
        this.webServer = new MockWebServer();
        this.webServer.start();

        this.restoService = new RestoServiceImpl(webServer.url("/resto/").toString(), "restouser", "restopass");
    }

    @After
    public void tearDown() throws Exception {
        this.webServer.shutdown();
    }

    @Test
    public void getGeoJson() throws Exception {
        UUID uuid = UUID.randomUUID();
        FtepFile ftepFile = new FtepFile(URI.create("sentinel2:///" + uuid), uuid);

        webServer.enqueue(new MockResponse().setBody(getSentinel2Body()));

        Feature geoJson = (Feature) restoService.getGeoJson(ftepFile);
        assertThat(geoJson, is(notNullValue()));
        assertThat(geoJson.getGeometry(), is(new Polygon(
                new LngLatAlt(-92.908671305765, 16.231374140309),
                new LngLatAlt(-92.94035570476, 16.237381496473),
                new LngLatAlt(-92.941110416354, 16.234107082063),
                new LngLatAlt(-93.175080295093, 16.277819019559),
                new LngLatAlt(-93.174514982992, 16.280282788891),
                new LngLatAlt(-92.908649214991, 16.280813604616),
                new LngLatAlt(-92.908671305765, 16.231374140309),
                new LngLatAlt(-92.908671305765, 16.231374140309)
        )));
    }

    private String getSentinel2Body() {
        return "{\"type\":\"FeatureCollection\",\"properties\":{\"id\":\"e7e8b322-136a-5600-876f-659e61ae6978\",\"totalResults\":1,\"exactCount\":true,\"startIndex\":1,\"itemsPerPage\":1,\"query\":{\"originalFilters\":{\"identifier\":\"6f8cc1c8-bcdd-5500-b69a-d956dfdfc2e6\",\"collection\":\"*\"},\"appliedFilters\":{\"identifier\":\"6f8cc1c8-bcdd-5500-b69a-d956dfdfc2e6\",\"collection\":\"*\"},\"analysis\":{\"query\":null,\"language\":\"en\",\"analyze\":{\"What\":[],\"When\":[],\"Where\":[],\"Errors\":[],\"Explained\":[]},\"processingTime\":5.0067901611328e-6},\"processingTime\":0.02421498298645},\"links\":[{\"rel\":\"self\",\"type\":\"application\\/json\",\"title\":\"self\",\"href\":\"http:\\/\\/ftep-resto\\/resto\\/api\\/collections\\/search.json?&identifier=6f8cc1c8-bcdd-5500-b69a-d956dfdfc2e6\"},{\"rel\":\"search\",\"type\":\"application\\/opensearchdescription+xml\",\"title\":\"OpenSearch Description Document\",\"href\":\"http:\\/\\/ftep-resto\\/resto\\/api\\/collections\\/describe.xml\"}]},\"features\":[{\"type\":\"Feature\",\"id\":\"6f8cc1c8-bcdd-5500-b69a-d956dfdfc2e6\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-92.908671305765,16.231374140309],[-92.94035570476,16.237381496473],[-92.941110416354,16.234107082063],[-93.175080295093,16.277819019559],[-93.174514982992,16.280282788891],[-92.908649214991,16.280813604616],[-92.908671305765,16.231374140309],[-92.908671305765,16.231374140309]]]},\"properties\":{\"collection\":\"sentinel2\",\"license\":{\"licenseId\":\"unlicensed\",\"hasToBeSigned\":\"never\",\"grantedCountries\":null,\"grantedOrganizationCountries\":null,\"grantedFlags\":null,\"viewService\":\"public\",\"signatureQuota\":-1,\"description\":{\"shortName\":\"No license\"}},\"productIdentifier\":\"S2A_OPER_PRD_MSIL1C_PDMC_20161030T223944_R083_V20161030T164852_20161030T164852\",\"parentIdentifier\":null,\"title\":\"S2A_OPER_PRD_MSIL1C_PDMC_20161030T223944_R083_V20161030T164852_20161030T164852\",\"description\":null,\"organisationName\":null,\"startDate\":\"Z\",\"completionDate\":\"Z\",\"productType\":null,\"processingLevel\":null,\"platform\":null,\"instrument\":null,\"resolution\":0,\"sensorMode\":null,\"orbitNumber\":0,\"quicklook\":null,\"thumbnail\":null,\"updated\":\"2017-05-21T10:32:33.71368Z\",\"published\":\"2017-05-21T10:32:33.71368Z\",\"snowCover\":0,\"cloudCover\":0,\"keywords\":[],\"centroid\":{\"type\":\"Point\",\"coordinates\":[-93.041864755042,16.256093872463]},\"ftepFileType\":\"EXTERNAL_PRODUCT\",\"services\":[],\"links\":[{\"rel\":\"self\",\"type\":\"application\\/json\",\"title\":\"GeoJSON link for 6f8cc1c8-bcdd-5500-b69a-d956dfdfc2e6\",\"href\":\"http:\\/\\/ftep-resto\\/resto\\/collections\\/sentinel2\\/6f8cc1c8-bcdd-5500-b69a-d956dfdfc2e6.json?&lang=en\"}]}}]}";
    }

}