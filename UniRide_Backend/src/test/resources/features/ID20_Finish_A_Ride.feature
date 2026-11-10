@ID20
Feature: Finish a Ride
  As a UniRide driver
  I want to mark my ride as finished
  So that the ride is recorded as completed and passengers can rate me afterwards

  # Proposed TID116 rule: only the driver who posted the ride can finish it.
  # Open and Full rides are eligible at or after scheduled arrival
  # (departure time plus the 60-minute duration used by these fixtures).

  Background:
    Given the current date and time is "2027-05-08 13:00"
    And the following drivers exist:
      | email                     | firstName | lastName | licenseNumber |
      | marc.lavoie@mail.mcgill.ca | Marc      | Lavoie   | QC123456      |
      | sara.nguyen@mail.mcgill.ca | Sara      | Nguyen   | QC654321      |
    And the following rides exist:
      | ride_id | driver                    | origin   | destination | departure_date_time | total_seats | price_per_seat | status    |
      |       1 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 11:00 |           4 |          15.00 | Full      |
      |       2 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 11:00 |           4 |          15.00 | Cancelled |
      |       3 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 12:00 |           4 |          15.00 | Open      |
      |       4 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 12:01 |           4 |          15.00 | Open      |
      |       5 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 11:00 |           4 |          15.00 | Completed |

  Scenario: The driver finishes their own ride
    When driver "marc.lavoie@mail.mcgill.ca" attempts to finish ride 1
    Then the finish-ride response status is 204
    And ride 1 has status "Completed"

  Scenario: The driver finishes an open ride at its scheduled arrival
    When driver "marc.lavoie@mail.mcgill.ca" attempts to finish ride 3
    Then the finish-ride response status is 204
    And ride 3 has status "Completed"

  Scenario: The driver cannot finish a ride before its scheduled arrival
    When driver "marc.lavoie@mail.mcgill.ca" attempts to finish ride 4
    Then the finish-ride response status is 409
    And ride 4 has status "Open"

  Scenario: Another driver cannot finish the ride
    When driver "sara.nguyen@mail.mcgill.ca" attempts to finish ride 1
    Then the finish-ride response status is 403
    And ride 1 has status "Full"

  Scenario: The driver cannot finish a cancelled ride
    When driver "marc.lavoie@mail.mcgill.ca" attempts to finish ride 2
    Then the finish-ride response status is 409
    And ride 2 has status "Cancelled"

  Scenario: The driver cannot finish an already completed ride
    When driver "marc.lavoie@mail.mcgill.ca" attempts to finish ride 5
    Then the finish-ride response status is 409
    And ride 5 has status "Completed"

  Scenario: The driver tries to finish a ride that does not exist
    Given ride 99 does not exist
    When driver "marc.lavoie@mail.mcgill.ca" attempts to finish ride 99
    Then the finish-ride response status is 404
