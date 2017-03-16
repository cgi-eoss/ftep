package com.cgi.eoss.ftep;

import com.cgi.eoss.ftep.util.FtepPage;
import com.cgi.eoss.ftep.util.FtepWebClient;
import cucumber.api.java8.En;

public class CommonStepdefs implements En {

    public CommonStepdefs(FtepWebClient client) {
        Given("^I am on the \"([^\"]*)\" page$", (String page) -> {
            client.load(FtepPage.valueOf(page));
        });

        Given("^I am logged in as \"([^\"]*)\" with role \"([^\"]*)\"$", (String username, String role) -> {
            client.loginAs(username, role);
        });
    }

}
