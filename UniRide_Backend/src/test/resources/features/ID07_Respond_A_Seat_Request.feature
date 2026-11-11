@ID07
Feature: Respond to Seat Request
  As a UniRide Driver
  I want to approve or reject seat requests for my ride
  So that I can manage who travels with me and ensure I don't exceed my vehicle's capacity

  Background:
    Given the current date is "2026-11-01"
    And the following drivers exist:
      | email                      | firstName | lastName | licenseNumber | student_id |
      | marc.lavoie@mail.mcgill.ca | Marc      | Lavoie   | QC123456      | 1234567890 |
      | sara.nguyen@mail.mcgill.ca | Sara      | Nguyen   | QC654321      | 260100002  |
    And the following passengers exist:
      | email                    | studentID | firstName | lastName |
      | jef.smith@mail.mcgill.ca | 260200001 | Jef       | Smith    |
      | alice.wong@mail.mcgill.ca| 260200002 | Alice     | Wong     |
    And the following rides exist:
      | ride_id | driver                     | origin   | destination | departure_date_time | total_seats | accepted_seats | price_per_seat | status    |
      |       1 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2026-11-10 09:00 |           4 |              1 |           15.0 | Open      |
      |       2 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2026-11-10 14:00 |           4 |              4 |           15.0 | Full      |

  Scenario Outline: Driver successfully responds to a seat request (Normal Flow)
    Given passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride <ride_id> with status "Pending"
    When driver "<driver_email>" responds to the seat request for passenger "jef.smith@mail.mcgill.ca" on ride <ride_id> with "<new_status>"
    Then the seat request for passenger "jef.smith@mail.mcgill.ca" on ride <ride_id> has status "<new_status>"

    Examples:
      | ride_id | driver_email               | new_status |
      |       1 | marc.lavoie@mail.mcgill.ca | Accepted   |
      |       1 | marc.lavoie@mail.mcgill.ca | Rejected   |

  Scenario Outline: Unauthorized user attempts to respond to a seat request (Error Flow)
    Given passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride <ride_id> with status "Pending"
    When driver "<wrong_driver>" responds to the seat request for passenger "jef.smith@mail.mcgill.ca" on ride <ride_id> with "Accepted"
    Then an "Unauthorized: Only the driver of this ride can respond to requests" message is issued
    And the seat request for passenger "jef.smith@mail.mcgill.ca" on ride <ride_id> has status "Pending"

    Examples:
      | ride_id | wrong_driver               |
      |       1 | sara.nguyen@mail.mcgill.ca |

  Scenario Outline: Driver attempts to approve a seat request when capacity is full (Error Flow)
    Given passenger "alice.wong@mail.mcgill.ca" already has a seat request on ride <ride_id> with status "Pending"
    When driver "<driver_email>" responds to the seat request for passenger "alice.wong@mail.mcgill.ca" on ride <ride_id> with "Accepted"
    Then a "Not enough seats available" message is issued
    And the seat request for passenger "alice.wong@mail.mcgill.ca" on ride <ride_id> has status "Pending"

    Examples:
      | ride_id | driver_email               |
      |       2 | marc.lavoie@mail.mcgill.ca |

  Scenario Outline: Driver attempts to respond to a request that is not PENDING (Error Flow)
    Given passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride <ride_id> with status "<current_status>"
    When driver "<driver_email>" responds to the seat request for passenger "jef.smith@mail.mcgill.ca" on ride <ride_id> with "Accepted"
    Then a "Seat request must be PENDING to update status" message is issued
    And the seat request for passenger "jef.smith@mail.mcgill.ca" on ride <ride_id> has status "<current_status>"

    Examples:
      | ride_id | driver_email               | current_status |
      |       1 | marc.lavoie@mail.mcgill.ca | Accepted       |
      |       1 | marc.lavoie@mail.mcgill.ca | Rejected       |
      |       1 | marc.lavoie@mail.mcgill.ca | Cancelled      |