package com.cgi.eoss.ftep.catalogue.resto;

import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * <p>Implementation of RestoService using Resto's HTTP REST-style API.</p>
 */
@Component
@Log4j2
public class RestoServiceImpl implements RestoService {

    private final OkHttpClient client;

    @Value("${ftep.catalogue.resto.url:http://ftep-resto/}")
    private String restoBaseUrl;

    @Value("${ftep.catalogue.resto.collection.refData:ftepRefData}")
    private String refDataCollection;

    @Value("${ftep.catalogue.resto.collection.outputProducts:ftepOutputProducts}")
    private String outputProductCollection;

    @Autowired
    public RestoServiceImpl(@Value("${ftep.catalogue.resto.username:ftepresto}") String username,
                            @Value("${ftep.catalogue.resto.password:ftepresto}") String password) {
        this.client = new OkHttpClient.Builder()
                .authenticator((route, response) -> {
                    String credential = Credentials.basic(username, password);
                    return response.request().newBuilder()
                            .header("Authorization", credential)
                            .build();
                })
                .build();
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

    private UUID ingest(String collection, GeoJsonObject object) {
        URI uri = URI.create(restoBaseUrl).resolve("collections").resolve(collection);
        LOG.debug("Creating new Resto catalogue entry in collection: {}", collection);

        Request request = new Request.Builder()
                .url(uri.toString())
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON_VALUE), GeoUtil.geojsonToString(object)))
                .build();

        try {
            Response response = client.newCall(request).execute();
            try (ResponseBody body = response.body()) {
                String uuid = JsonPath.read(body.string(), "featureIdentifier");
                LOG.debug("Created new Resto object with ID: {}", uuid);
                return UUID.fromString(uuid);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void delete(String collection, UUID uuid) {
        URI uri = URI.create(restoBaseUrl).resolve("collections").resolve(collection);
        LOG.debug("Deleting Resto catalogue entry {} from collection: {}", uuid, collection);

        Request request = new Request.Builder().url(uri.toString()).delete().build();
        Response response = null;

        try {
            response = client.newCall(request).execute();
            LOG.info("Deleted Resto object with ID {}", uuid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (response != null) {
                LOG.debug("Received HTTP {} on deleting: {}", response.code(), uri);
            }
        }
    }

}
