Feature: Song metadata management
  As a client of song-service
  I want to create, retrieve and delete song metadata records
  So that other services can look up track information by resource ID

  Background:
    Given the song catalog is empty

  Scenario: Creating metadata for a new resource
    When I create song metadata for resource 1 with:
      | name     | Bohemian Rhapsody    |
      | artist   | Queen                |
      | album    | A Night at the Opera |
      | duration | 05:55                |
      | year     | 1975                 |
    Then the response status is 200
    And the response contains the created record id 1
    And song metadata for resource 1 can be retrieved with:
      | name     | Bohemian Rhapsody    |
      | artist   | Queen                |
      | album    | A Night at the Opera |
      | duration | 05:55                |
      | year     | 1975                 |

  Scenario: Creating metadata for a resource that already has one is rejected
    Given song metadata already exists for resource 2
    When I create song metadata for resource 2 with:
      | name     | Duplicate |
      | artist   | Someone   |
      | album    | Album     |
      | duration | 03:00     |
      | year     | 2000      |
    Then the response status is 409

  Scenario: Creating metadata with an invalid duration is rejected
    When I create song metadata for resource 3 with:
      | name     | Track   |
      | artist   | Artist  |
      | album    | Album   |
      | duration | invalid |
      | year     | 2000    |
    Then the response status is 400
    And the validation error mentions field "duration"

  Scenario: Retrieving metadata for an unknown resource returns not found
    When I look up song metadata for resource 999
    Then the response status is 404

  Scenario: Retrieving metadata with a malformed id is rejected
    When I look up song metadata for resource "abc"
    Then the response status is 400

  Scenario: Deleting existing metadata removes it
    Given song metadata already exists for resource 4
    And song metadata already exists for resource 5
    When I delete song metadata for resources "4,5"
    Then the response status is 200
    And the response lists deleted ids "4,5"
    And song metadata for resource 4 no longer exists
    And song metadata for resource 5 no longer exists

  Scenario: Deleting a mix of existing and missing ids only reports the existing ones
    Given song metadata already exists for resource 6
    When I delete song metadata for resources "6,7"
    Then the response status is 200
    And the response lists deleted ids "6"
