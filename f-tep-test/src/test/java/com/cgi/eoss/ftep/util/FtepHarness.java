package com.cgi.eoss.ftep.util;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.Description;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.dockerclient.LogToStringContainerCallback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TEN_SECONDS;
import static org.awaitility.Duration.TWO_MINUTES;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Log4j2
public class FtepHarness {

    private Description junitDescription;

    private final DockerComposeContainer environment;

    private final ChromeBrowserContainer chrome;

    private final OkHttpClient httpClient;

    private String hostIp;

    public FtepHarness() {
        environment = new DockerComposeContainer(Paths.get(System.getProperty("HARNESS_DIR"), "docker-compose.yml").toFile())
                .withExposedService("ftep-portal_1", 80);
        chrome = new ChromeBrowserContainer();

        ImmutableSet.of("http_proxy", "https_proxy", "no_proxy").stream()
                .filter(var -> System.getenv().containsKey(var))
                .forEach(var -> {
                    environment.withEnv(var, System.getenv(var));
                    chrome.withEnv(var, System.getenv(var));
                });

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.MILLISECONDS)
                .build();
    }

    @Before
    public void preScenario(Scenario scenario) {
        junitDescription = Description.createSuiteDescription(scenario.getName());

        environment.starting(junitDescription);
        chrome.starting(junitDescription);

        with().pollInterval(FIVE_HUNDRED_MILLISECONDS)
                .and().atMost(TEN_SECONDS)
                .await("Test environment started")
                .until(() -> {
                    hostIp = getProxyHost();
                    assertThat(Strings.isNullOrEmpty(hostIp), is(false));
                });
        LOG.info("*** F-TEP Services exposed on: {} ***", getFtepBaseUrl());

        // Some services can take time to start up, so just wait until we get a suitable response on components
        with().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .and().atMost(TWO_MINUTES)
                .await("Test environment started")
                .until(this::containersAreUp);

        // Navigate to the webapp entry point
        getChromeWebDriver().get(getFtepBaseUrl() + "/app/");

        // Just in case, register a general 10s timeout for browser requests
        chrome.getWebDriver().manage().timeouts().pageLoadTimeout(10, SECONDS);
    }

    @After
    public void postScenario(Scenario scenario) {
        environment.finished(junitDescription);
        chrome.finished(junitDescription);
    }

    WebDriver getChromeWebDriver() {
        return chrome.getWebDriver();
    }

    String getFtepBaseUrl() {
        return "http://" +
                hostIp + ":" +
                environment.getServicePort("ftep-portal_1", 80);
    }

    @NotNull
    private static String getProxyHost() {
        // Use the IP detection from org.testcontainers.dockerclient.DockerClientConfigUtils#detectedDockerHostIp
        return StringUtils.trimToEmpty(DockerClientFactory.instance().runInsideDocker(
                cmd -> cmd.withCmd("sh", "-c", "ip route|awk '/default/ { print $3 }'"),
                (client, id) -> {
                    try {
                        return client.logContainerCmd(id)
                                .withStdOut(true)
                                .exec(new LogToStringContainerCallback())
                                .toString();
                    } catch (Exception e) {
                        LOG.warn("Can't parse the default gateway IP", e);
                        return null;
                    }
                }));
    }

    private void containersAreUp() {
        ImmutableMap.of(
                "/app/", 200,
                "/secure/wps/zoo_loader.cgi", 400,
                "/secure/api/v2.0/dev/user/become/test-admin,ADMIN", 200
        ).forEach((path, expectedCode) -> {
            Request webRoot = new Request.Builder().url(getFtepBaseUrl() + path).build();
            try (Response response = httpClient.newCall(webRoot).execute()) {
                assertThat(response.code(), is(expectedCode));
            } catch (Exception e) {
                fail();
            }
        });
    }

    private static final class ChromeBrowserContainer extends BrowserWebDriverContainer {
        private static final int SELENIUM_PORT = 4444;
        private static final int VNC_PORT = 5900;

        private DesiredCapabilities desiredCapabilities;

        ChromeBrowserContainer() {
            super();

            try {
                this.desiredCapabilities = DesiredCapabilities.chrome();

                Path recordingPath = Paths.get("target/recording").toAbsolutePath();
                Files.createDirectories(recordingPath);

                withRecordingMode(VncRecordingMode.RECORD_ALL, recordingPath.toFile());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void configure() {
            checkState(desiredCapabilities != null);
            super.setDockerImageName("selenium/standalone-chrome-debug:3.4.0");

            String timeZone = System.getProperty("user.timezone");

            if (timeZone == null || timeZone.isEmpty()) {
                timeZone = "Etc/UTC";
            }

            addExposedPorts(SELENIUM_PORT, VNC_PORT);
            addEnv("TZ", timeZone);
            setCommand("/opt/bin/entry_point.sh");

            final boolean[] setProxy = {false};
            Proxy proxy = new Proxy();
            Optional.ofNullable(System.getenv("http_proxy")).ifPresent(httpProxy -> {
                setProxy[0] = true;
                withEnv("http_proxy", httpProxy);
                proxy.setHttpProxy(httpProxy);
            });
            Optional.ofNullable(System.getenv("https_proxy")).ifPresent(sslProxy -> {
                setProxy[0] = true;
                withEnv("https_proxy", sslProxy);
                proxy.setSslProxy(sslProxy);
            });
            Optional.ofNullable(System.getenv("no_proxy")).ifPresent(noProxy -> {
                setProxy[0] = true;
                withEnv("no_proxy", getProxyHost() + "," + noProxy);
                proxy.setNoProxy(getProxyHost() + "," + noProxy);
            });

            if (setProxy[0]) {
                desiredCapabilities.setCapability(CapabilityType.PROXY, proxy);
            }

            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("excludeSwitches", ImmutableList.of("enable-automation"));
            options.addArguments("start-maximized");
            desiredCapabilities.setCapability(ChromeOptions.CAPABILITY, options);

            withDesiredCapabilities(desiredCapabilities);

            //Some unreliability of the selenium browser containers has been observed, so allow multiple attempts to start.
            setStartupAttempts(3);
        }

        protected void starting(Description description) {
            super.starting(description);
        }

        protected void finished(Description description) {
            super.succeeded(description);
            super.finished(description);
        }
    }

}
