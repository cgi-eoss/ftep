package com.cgi.eoss.ftep;

import cucumber.api.java8.En;
import okhttp3.Response;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.containers.BrowserWebDriverContainer;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class WpsStepdefs implements En {

    private Harness harness = new Harness();

    private BrowserWebDriverContainer browser;

    private String responseBody;

    public WpsStepdefs() {
        Given("^the F-TEP environment$", () -> {
            harness.startFtepEnvironment();
            browser = new BrowserWebDriverContainer().withDesiredCapabilities(DesiredCapabilities.chrome());
        });

        When("^a user requests GetCapabilities from WPS$", () -> {
            Response response = harness.getWpsResponse("/zoo_loader.cgi?request=GetCapabilities&service=WPS");
            assertThat(response.code(), is(200));
            responseBody = harness.getResponseContent(response);
        });

        Then("^they receive the F-TEP service list$", () -> {
            assertThat(responseBody, allOf(
                    containsString("<ows:Title>Forestry TEP (F-TEP)  WPS Server</ows:Title>"),
                    containsString("<ows:Identifier>QGIS</ows:Identifier>")
            ));
        });
    }
}
