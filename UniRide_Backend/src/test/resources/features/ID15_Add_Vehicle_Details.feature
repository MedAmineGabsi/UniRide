@ID15 @wip
Feature: Add Vehicle Details
  As a UniRide Driver
  I want to add the details of my vehicle to my profile
  So that passengers know which car to look for and my rides use my car's seat capacity

  Background:
    Given the following drivers exist:
      | email                      | firstName | lastName | licenseNumber | student_id |
      | marc.lavoie@mail.mcgill.ca | Marc      | Lavoie   | QC123456      | 260100001  |
      | sara.nguyen@mail.mcgill.ca | Sara      | Nguyen   | QC654321      | 260100002  |
    And the following passengers exist:
      | email                    | studentID | firstName | lastName |
      | jef.smith@mail.mcgill.ca | 260200001 | Jef       | Smith    |
    And the following cars exist:
      | driver                     | make  | model | color | license_plate | seat_capacity |
      | sara.nguyen@mail.mcgill.ca | Honda | Civic | Black | ABC 123       |             5 |

  Scenario Outline: Driver adds the details of their vehicle (Normal Flow)
    When driver "marc.lavoie@mail.mcgill.ca" adds a vehicle with the following details:
      | make   | model   | color   | license_plate   | seat_capacity   |
      | <make> | <model> | <color> | <license_plate> | <seat_capacity> |
    Then a vehicle is registered for driver "marc.lavoie@mail.mcgill.ca" with the following details:
      | make   | model   | color   | license_plate          | seat_capacity   |
      | <make> | <model> | <color> | <stored_license_plate> | <seat_capacity> |

    Examples:
      | make       | model    | color  | license_plate | seat_capacity | stored_license_plate |
      | Toyota     | Corolla  | Silver | XYZ 789       |             5 | XYZ 789              |
      | Volkswagen | Golf     | Blue   | f45-kpl       |             4 | F45-KPL              |
      | Mazda      | CX-5     | Red    | 4k7   z9p     |             2 | 4K7 Z9P              |
      | Kia        | Carnival | White  | G12 HJK       |             8 | G12 HJK              |

  Scenario: A passenger attempts to add vehicle details (Error Flow)
    When driver "jef.smith@mail.mcgill.ca" adds a vehicle with the following details:
      | make   | model   | color  | license_plate | seat_capacity |
      | Toyota | Corolla | Silver | XYZ 789       |             5 |
    Then an "Only drivers are allowed to register a car" message is issued
    And no new vehicle is registered

  Scenario: An unregistered user attempts to add vehicle details (Error Flow)
    When driver "nobody@mail.mcgill.ca" adds a vehicle with the following details:
      | make   | model   | color  | license_plate | seat_capacity |
      | Toyota | Corolla | Silver | XYZ 789       |             5 |
    Then a "User not found" message is issued
    And no new vehicle is registered

  Scenario Outline: Driver adds a vehicle with missing or invalid details (Error Flow)
    When driver "marc.lavoie@mail.mcgill.ca" adds a vehicle with the following details:
      | make   | model   | color   | license_plate   | seat_capacity   |
      | <make> | <model> | <color> | <license_plate> | <seat_capacity> |
    Then an "Invalid Car Details" message is issued
    And the "<field>" field error is "<field_error>"
    And no vehicle is registered for driver "marc.lavoie@mail.mcgill.ca"

    Examples: Missing fields
      | make   | model   | color  | license_plate | seat_capacity | field         | field_error               |
      |        | Corolla | Silver | XYZ 789       |             5 | make          | Make is required          |
      | Toyota |         | Silver | XYZ 789       |             5 | model         | Model is required         |
      | Toyota | Corolla |        | XYZ 789       |             5 | color         | Color is required         |
      | Toyota | Corolla | Silver |               |             5 | licensePlate  | License plate is required |
      | Toyota | Corolla | Silver | -             |             5 | licensePlate  | License plate is required |
      | Toyota | Corolla | Silver | XYZ 789       |               | seatCapacity  | Seat capacity is required |

    Examples: Invalid seat capacity
      | make   | model   | color  | license_plate | seat_capacity | field        | field_error                          |
      | Toyota | Corolla | Silver | XYZ 789       |             1 | seatCapacity | Seat capacity must be greater than 1 |
      | Toyota | Corolla | Silver | XYZ 789       |             0 | seatCapacity | Seat capacity must be greater than 1 |
      | Toyota | Corolla | Silver | XYZ 789       |            -3 | seatCapacity | Seat capacity must be greater than 1 |

    Examples: Fields too long
      | make                                                | model   | color                           | license_plate         | seat_capacity | field        | field_error                                  |
      | Toyota Motor Corporation Limited Special Edition XL | Corolla | Silver                          | XYZ 789               |             5 | make         | Make must be at most 50 characters           |
      | Toyota                                              | Corolla | Silver Metallic Pearl Frost Mix | XYZ 789               |             5 | color        | Color must be at most 30 characters          |
      | Toyota                                              | Corolla | Silver                          | ABCDEFGHIJKLMNOPQRSTU |             5 | licensePlate | License plate must be at most 20 characters  |

  Scenario: Driver who already has a vehicle attempts to add another one (Error Flow)
    When driver "sara.nguyen@mail.mcgill.ca" adds a vehicle with the following details:
      | make   | model   | color  | license_plate | seat_capacity |
      | Toyota | Corolla | Silver | XYZ 789       |             5 |
    Then an "A driver can only have one car" message is issued
    And driver "sara.nguyen@mail.mcgill.ca" still has the vehicle with license plate "ABC 123"

  Scenario Outline: Driver adds a vehicle with a license plate that is already registered (Error Flow)
    When driver "marc.lavoie@mail.mcgill.ca" adds a vehicle with the following details:
      | make   | model   | color  | license_plate   | seat_capacity |
      | Toyota | Corolla | Silver | <license_plate> |             5 |
    Then an "A car with this license plate is already registered" message is issued
    And no vehicle is registered for driver "marc.lavoie@mail.mcgill.ca"

    Examples:
      | license_plate |
      | ABC 123       |
      | abc 123       |
      | ABC123        |
      | abc-123       |
