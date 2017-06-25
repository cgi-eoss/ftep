package com.cgi.eoss.ftep.catalogue.resto;

import com.cgi.eoss.ftep.catalogue.IngestionException;
import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.model.FtepFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * <p>Implementation of RestoService using Resto's HTTP REST-style API.</p>
 */
@Component
@Log4j2
public class RestoServiceImpl implements RestoService {

    private final OkHttpClient client;

    private final ObjectMapper jsonMapper;

    private final String restoBaseUrl;

    @Value("${ftep.catalogue.resto.enabled:true}")
    private boolean restoEnabled;

    @Value("${ftep.catalogue.resto.collection.externalProducts:ftepInputs}")
    private String externalProductCollection;

    @Value("${ftep.catalogue.resto.collection.externalProductsModel:RestoModel_Ftep_Input}")
    private String externalProductModel;

    @Value("${ftep.catalogue.resto.collection.refData:ftepRefData}")
    private String refDataCollection;

    @Value("${ftep.catalogue.resto.collection.refDataModel:RestoModel_Ftep_Reference}")
    private String refDataModel;

    @Value("${ftep.catalogue.resto.collection.outputProducts:ftepOutputs}")
    private String outputProductCollection;

    @Value("${ftep.catalogue.resto.collection.outputProductsModel:RestoModel_Ftep_Output}")
    private String outputProductModel;

