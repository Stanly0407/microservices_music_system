Feature: API Gateway routing and error handling
  As a client of the music catalog system
  I want every request to go through the single api-gateway entry point
  So that routing, service discovery, and error handling are centralized

  This runs against the real docker-compose stack (or a locally-running gateway, via the
  e2e.gatewayUrl system property), no stubs.

  Background:
    Given the api-gateway is reachable

  Scenario: A request for a defined route is forwarded to the correct downstream service
    When I request "/songs/999999999" through the gateway
    Then the gateway responds with status 404
    And the response body has errorCode "404"

  Scenario: A request for an undefined route gets a friendly error, not a raw failure
    When I request "/no-such-route" through the gateway
    Then the gateway responds with status 404
    And the response body has errorMessage "The requested resource was not found"
    And the response body has errorCode "404"

  Scenario: song-service's replicas are all registered for load-balanced routing
    Then eventually "SONG-SERVICE" has 2 instances registered as UP in Eureka
