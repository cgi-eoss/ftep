package com.cgi.eoss.ftep;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import cucumber.api.java8.En;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.shaded.com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TWO_MINUTES;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 */
public class WpsStepdefs implements En {

    private DockerComposeContainer environment;

    private BrowserWebDriverContainer browser;

    private final HttpRequestFactory httpRequestFactory = new NetHttpTransport().createRequestFactory();

    private String responseBody;

    public WpsStepdefs() {
        Given("^the F-TEP backend$", () -> {
            environment = new DockerComposeContainer(Paths.get(System.getProperty("HARNESS_DIR"), "docker-compose.yml").toFile())
                    .withExposedService("backend_1", 80);
            propagateEnvironment(environment, "http_proxy", "https_proxy", "no_proxy");
            environment.starting(null);

            // Puppet can take some time to process and start the services, so just wait until we get any response on backend:80
            with().pollInterval(ONE_HUNDRED_MILLISECONDS)
                    .and().atMost(TWO_MINUTES)
                    .await("Test environment starting")
                    .until(() -> {
                        try {
                            HttpResponse response = httpRequestFactory.buildGetRequest(getBackendUrl("/")).setConnectTimeout(10).execute();
                            assertThat(response, is(notNullValue()));
                        } catch (Exception e) {
                            fail();
                        }
                    });

            browser = new BrowserWebDriverContainer().withDesiredCapabilities(DesiredCapabilities.chrome());
        });

        When("^a user requests GetCapabilities from WPS$", () -> {
            HttpResponse response = getBackendHttpResponse("/cgi-bin/zoo_loader.cgi?request=GetCapabilities&service=WPS");
            assertThat(response.getStatusCode(), is(200));
            responseBody = getResponseContent(response);
        });

        Then("^they receive the F-TEP service list$", () -> {
            assertThat(responseBody, allOf(
                    containsString("<ows:Title>Forestry TEP (F-TEP)  WPS Server</ows:Title>"),
                    containsString("<ows:Identifier>QGIS</ows:Identifier>")
            ));
        });
    }

    private void propagateEnvironment(DockerComposeContainer container, String... environmentVariables) {
        for (String var : environmentVariables) {
            if (System.getenv().containsKey(var)) {
                container.withEnv(var, System.getenv(var));
            }
        }
    }

    private String getResponseContent(HttpResponse response) {
        try (InputStream is = response.getContent()) {
            return new String(ByteStreams.toByteArray(is));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse getBackendHttpResponse(String path) {
        try {
            return httpRequestFactory.buildGetRequest(getBackendUrl(path)).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GenericUrl getBackendUrl(String path) {
        return new GenericUrl("http://" +
                environment.getServiceHost("backend_1", 80) + ":" +
                environment.getServicePort("backend_1", 80) +
                path);
    }
}
