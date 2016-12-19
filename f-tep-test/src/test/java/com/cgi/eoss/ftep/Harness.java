package com.cgi.eoss.ftep;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.shaded.com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TWO_MINUTES;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

class Harness {

    final DockerComposeContainer environment;

    private final HttpRequestFactory httpRequestFactory = new NetHttpTransport().createRequestFactory();

    Harness() {
        this.environment = new DockerComposeContainer(Paths.get(System.getProperty("HARNESS_DIR"), "docker-compose.yml").toFile())
                .withExposedService("ftep-proxy_1", 80);
        propagateEnvironment(environment, "http_proxy", "https_proxy", "no_proxy");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.environment.finished(null)));
    }

    void startFtepEnvironment() {
        this.environment.starting(null);

        // Puppet can take some time to process and start the services, so just wait until we get any response on wps and webapp
        with().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .and().atMost(TWO_MINUTES)
                .await("Test environment started")
                .until(() -> {
                    try {
                        HttpResponse response = httpRequestFactory.buildGetRequest(getWebappUrl("/")).setConnectTimeout(10).execute();
                        assertThat(response, is(notNullValue()));
                        response = httpRequestFactory.buildGetRequest(getWpsUrl("/cgi-bin/zoo_loader.cgi?request=GetCapabilities&service=WPS")).setConnectTimeout(10).execute();
                        assertThat(response, is(notNullValue()));
                    } catch (Exception e) {
                        fail();
                    }
                });
    }

    String getResponseContent(HttpResponse response) {
        try (InputStream is = response.getContent()) {
            return new String(ByteStreams.toByteArray(is));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    HttpResponse getWpsResponse(String path) {
        try {
            return httpRequestFactory.buildGetRequest(getWpsUrl(path)).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GenericUrl getWebappUrl(String path) {
        return new GenericUrl(getFtepBaseUrl() + "/app" + path);
    }

    private GenericUrl getWpsUrl(String path) {
        return new GenericUrl(getFtepBaseUrl() + "/wps" + path);
    }

    private String getFtepBaseUrl() {
        return "http://" +
                environment.getServiceHost("ftep-proxy_1", 80) + ":" +
                environment.getServicePort("ftep-proxy_1", 80);
    }

    private static void propagateEnvironment(DockerComposeContainer container, String... environmentVariables) {
        for (String var : environmentVariables) {
            if (System.getenv().containsKey(var)) {
                container.withEnv(var, System.getenv(var));
            }
        }
    }
}
