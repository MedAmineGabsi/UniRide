Feature: Search For Rides

As a UniRide Passenger
I would like to search for available rides by origin, destination and departure date
So that I can find a ride that matches my travel plans

Background:
Given the following drivers exist:
| email                       | firstName | lastName | licenseNumber |
| marc.lavoie@mail.mcgill.ca  | Marc      | Lavoie   | QC123456      |
| sara.nguyen@mail.mcgill.ca  | Sara      | Nguyen   | QC654321      |
And the following passengers exist:
| email                    | studentID | firstName | lastName |
| jef.smith@mail.mcgill.ca | 260200001 | Jef       | Smith    |
And the following rides exist:
| ride_id | driver                     | origin   | destination | departure_date_time | total_seats | accepted_seats | price_per_seat | status    |
| 1       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2027-05-08 09:00    | 4           | 1              | 15.0           | Open      |
| 2       | sara.nguyen@mail.mcgill.ca | Montreal | Ottawa      | 2027-05-08 17:30    | 3           | 0              | 18.0           | Open      |
| 3       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2027-05-09 09:00    | 4           | 0              | 15.0           | Open      |
| 4       | sara.nguyen@mail.mcgill.ca | Montreal | Toronto     | 2027-05-08 08:00    | 3           | 2              | 30.0           | Open      |
| 5       | marc.lavoie@mail.mcgill.ca | Ottawa   | Montreal    | 2027-05-08 18:00    | 4           | 0              | 15.0           | Open      |
| 6       | sara.nguyen@mail.mcgill.ca | Montreal | Ottawa      | 2027-05-08 12:00    | 2           | 2              | 15.0           | Full      |
| 7       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2027-05-08 14:00    | 4           | 0              | 15.0           | Cancelled |
| 8       | sara.nguyen@mail.mcgill.ca | Montreal | Ottawa      | 2026-09-01 09:00    | 4           | 0              | 15.0           | Finished  |

Scenario: Passenger searches for rides with several matches (Normal Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides from Montreal to Ottawa on 2027-05-08
Then the following rides are returned in order of departure time:
| ride_id | driver      | origin   | destination | departure_date_time | price_per_seat | available_seats |
| 1       | Marc Lavoie | Montreal | Ottawa      | 2027-05-08 09:00    | 15.0           | 3               |
| 2       | Sara Nguyen | Montreal | Ottawa      | 2027-05-08 17:30    | 18.0           | 3               |

Scenario Outline: Passenger searches for rides with a single match (Normal Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides from <origin> to <destination> on <departure_date>
Then the following rides are returned in order of departure time:
| ride_id   | driver   | origin   | destination   | departure_date_time   | price_per_seat   | available_seats   |
| <ride_id> | <driver> | <origin> | <destination> | <departure_date_time> | <price_per_seat> | <available_seats> |

Examples:
| origin   | destination | departure_date | ride_id | driver      | departure_date_time | price_per_seat | available_seats |
| Montreal | Ottawa      | 2027-05-09     | 3       | Marc Lavoie | 2027-05-09 09:00    | 15.0           | 4               |
| Montreal | Toronto     | 2027-05-08     | 4       | Sara Nguyen | 2027-05-08 08:00    | 30.0           | 1               |
| Ottawa   | Montreal    | 2027-05-08     | 5       | Marc Lavoie | 2027-05-08 18:00    | 15.0           | 4               |

Scenario: Search results only show details relevant to the passenger (Normal Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides from Montreal to Ottawa on 2027-05-08
Then each returned ride does not show the driver's email, phone number or license number
And each returned ride does not show its total seats or accepted seat requests

Scenario Outline: Passenger searches with only some of the criteria (Alternate Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides with origin <origin>, destination <destination> and departure date <departure_date>
Then the rides <ride_ids> are returned in order of departure time

Examples:
| origin   | destination | departure_date | ride_ids      |
| Montreal |             |                | 4, 1, 2, 3    |
|          | Ottawa      |                | 1, 2, 3       |
|          |             | 2027-05-08     | 4, 1, 2, 5    |
| Montreal | Ottawa      |                | 1, 2, 3       |

Scenario Outline: Search is not case sensitive (Alternate Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides from <origin> to <destination> on 2027-05-08
Then the rides 1, 2 are returned in order of departure time

Examples:
| origin   | destination |
| montreal | ottawa      |
| MONTREAL | OTTAWA      |
| MontReal | OtTawa      |

Scenario Outline: Rides that are not open are excluded from the results (Alternate Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides with origin Montreal and destination Ottawa
Then the rides 1, 2, 3 are returned in order of departure time
And ride <ride_id> with status <status> is not returned

Examples:
| ride_id | status    |
| 6       | Full      |
| 7       | Cancelled |
| 8       | Finished  |

Scenario Outline: No rides match the search criteria (Alternate Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides from <origin> to <destination> on <departure_date>
Then no rides are returned
And a "No rides found" message is issued

Examples:
| origin   | destination | departure_date |
| Montreal | Quebec City | 2027-05-08     |
| Toronto  | Ottawa      | 2027-05-08     |
| Montreal | Ottawa      | 2027-05-20     |

Scenario Outline: Passenger searches with an invalid departure date (Error Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides from Montreal to Ottawa on <departure_date>
Then an "Invalid departure date" message is issued
And no rides are returned

Examples:
| departure_date |
| 2027-02-30     |
| 08/05/2027     |
| tomorrow       |

Scenario: Passenger searches for rides on a past date (Error Flow)

When passenger "jef.smith@mail.mcgill.ca" searches for rides from Montreal to Ottawa on 2026-09-01
Then a "Departure date cannot be in the past" message is issued
And no rides are returned

Scenario: Unregistered user attempts to search for rides (Error Flow)

Given no UniRide user exists with email "fred.smith@gmail.com"
When "fred.smith@gmail.com" searches for rides from Montreal to Ottawa on 2027-05-08
Then an "Unauthorized request" message is issued
And no rides are returned
