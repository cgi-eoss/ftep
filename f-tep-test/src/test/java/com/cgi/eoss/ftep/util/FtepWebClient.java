package com.cgi.eoss.ftep.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FtepWebClient {

    private final FtepHarness harness;
    private final OkHttpClient httpClient;

    public FtepWebClient(FtepHarness ftepHarness) {
        harness = ftepHarness;
        httpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.MILLISECONDS).build();
    }

    public Response get(String path) {
        try {
            Request request = new Request.Builder().url(getUrl(path)).build();
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(String path) {
        getWebDriver().get(getUrl(path));
    }

    public void load(FtepPage page) {
        load(page.getUrl());
    }

    public void loginAs(String username, String role) {
        String previousUrl = getWebDriver().getCurrentUrl();
        // Makes use of the DEVELOPMENT_BECOME_ANY_USER security mode
        load("/secure/api/v2.0/dev/user/become/" + username + "," + role);
        // Then return to the previous location
        getWebDriver().get(previousUrl);
    }

    public void openPanel(FtepPanel panel) {
        getWebDriver().findElement(By.cssSelector(panel.getSelector())).click();
        // Wait for the panel to fully fly out
        waitUntilStoppedMoving("#sidebar-left");
    }

    public void waitUntilStoppedMoving(String cssSelector) {
        WebElement sidebar = getWebDriver().findElement(By.cssSelector(cssSelector));
        driverWait(5).until(elementHasStoppedMoving(sidebar));
    }

    public void click(FtepClickable button) {
        WebElement el = driverWait(5).until(ExpectedConditions.elementToBeClickable(By.cssSelector(button.getSelector())));
        Actions actions = new Actions(getWebDriver());
        actions.moveToElement(el).click().perform();
    }

    public void enterText(FtepFormField formField, String input) {
        By selector = By.cssSelector(formField.getSelector());
        getWebDriver().findElement(selector).sendKeys(input);
        driverWait(5).until(ExpectedConditions.textToBePresentInElementValue(selector, input));
    }

    public void assertAnyExistsWithContent(String cssSelector, String elContent) {
        assertThat(getWebDriver().findElements(By.cssSelector(cssSelector)), hasItem(elText(is(elContent))));
    }

    public String getContent(Response response) {
        try (ResponseBody body = response.body()) {
            return body.string();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WebDriver getWebDriver() {
        return harness.getChromeWebDriver();
    }

    private String getUrl(String path) {
        return harness.getFtepBaseUrl() + path;
    }

    private WebDriverWait driverWait(long timeOutInSeconds) {
        return new WebDriverWait(getWebDriver(), timeOutInSeconds);
    }

    private static ExpectedCondition<Boolean> elementHasStoppedMoving(final WebElement element) {
        return (WebDriver driver) -> {
            Point initialLocation = ((Locatable) element).getCoordinates().inViewPort();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Point finalLocation = ((Locatable) element).getCoordinates().inViewPort();
            return initialLocation.equals(finalLocation);
        };
    }

    private FeatureMatcher<WebElement, String> elText(Matcher<String> matcher) {
        return new FeatureMatcher<WebElement, String>(matcher, "elText", "elText") {
            @Override
            protected String featureValueOf(WebElement actual) {
                return actual.getText();
            }
        };
    }

}
