Feature: Request For Seat

As a UniRide Passenger
I would like to request seats on a ride
So that I can travel to my destination

Background:
Given the current date is "2026-11-01"
And the following drivers exist:
| email                     | firstName | lastName | licenseNumber | student_id |
| marc.lavoie@mail.mcgill.ca | Marc      | Lavoie   | QC123456     | 1234567890 |
And the following passengers exist:
| email                    | studentID | firstName | lastName |
| jef.smith@mail.mcgill.ca | 260200001 | Jef       | Smith    |
And the following rides exist:
| ride_id | driver                     | origin   | destination | departure_date_time | total_seats | accepted_seats | price_per_seat | status    |
| 1       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2026-11-10 09:00    | 4           | 0              | 15.0           | Open      |
| 2       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2026-11-10 14:00    | 4           | 1              | 15.0           | Open      |
| 3       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2026-11-11 09:00    | 4           | 2              | 15.0           | Open      |
| 4       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2026-11-11 12:00    | 3           | 2              | 15.0           | Open      |
| 5       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2026-11-12 09:00    | 4           | 4              | 15.0           | Full      |
| 6       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2026-11-12 14:00    | 4           | 0              | 15.0           | Cancelled |
| 7       | marc.lavoie@mail.mcgill.ca | Montreal | Ottawa      | 2026-10-20 09:00    | 4           | 4              | 15.0           | Finished  |

Scenario Outline: Passenger requests seats on an open ride (Normal Flow)

When passenger "jef.smith@mail.mcgill.ca" requests <seats_requested> seats on ride <ride_id>
Then a seat request is created for passenger "jef.smith@mail.mcgill.ca" on ride <ride_id> for <seats_requested> seats with status "Pending"

Examples:
| ride_id | seats_requested |
| 1       | 1               |
| 2       | 3               |

Scenario Outline: Passenger requests more seats than remaining (Error Flow)

When passenger "jef.smith@mail.mcgill.ca" requests <seats_requested> seats on ride <ride_id>
Then a "Not enough seats available" message is issued
And no seat request is created

Examples:
| ride_id | seats_requested |
| 3       | 3               |
| 4       | 2               |

Scenario Outline: Passenger already has a seat request on the ride (Error Flow)

Given passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride <ride_id> with status <existing_status>
When passenger "jef.smith@mail.mcgill.ca" requests <seats_requested> seats on ride <ride_id>
Then an "Already requested" message is issued
And no additional seat request is created

Examples:
| ride_id | existing_status | seats_requested |
| 1       | "Pending"       | 1               |
| 1       | "Accepted"      | 2               |

Scenario Outline: Passenger requests seats on a ride that is not open (Error Flow)

When passenger "jef.smith@mail.mcgill.ca" requests 1 seats on ride <ride_id>
Then a "Ride is not open for requests" message is issued
And no seat request is created

Examples:
| ride_id |
| 5       |
| 6       |
| 7       |

Scenario Outline: Passenger requests zero or negative seats (Error Flow)

When passenger "jef.smith@mail.mcgill.ca" requests <seats_requested> seats on ride <ride_id>
Then an "Invalid number of seats requested" message is issued
And no seat request is created

Examples:
| ride_id | seats_requested |
| 1       | 0               |
| 2       | -1              |
