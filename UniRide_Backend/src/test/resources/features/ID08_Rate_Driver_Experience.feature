@ID08 @wip
Feature: Rate Driver Experience
  As a UniRide passenger
  I want to rate my driver after a completed trip (1 to 5 stars, with an optional comment)
  So that other students can see how reliable and pleasant the driver is

  # Rules: the trip must be Completed, the rater must have had an Accepted seat request on it,
  # a user cannot rate themselves, and a passenger can rate a driver only once per trip.
  # The score is required and must be a whole number from 1 to 5.
  # The comment is optional and can be at most 500 characters.

  Background:
    Given the current date is "2026-11-10"
    And the following drivers exist:
      | email                      | firstName | lastName | licenseNumber | student_id |
      | marc.lavoie@mail.mcgill.ca | Marc      | Lavoie   | QC123456      | 260100010  |
    And the following passengers exist:
      | email                    | studentID | firstName | lastName |
      | jef.smith@mail.mcgill.ca | 260200001 | Jef       | Smith    |
      | anna.lee@mail.mcgill.ca  | 260200002 | Anna      | Lee      |
      | tom.roy@mail.mcgill.ca   | 260200003 | Tom       | Roy      |
    And the following rides exist:
      | ride_id | driver                     | origin   | destination | departure_date_time | total_seats | accepted_seats | price_per_seat | status    |
      |       1 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2026-11-01 09:00 |           4 |              2 |           15.0 | Completed |
      |       2 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2026-11-03 09:00 |           4 |              1 |           15.0 | Completed |
      |       3 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2026-11-12 09:00 |           4 |              1 |           15.0 | Open      |
      |       4 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2026-11-13 09:00 |           4 |              4 |           15.0 | Full      |
      |       5 | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      |    2026-11-14 09:00 |           4 |              1 |           15.0 | Cancelled |
    And passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride 1 with status "Accepted"
    And passenger "anna.lee@mail.mcgill.ca" already has a seat request on ride 1 with status "Accepted"
    And passenger "tom.roy@mail.mcgill.ca" already has a seat request on ride 1 with status "Rejected"
    And passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride 2 with status "Accepted"
    And passenger "anna.lee@mail.mcgill.ca" already has a seat request on ride 2 with status "Pending"
    And passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride 3 with status "Accepted"
    And passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride 4 with status "Accepted"
    And passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride 5 with status "Accepted"

  Scenario Outline: Passenger rates the driver of a completed trip (Normal Flow)
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 1 with score <score> and comment "<comment>"
    Then the rate-driver response status is 201
    And a rating is created by passenger "jef.smith@mail.mcgill.ca" for driver "marc.lavoie@mail.mcgill.ca" on ride 1 with score <score> and comment "<comment>"

    Examples:
      | score | comment                        |
      |     1 | Arrived 40 minutes late        |
      |     3 |                                |
      |     5 | Great driver, very punctual!   |

  Scenario: Driver average rating is updated after a new rating (Normal Flow)
    Given passenger "anna.lee@mail.mcgill.ca" has rated the driver of ride 1 with score 3
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 1 with score 4 and comment ""
    Then the rate-driver response status is 201
    And driver "marc.lavoie@mail.mcgill.ca" has an average rating of 3.5 from 2 ratings

  Scenario: Driver average rating includes ratings from different trips (Normal Flow)
    Given passenger "jef.smith@mail.mcgill.ca" has rated the driver of ride 1 with score 5
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 2 with score 4 and comment "Smooth ride"
    Then the rate-driver response status is 201
    And driver "marc.lavoie@mail.mcgill.ca" has an average rating of 4.5 from 2 ratings

  Scenario: Driver with no ratings has no average rating (Normal Flow)
    Then driver "marc.lavoie@mail.mcgill.ca" has no average rating

  Scenario Outline: Passenger gives a score outside 1 to 5 (Error Flow)
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 1 with score <score> and comment ""
    Then the rate-driver response status is 400
    And a "Score must be between 1 and 5" message is issued
    And no rating is created

    Examples:
      | score |
      |     0 |
      |     6 |
      |    -1 |

  Scenario: Passenger submits a rating without a score (Error Flow)
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 1 without a score
    Then the rate-driver response status is 400
    And a "Score is required" message is issued
    And no rating is created

  Scenario: Passenger submits a comment of exactly the maximum length (Normal Flow)
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 1 with score 4 and a comment of 500 characters
    Then the rate-driver response status is 201
    And a rating is created by passenger "jef.smith@mail.mcgill.ca" for driver "marc.lavoie@mail.mcgill.ca" on ride 1 with score 4

  Scenario: Passenger submits a comment that is too long (Error Flow)
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 1 with score 4 and a comment of 501 characters
    Then the rate-driver response status is 400
    And a "Comment must be at most 500 characters" message is issued
    And no rating is created

  Scenario Outline: Passenger rates the driver of a trip that is not completed (Error Flow)
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride <ride_id> with score 4 and comment ""
    Then the rate-driver response status is 409
    And a "Trip is not completed" message is issued
    And no rating is created

    Examples:
      | ride_id |
      |       3 |
      |       4 |
      |       5 |

  Scenario Outline: User who was not a passenger on the trip tries to rate the driver (Error Flow)
    When passenger "<email>" rates the driver of ride <ride_id> with score 4 and comment ""
    Then the rate-driver response status is 403
    And a "Not a passenger on this trip" message is issued
    And no rating is created

    Examples:
      | email                   | ride_id |
      | tom.roy@mail.mcgill.ca  |       1 |
      | anna.lee@mail.mcgill.ca |       2 |
      | tom.roy@mail.mcgill.ca  |       2 |

  Scenario: Driver tries to rate themselves on their own trip (Error Flow)
    When passenger "marc.lavoie@mail.mcgill.ca" rates the driver of ride 1 with score 5 and comment "Best driver ever"
    Then the rate-driver response status is 403
    And a "Cannot rate yourself" message is issued
    And no rating is created

  Scenario: Passenger rates the same trip twice (Error Flow)
    Given passenger "jef.smith@mail.mcgill.ca" has rated the driver of ride 1 with score 5
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 1 with score 2 and comment "Changed my mind"
    Then the rate-driver response status is 409
    And an "Already rated" message is issued
    And no additional rating is created
    And driver "marc.lavoie@mail.mcgill.ca" has an average rating of 5.0 from 1 ratings

  Scenario: Passenger rates the driver of a trip that does not exist (Error Flow)
    Given ride 99 does not exist
    When passenger "jef.smith@mail.mcgill.ca" rates the driver of ride 99 with score 4 and comment ""
    Then the rate-driver response status is 404
    And a "Trip not found" message is issued
    And no rating is created

  Scenario: Unregistered user tries to rate a driver (Error Flow)
    When passenger "fred.smith@gmail.com" rates the driver of ride 1 with score 4 and comment ""
    Then the rate-driver response status is 401
    And an "Unauthorized request" message is issued
    And no rating is created
