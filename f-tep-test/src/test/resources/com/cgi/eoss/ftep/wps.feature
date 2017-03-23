Feature: F-TEP :: WPS

  Scenario: GetCapabilities
    Given F-TEP WPS is available
    And the default services are loaded
    When a user requests GetCapabilities from WPS
    Then they receive the F-TEP service list
