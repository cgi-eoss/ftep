Feature: F-TEP :: WPS

  Scenario: GetCapabilities
    Given the F-TEP backend
    When a user requests GetCapabilities from WPS
    Then they receive the F-TEP service list
