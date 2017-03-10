package com.cgi.eoss.ftep;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TWO_MINUTES;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

class Harness {
    private static final Logger LOG = LogManager.getLogger("HelloWorld");

    final DockerComposeContainer environment;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MILLISECONDS)
            .build();

    Harness() {
        this.environment = new DockerComposeContainer(Paths.get(System.getProperty("HARNESS_DIR"), "docker-compose.yml").toFile())
                .withExposedService("ftep-proxy_1", 80);
        propagateEnvironment(environment, "http_proxy", "https_proxy", "no_proxy");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.environment.finished(null)));
    }

    void startFtepEnvironment() {
        this.environment.starting(null);

        LOG.info("*** F-TEP Services exposed on: {} ***", getFtepBaseUrl());

        // Puppet can take some time to process and start the services, so just wait until we get any response on wps and webapp
        with().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .and().atMost(TWO_MINUTES)
                .await("Test environment started")
                .until(() -> {
                    try {
                        Request webappRoot = new Request.Builder().url(getWebappUrl("/")).build();
                        try (Response response = httpClient.newCall(webappRoot).execute()) {
                            assertThat(response, is(notNullValue()));
                        }
                        Request wpsServices = new Request.Builder().url(getWpsUrl("/zoo_loader.cgi?request=GetCapabilities&service=WPS")).build();
                        try (Response response = httpClient.newCall(wpsServices).execute()) {
                            assertThat(response.code(), is(200));
                        }
                    } catch (Exception e) {
                        fail();
                    }
                });
    }

    String getResponseContent(Response response) {
        try (ResponseBody body = response.body()) {
            return body.string();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    Response getWpsResponse(String path) {
        try {
            Request request = new Request.Builder().url(getWpsUrl(path)).build();
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getWebappUrl(String path) {
        return getFtepBaseUrl() + "/app" + path;
    }

    private String getWpsUrl(String path) {
        return getFtepBaseUrl() + "/secure/wps" + path;
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
