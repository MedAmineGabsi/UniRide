Feature: Create Ride Listing
  As a Licensed Driver
  I want to be able to create a ride listing on the UniRide Platform
  So that I can offer rides to other McGill Students to their desired destinations

  Scenario Outline: Create different rides (Normal Flow)
    Given user email "<email>" with user id "<id>" with role "Driver"
    When user "<email>" requests to create a ride listing with the following details:
      | departure_location   | arrival_location   | departure_time   | total_seats   | price_per_seat   |
      | <departure_location> | <arrival_location> | <departure_time> | <total_seats> | <price_per_seat> |
    Then a new ride "<ride_id>" should be created with the following details:
      | driver_id | departure_location   | arrival_location   | departure_time   | total_seats   | price_per_seat   |
      | <id>      | <departure_location> | <arrival_location> | <departure_time> | <total_seats> | <price_per_seat> |

    Examples:
      | email                      | id        | departure_location                                      | arrival_location                                        | departure_time   | total_seats | price_per_seat | ride_id   |
      | john.doe@mail.mcgill.ca    | 260987654 | 800 Boulevard René Lévesque Ouest, Montréal QC, H3B 2G7 |                   220 Yonge Street, Toronto ON, M5T 1E3 | 2026-12-02 15:00 |           4 |             12 | CRPMG0001 |
      | kyuin.li@mail.mcgill.ca    | 260987655 |                   220 Yonge Street, Toronto ON, M5T 1E3 | 800 Boulevard René Lévesque Ouest, Montréal QC, H3B 2G7 | 2026-11-05 15:00 |           3 |             20 | CRPMG0002 |
      | patel.singh@mail.mcgill.ca | 260987657 |                     130 Besserer St, Ottawa ON, K1N 9M9 |                    201 Rue Milton, Montréal, QC H2X 1V5 | 2026-11-10 15:00 |           2 |             18 | CRPMG0004 |

  Scenario Outline: A non-driver user attempts to create a ride listing (Error Flow)
    Given user email "<email>" with user id "<id>" with role "Passenger"
    When user "<email>" requests to create a ride listing with the following details:
      | departure_location   | arrival_location   | departure_time   | total_seats   | price_per_seat   |
      | <departure_location> | <arrival_location> | <departure_time> | <total_seats> | <price_per_seat> |
    Then a "Only drivers are allowed to create a ride" message is issued

    Examples:
      | email                     | id        | departure_location                    | arrival_location                      | departure_time   | total_seats | price_per_seat |
      | passenger1@mail.mcgill.ca | 260987700 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 | 2026-11-08 15:00 |           4 |             15 |

  Scenario Outline: A driver attempts to create a ride listing with invalid details (Error Flow)
    Given user email "<email>" with user id "<id>" with role "Driver"
    When user "<email>" requests to create a ride listing with the following details:
      | departure_location   | arrival_location   | departure_time   | total_seats   | price_per_seat   |
      | <departure_location> | <arrival_location> | <departure_time> | <total_seats> | <price_per_seat> |
    Then a "Invalid Ride Details" message is issued

    Examples: Invalid seats / price / past date / same location
      | email                   | id        | departure_location                    | arrival_location                      | departure_time   | total_seats | price_per_seat |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 | 2020-01-01 15:00 |           3 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 | 2026-11-08 15:00 |           0 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 | 2026-11-08 15:00 |           3 |             -5 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 2026-11-08 15:00 |           3 |             15 |

    Examples: Invalid time format
      | email                   | id        | departure_location                    | arrival_location                      | departure_time   | total_seats | price_per_seat |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 |                  |           3 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 | 2026-11-08 25:99 |           3 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 | 2026-02-30 15:00 |           3 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 | tomorrow at 3pm  |           3 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 | 220 Yonge Street, Toronto ON, M5T 1E3 | 08/11/2026 15:00 |           3 |             15 |

    Examples: Empty or invalid address
      | email                   | id        | departure_location                    | arrival_location                      | departure_time   | total_seats | price_per_seat |
      | john.doe@mail.mcgill.ca | 260987654 |                                       | 220 Yonge Street, Toronto ON, M5T 1E3 | 2026-11-08 15:00 |           3 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 |                                       | 2026-11-08 15:00 |           3 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | asdfghjkl                             | 220 Yonge Street, Toronto ON, M5T 1E3 | 2026-11-08 15:00 |           3 |             15 |
      | john.doe@mail.mcgill.ca | 260987654 | 252 Laurier Ave E, Ottawa ON, K1P 5G4 |                                 12345 | 2026-11-08 15:00 |           3 |             15 |
