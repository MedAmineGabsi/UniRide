@ID21 @wip
Feature: Cancel a Ride
  As a UniRide driver
  I want to cancel a ride I posted
  So that passengers know it will no longer take place and the ride is recorded as cancelled

  # TID121 rules: only the driver who posted the ride can cancel it.
  # Open and Full rides can be cancelled until their departure time.
  # Pending and Accepted seat requests on the ride become Cancelled; Rejected ones stay Rejected.

  Background:
    Given the current date and time is "2027-05-08 10:00"
    And the following drivers exist:
      | email                      | firstName | lastName | licenseNumber |
      | marc.lavoie@mail.mcgill.ca | Marc      | Lavoie   | QC123456      |
      | sara.nguyen@mail.mcgill.ca | Sara      | Nguyen   | QC654321      |
    And the following passengers exist:
      | email                    | studentID | firstName | lastName |
      | jef.smith@mail.mcgill.ca | 260200001 | Jef       | Smith    |
      | anna.lee@mail.mcgill.ca  | 260200002 | Anna      | Lee      |
      | tom.roy@mail.mcgill.ca   | 260200003 | Tom       | Roy      |
    And the following rides exist:
      | ride_id | driver                     | origin   | destination | departure_date_time | total_seats | accepted_seats | price_per_seat | status    |
      |       1 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 12:00 |           4 |              0 |          15.00 | Open      |
      |       2 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 15:00 |           4 |              4 |          15.00 | Full      |
      |       3 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 09:00 |           4 |              0 |          15.00 | Open      |
      |       4 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 10:00 |           4 |              0 |          15.00 | Open      |
      |       5 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-08 16:00 |           4 |              0 |          15.00 | Cancelled |
      |       6 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2027-05-01 09:00 |           4 |              0 |          15.00 | Completed |
    And passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride 1 with status "Pending"
    And passenger "anna.lee@mail.mcgill.ca" already has a seat request on ride 1 with status "Accepted"
    And passenger "tom.roy@mail.mcgill.ca" already has a seat request on ride 1 with status "Rejected"

  Scenario Outline: The driver cancels their own open or full ride (Normal Flow)
    When user "marc.lavoie@mail.mcgill.ca" attempts to cancel ride <ride_id>
    Then the cancel-ride response status is 204
    And ride <ride_id> has status "Cancelled"

    Examples:
      | ride_id |
      |       1 |
      |       2 |

  Scenario: Pending and accepted seat requests are cancelled with the ride (Normal Flow)
    When user "marc.lavoie@mail.mcgill.ca" attempts to cancel ride 1
    Then the cancel-ride response status is 204
    And the seat request of passenger "jef.smith@mail.mcgill.ca" on ride 1 has status "Cancelled"
    And the seat request of passenger "anna.lee@mail.mcgill.ca" on ride 1 has status "Cancelled"
    And the seat request of passenger "tom.roy@mail.mcgill.ca" on ride 1 has status "Rejected"

  Scenario Outline: Someone other than the ride's driver tries to cancel it (Error Flow)
    When user "<email>" attempts to cancel ride 1
    Then the cancel-ride response status is 403
    And an "Only the ride owner can cancel it" message is issued
    And ride 1 has status "Open"
    And the seat request of passenger "jef.smith@mail.mcgill.ca" on ride 1 has status "Pending"

    Examples:
      | email                      |
      | sara.nguyen@mail.mcgill.ca |
      | jef.smith@mail.mcgill.ca   |

  Scenario Outline: The driver tries to cancel a ride that has already departed (Error Flow)
    When user "marc.lavoie@mail.mcgill.ca" attempts to cancel ride <ride_id>
    Then the cancel-ride response status is 409
    And a "Ride cannot be cancelled after departure" message is issued
    And ride <ride_id> has status "Open"

    Examples:
      | ride_id |
      |       3 |
      |       4 |

  Scenario Outline: The driver tries to cancel a ride that is not open or full (Error Flow)
    When user "marc.lavoie@mail.mcgill.ca" attempts to cancel ride <ride_id>
    Then the cancel-ride response status is 409
    And an "Only open or full rides can be cancelled" message is issued
    And ride <ride_id> has status "<status>"

    Examples:
      | ride_id | status    |
      |       5 | Cancelled |
      |       6 | Completed |

  Scenario: The driver tries to cancel a ride that does not exist (Error Flow)
    Given ride 99 does not exist
    When user "marc.lavoie@mail.mcgill.ca" attempts to cancel ride 99
    Then the cancel-ride response status is 404
    And a "Ride not found" message is issued