    @Autowired
    public RestoServiceImpl(@Value("${ftep.catalogue.resto.url:http://ftep-resto/resto/}") String restoBaseUrl,
                            @Value("${ftep.catalogue.resto.username:ftepresto}") String username,
                            @Value("${ftep.catalogue.resto.password:fteprestopass}") String password) {
        this.restoBaseUrl = restoBaseUrl;
        this.client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Request authenticatedRequest = request.newBuilder()
                                .header("Authorization", Credentials.basic(username, password))
                                .build();
                        return chain.proceed(authenticatedRequest);
                    }
                })
                .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        jsonMapper = new ObjectMapper();
    }

    @Override
    public UUID ingestReferenceData(GeoJsonObject object) {
        return ingest(refDataCollection, object);
    }

    @Override
    public UUID ingestOutputProduct(GeoJsonObject object) {
        return ingest(outputProductCollection, object);
    }

    @Override
    public UUID ingestExternalProduct(String collection, GeoJsonObject object) {
        return ingest(collection, object);
    }

    @Override
    public void deleteReferenceData(UUID restoId) {
        delete(refDataCollection, restoId);
    }

    @Override
    public GeoJsonObject getGeoJson(FtepFile ftepFile) {
        HttpUrl url = HttpUrl.parse(restoBaseUrl).newBuilder()
                .addPathSegment("api")
                .addPathSegment("collections")
                .addPathSegment("search.json")
                .addQueryParameter("identifier", ftepFile.getRestoId().toString())
                .build();

        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            Configuration configuration = Configuration.defaultConfiguration().mappingProvider(new JacksonMappingProvider());
            return JsonPath.using(configuration).parse(response.body().string()).read("$.features[0]", Feature.class);
        } catch (Exception e) {
            LOG.error("Failed to query Resto for GeoJson for identifier {}", ftepFile.getRestoId(), e);
            throw new RestoException(e);
        }
    }

    @Override
    public GeoJsonObject getGeoJsonSafe(FtepFile ftepFile) {
        try {
            return getGeoJson(ftepFile);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getReferenceDataCollection() {
        return refDataCollection;
    }

    @Override
    public String getOutputProductsCollection() {
        return outputProductCollection;
    }

    private UUID ingest(String collection, GeoJsonObject object) {
        if (!restoEnabled) {
            UUID dummy = UUID.randomUUID();
            LOG.warn("Resto is disabled; 'ingested' dummy feature: {}", dummy);
            return dummy;
        }

        HttpUrl url = HttpUrl.parse(restoBaseUrl).newBuilder().addPathSegment("collections").addPathSegment(collection).build();
        LOG.debug("Creating new Resto catalogue entry in collection: {}", collection);

        try {
            ensureCollectionExists(collection);
        } catch (Exception e) {
            throw new IngestionException("Failed to get/create Resto collection", e);
        }

        String geojson = GeoUtil.geojsonToString(object);
        LOG.debug("Ingesting GeoJSON to {}: {}", geojson);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON_VALUE), geojson))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String uuid = JsonPath.read(response.body().string(), "$.featureIdentifier");
                LOG.info("Created new Resto object with ID: {}", uuid);
                return UUID.fromString(uuid);
            } else {
                LOG.error("Failed to ingest Resto object to collection '{}': {} {}: {}", collection, response.code(), response.message(), response.body());
                throw new IngestionException("Failed to ingest Resto object");
            }
        } catch (Exception e) {
            throw new IngestionException(e);
        }
    }

    private void delete(String collection, UUID uuid) {
        if (!restoEnabled) {
            LOG.warn("Resto is disabled; no deletion occurring for {}", uuid);
            return;
        }

        HttpUrl url = HttpUrl.parse(restoBaseUrl).newBuilder().addPathSegment("collections").addPathSegment(collection).build();
        LOG.debug("Deleting Resto catalogue entry {} from collection: {}", uuid, collection);

        Request request = new Request.Builder().url(url).delete().build();

        try (Response response = client.newCall(request).execute()) {
            LOG.info("Deleted Resto object with ID {}", uuid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getRestoCollections() {
        HttpUrl allCollectionsUrl = HttpUrl.parse(restoBaseUrl).newBuilder().addPathSegment("collections.json").build();
        Request request = new Request.Builder().url(allCollectionsUrl).build();

        try (Response response = client.newCall(request).execute()) {
            return JsonPath.read(response.body().string(), "$.collections[*].name");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureCollectionExists(String collectionName) {
        if (!getRestoCollections().contains(collectionName)) {
            LOG.debug("Collection '{}' not found, creating", collectionName);
            try (Response response = client
                    .newCall(new Request.Builder()
                            .url(HttpUrl.parse(restoBaseUrl).newBuilder().addPathSegment("collections").build())
                            .post(RequestBody.create(MediaType.parse(APPLICATION_JSON_VALUE), jsonMapper.writeValueAsString(buildCollectionConfig(collectionName))))
                            .build())
                    .execute()) {
                if (response.isSuccessful()) {
                    LOG.info("Created Resto collection '{}'", collectionName);
                } else {
                    LOG.warn("Failed to create Resto collection '{}': {} {}: {}", collectionName, response.code(), response.message(), response.body());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private RestoCollection buildCollectionConfig(String collectionName) {
        RestoCollection.RestoCollectionBuilder builder = RestoCollection.builder()
                .name(collectionName)
                .status("public")
                .licenseId("unlicensed")
                .rights(ImmutableMap.of("download", 0, "visualize", 1));

        // TODO Add WMS mapping properties where possible

        if (collectionName.equals(outputProductCollection)) {
            builder
                    .model(outputProductModel)
                    .osDescription(ImmutableMap.of("en", RestoCollection.OpensearchDescription.builder()
                            .shortName("ftepOutputs")
                            .longName("F-TEP Output Products")
                            .description("Products created by F-TEP services")
                            .tags("ftep f-tep output outputs generated")
                            .query("ftepOutputs")
                            .build()))
                    .propertiesMapping(ImmutableMap.of(
                            "ftepFileType", FtepFile.Type.OUTPUT_PRODUCT.toString())
                    );
        } else if (collectionName.equals(refDataCollection)) {
            builder
                    .model(refDataModel)
                    .osDescription(ImmutableMap.of("en", RestoCollection.OpensearchDescription.builder()
                            .shortName("ftepRefData")
                            .longName("F-TEP Reference Data Products")
                            .description("Reference data uploaded by F-TEP users")
                            .tags("ftep f-tep refData reference")
                            .query("ftepRefData")
                            .build()))
                    .propertiesMapping(ImmutableMap.of(
                            "ftepFileType", FtepFile.Type.REFERENCE_DATA.toString())
                    );
        } else {
            builder
                    .model(externalProductModel)
                    .osDescription(ImmutableMap.of("en", RestoCollection.OpensearchDescription.builder()
                            .shortName(collectionName)
                            .longName("F-TEP External Products: " + collectionName)
                            .description("External products used as inputs by F-TEP services (" + collectionName + ")")
                            .tags("ftep f-tep inputs input external " + collectionName)
                            .query("ftepInputs_" + collectionName)
                            .build()))
                    .propertiesMapping(ImmutableMap.of(
                            "ftepFileType", FtepFile.Type.EXTERNAL_PRODUCT.toString())
                    );
        }

        return builder.build();
    }

}
