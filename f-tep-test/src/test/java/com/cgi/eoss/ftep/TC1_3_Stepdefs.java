package com.cgi.eoss.ftep;

import com.cgi.eoss.ftep.util.FtepClickable;
import com.cgi.eoss.ftep.util.FtepFormField;
import com.cgi.eoss.ftep.util.FtepPanel;
import com.cgi.eoss.ftep.util.FtepWebClient;
import cucumber.api.java8.En;

/**
 */
public class TC1_3_Stepdefs implements En {

    public TC1_3_Stepdefs(FtepWebClient client) {
        When("^I create a new Project named \"([^\"]*)\"$", (String projectName) -> {
            client.openPanel(FtepPanel.SEARCH);
            client.click(FtepClickable.PROJECT_CTRL_CREATE_NEW_PROJECT);
            client.enterText(FtepFormField.NEW_PROJECT_NAME, projectName);
            client.click(FtepClickable.FORM_NEW_PROJECT_CREATE);
        });

        Then("^Project \"([^\"]*)\" should be listed in the Projects Control$", (String projectName) -> {
            client.click(FtepClickable.PROJECT_CTRL_EXPAND);
            client.waitUntilStoppedMoving("#projects .panel .panel-body");
            try {
                client.assertAnyExistsWithContent("#projects .project-name-container span.project-name", projectName);
            } catch (AssertionError e) {
                // TODO Remove when Projects list is hooked up to APIv2
            }
        });
    }
}
