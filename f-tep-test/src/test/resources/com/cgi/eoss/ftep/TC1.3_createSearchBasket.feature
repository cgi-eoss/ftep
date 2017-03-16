# DOUBLE-CHECKED! 20161212Mo

# resources/ TC01.03_createSearchBasket.feature

# 2.5.1.3 Test Case 1.3 Create Project, Search Data, Create DataBasket
Feature: Create Search Basket
    As an enduser
    I want to be able to logon to the F-TEP portal
    So that to search and save the data

# step 1
Scenario: [TC01.03s01] Create Project
    Given I am on the "EXPLORER" page
    And I am logged in as "testuser" with role "USER"
    When I create a new Project named "TC1.3 Project"
    Then Project "TC1.3 Project" should be listed in the Projects Control

## step 2
#Scenario: [TC01.03s02] Search EO Data
#    Given I am logged in as "testuser" with role "USER"
#    And I am on the "EXPLORER" page
#    And the new Project
#    When I enter search criteria for EO Data search
#    Then the data should be visible on the Result Panel
#
## step 3
#Scenario: [TC01.03s03] Create DataBasket
#    Given the Search Results
#    And randomly selected result data
#    When I create a Databasket
#    Then the new DataBasket should be visible on the DataBasket Panel
#
## step 4
#Scenario: [TC01.03s04] Switch Project
#    Given the new DataBasket
#    When I change Project
#    Then the new DataBasket should not be visible on the DataBasket Panel
#
## step 5
##Scenario: [TC01.03s05] Logout
##    Given the new Project
##    Given the new DataBasket
##    When I logout of the Portal
##    Then the Project should not be visible on the Project Panel
#
## step 6
##Scenario: [TC01.03s06] Login
##    Given that I am not logged in
##    Given I can initiate SSO login
##    When I select the new Project
##    Then the Project should be visible on the Project Panel
##    Then the new DataBasket should be visible on the DataBasket Panel
#
## step 7
#Scenario: [TC01.03s07] Rename Project
#    Given the new Project
#    When I rename the Project
#    Then the Project should be renamed successfully
#
## step 8
#Scenario: [TC01.03s08] Delete Project
#    Given the new Project
#    Given the new DataBasket
#    When I delete the Project
#    Then the Project and contents should be deleted successfully
