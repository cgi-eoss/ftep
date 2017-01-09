Feature: F-TEP :: WPS

  Scenario: GetCapabilities
    Given the F-TEP environment
    When a user requests GetCapabilities from WPS
    Then they receive the F-TEP service list
