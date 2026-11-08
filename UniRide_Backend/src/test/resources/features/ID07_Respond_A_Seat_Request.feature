Feature: Respond to a seat request (ID07)
  As a driver on UniRide
  I want to accept or reject incoming seat requests for my ride
  So that I can manage who joins my ride and update available seats

  Background:
    Given a user "John" exists and is logged in
    And a user "Jef" exists and is logged in
    And "John" has created a ride from "Montreal" to "Toronto" with 3 available seats
    And "Jef" has sent a seat request for "John"'s ride with status "PENDING"

  Scenario: Driver John accepts a pending seat request
    When "John" accepts the seat request from "Jef"
    Then the status of the seat request should be updated to "ACCEPTED"
    And the available seats for "John"'s ride should be 2

  Scenario: Driver John rejects a pending seat request
    When "John" rejects the seat request from "Jef"
    Then the status of the seat request should be updated to "REJECTED"
    And the available seats for "John"'s ride should remain 3

  Scenario: User Bob attempts to accept John's seat request
    Given a user "Bob" exists and is logged in
    When "Bob" attempts to accept the seat request from "Jef" for "John"'s ride
    Then the system should deny the action with an unauthorized error message
    And the status of the seat request should remain "PENDING" 