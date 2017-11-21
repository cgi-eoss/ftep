package com.cgi.eoss.ftep.search.providers.resto;

import com.cgi.eoss.ftep.search.SearchConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.Feature;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RestoSearchProviderTest {

    private static final String FEATURE_JSON = "{\"type\":\"Feature\",\"id\":\"bb43c06a-0f9d-5425-ad2f-76b00f49a452\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[0,0]},\"properties\":{\"collection\":\"ftepOutputs\",\"license\":{\"licenseId\":\"unlicensed\",\"hasToBeSigned\":\"never\",\"grantedCountries\":null,\"grantedOrganizationCountries\":null,\"grantedFlags\":null,\"viewService\":\"public\",\"signatureQuota\":-1,\"description\":{\"shortName\":\"No license\"}},\"productIdentifier\":\"7adce5a1-e690-4fec-b797-34fd96bf2c96_FTEP_LANDCOVERS2_20171117_133112Z_confusion_matrix.csv\",\"parentIdentifier\":null,\"title\":\"7adce5a1-e690-4fec-b797-34fd96bf2c96_FTEP_LANDCOVERS2_20171117_133112Z_confusion_matrix.csv\",\"description\":null,\"organisationName\":null,\"startDate\":\"Z\",\"completionDate\":\"Z\",\"productType\":null,\"processingLevel\":null,\"platform\":null,\"instrument\":null,\"resolution\":0,\"sensorMode\":null,\"orbitNumber\":0,\"quicklook\":null,\"thumbnail\":null,\"updated\":\"2017-11-17T13:31:14.949303Z\",\"published\":\"2017-11-17T13:31:14.949303Z\",\"snowCover\":0,\"cloudCover\":0,\"keywords\":[],\"centroid\":{\"type\":\"Point\",\"coordinates\":[0,0]},\"jobId\":\"7adce5a1-e690-4fec-b797-34fd96bf2c96\",\"intJobId\":1688,\"serviceName\":\"LandCoverGeotiff\",\"jobOwner\":\"YrjoRauste\",\"jobStartDate\":\"2017-11-17T13:30:51.413Z\",\"jobEndDate\":\"2017-11-17T13:31:28.92Z\",\"filename\":\"FTEP_LANDCOVERS2_20171117_133112Z_confusion_matrix.csv\",\"ftepUrl\":\"ftep:\\/\\/outputProduct\\/7adce5a1-e690-4fec-b797-34fd96bf2c96\\/FTEP_LANDCOVERS2_20171117_133112Z_confusion_matrix.csv\",\"ftepparam\":\"\\\"{}\\\"\",\"ftepFileType\":\"OUTPUT_PRODUCT\",\"services\":{\"download\":{\"url\":\"http:\\/\\/ftep-resto\\/resto\\/collections\\/ftepOutputs\\/bb43c06a-0f9d-5425-ad2f-76b00f49a452\\/download\",\"mimeType\":\"application\\/unknown\",\"size\":145,\"checksum\":\"sha256=df208e8e7fed7f7f9c8177e7cc612c6c42d6fae10bd4a676533e1fa4a5902080\"}},\"links\":[{\"rel\":\"self\",\"type\":\"application\\/json\",\"title\":\"GeoJSON link for bb43c06a-0f9d-5425-ad2f-76b00f49a452\",\"href\":\"http:\\/\\/ftep-resto\\/resto\\/collections\\/ftepOutputs\\/bb43c06a-0f9d-5425-ad2f-76b00f49a452.json?&lang=en\"}]}}";
    private static final String FEATURE_JSON_2 = "{\"type\":\"Feature\",\"id\":\"5aafaeb6-5b0b-50c8-b04c-2312484fac9a\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[0,0]},\"properties\":{\"collection\":\"ftepOutputs\",\"license\":{\"licenseId\":\"unlicensed\",\"hasToBeSigned\":\"never\",\"grantedCountries\":null,\"grantedOrganizationCountries\":null,\"grantedFlags\":null,\"viewService\":\"public\",\"signatureQuota\":-1,\"description\":{\"shortName\":\"No license\"}},\"productIdentifier\":\"b5e31261-6de2-4fa5-a61d-078fefb33a53_B2.img\",\"parentIdentifier\":null,\"title\":\"b5e31261-6de2-4fa5-a61d-078fefb33a53_B2.img\",\"description\":null,\"organisationName\":null,\"startDate\":\"Z\",\"completionDate\":\"Z\",\"productType\":null,\"processingLevel\":null,\"platform\":null,\"instrument\":null,\"resolution\":0,\"sensorMode\":null,\"orbitNumber\":0,\"quicklook\":null,\"thumbnail\":null,\"updated\":\"2017-10-09T10:44:45.76066Z\",\"published\":\"2017-10-09T10:44:45.76066Z\",\"snowCover\":0,\"cloudCover\":0,\"keywords\":[],\"centroid\":{\"type\":\"Point\",\"coordinates\":[0,0]},\"jobId\":\"b5e31261-6de2-4fa5-a61d-078fefb33a53\",\"intJobId\":1332,\"serviceName\":\"SNAP\",\"jobOwner\":\"YrjoRauste\",\"jobStartDate\":\"2017-10-09T08:42:16.461Z\",\"jobEndDate\":\"2017-10-09T09:42:39.77Z\",\"filename\":\"B2.img\",\"ftepUrl\":\"ftep://outputProduct/b5e31261-6de2-4fa5-a61d-078fefb33a53/B2.img\",\"ftepparam\":\"\\\"{}\\\"\",\"ftepFileType\":\"OUTPUT_PRODUCT\",\"services\":{\"download\":{\"url\":\"http://ftep-resto/resto/collections/ftepOutputs/5aafaeb6-5b0b-50c8-b04c-2312484fac9a/download\",\"mimeType\":\"application/unknown\",\"size\":24112080000,\"checksum\":\"sha256=9ed1c625ca2ef94dc9d9e96b8077cf182b3ef2e203f4fa5dca72d82afd6e3575\"}},\"links\":[{\"rel\":\"self\",\"type\":\"application/json\",\"title\":\"GeoJSON link for 5aafaeb6-5b0b-50c8-b04c-2312484fac9a\",\"href\":\"http://ftep-resto/resto/collections/ftepOutputs/5aafaeb6-5b0b-50c8-b04c-2312484fac9a.json?&lang=en\"}]}}";

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        SearchConfig springContext = new SearchConfig();
        objectMapper = springContext.objectMapper();

    }

    @Test
    public void getRestoFilesize() throws Exception {
        Feature feature = objectMapper.readValue(FEATURE_JSON, Feature.class);
        assertThat(RestoSearchProvider.getRestoFilesize(feature), is(145L));
        Feature feature2 = objectMapper.readValue(FEATURE_JSON_2, Feature.class);
        assertThat(RestoSearchProvider.getRestoFilesize(feature2), is(24112080000L));
    }

}
