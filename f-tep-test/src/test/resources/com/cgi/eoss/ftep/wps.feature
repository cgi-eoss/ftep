Feature: F-TEP :: WPS

  Scenario: GetCapabilities
    Given F-TEP WPS is available
    When a user requests GetCapabilities from WPS
    Then they receive the F-TEP service list
