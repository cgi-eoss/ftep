package com.cgi.eoss.ftep;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
       plugin = {"json:target/test-results/cucumber.json"}
)
public class CucumberIT {
}