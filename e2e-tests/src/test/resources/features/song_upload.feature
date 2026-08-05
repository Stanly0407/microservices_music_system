Feature: Song upload — full stack
  As a user of the music catalog
  I want to upload an MP3 through the public gateway
  So that it is stored, processed asynchronously, and searchable as a song

  This is the one critical business flow of the system:
  gateway -> resource-service -> RabbitMQ -> resource-processor -> song-service.
  It runs against the real docker-compose stack, no stubs.

  Background:
    Given the api-gateway is reachable

  Scenario: Uploading a valid MP3 makes its metadata available in the catalog
    When I upload the fixture MP3 "valid-track.mp3" through the gateway
    Then the upload is accepted with a resource id
    And within 15 seconds the song catalog has metadata for that resource with:
      | name   | E2E Test Track  |
      | artist | E2E Test Artist |
      | album  | E2E Test Album  |
      | year   | 2001            |

  Scenario: Deleting the uploaded resource also removes its song metadata
    Given I have uploaded the fixture MP3 "valid-track.mp3" and its metadata is in the catalog
    When I delete that resource through the gateway
    Then the song catalog no longer has metadata for that resource
