Feature: Request For Seat

As a UniRide Passenger
I would like to request seats on a ride
So that I can travel to my destination

Background:
Given the following drivers exist:
| email                     | firstName | lastName | licenseNumber |
| marc.lavoie@mail.mcgill.ca | Marc      | Lavoie   | QC123456      |
And the following passengers exist:
| email                    | studentID | firstName | lastName |
| jef.smith@mail.mcgill.ca | 260200001 | Jef       | Smith    |

Scenario Outline: Passenger requests seats on an open ride (Normal Flow)

Given ride <ride_id> by driver "marc.lavoie@mail.mcgill.ca" from "Montreal" to "Ottawa" has <total_seats> total seats, <accepted_seats> already accepted, price per seat <price> and status "Open"
When passenger "jef.smith@mail.mcgill.ca" requests <seats_requested> seats on ride <ride_id>
Then a seat request is created for passenger "jef.smith@mail.mcgill.ca" on ride <ride_id> with <seats_requested> seats requested and status "Pending"

Examples:
| ride_id | total_seats | accepted_seats | price | seats_requested |
| 1       | 4           | 0              | 15.0  | 1               |
| 2       | 4           | 1              | 15.0  | 3               |

Scenario Outline: Passenger requests more seats than remaining (Error Flow)

Given ride <ride_id> by driver "marc.lavoie@mail.mcgill.ca" from "Montreal" to "Ottawa" has <total_seats> total seats, <accepted_seats> already accepted, price per seat 15.0 and status "Open"
When passenger "jef.smith@mail.mcgill.ca" requests <seats_requested> seats on ride <ride_id>
Then a "Not enough seats available" message is issued
And no seat request is created

Examples:
| ride_id | total_seats | accepted_seats | seats_requested |
| 1       | 4           | 2              | 3               |
| 2       | 2           | 2              | 1               |

Scenario Outline: Passenger already has a seat request on the ride (Error Flow)

Given ride <ride_id> by driver "marc.lavoie@mail.mcgill.ca" from "Montreal" to "Ottawa" has 4 total seats, 0 already accepted, price per seat 15.0 and status "Open"
And passenger "jef.smith@mail.mcgill.ca" already has a seat request on ride <ride_id> with status <existing_status>
When passenger "jef.smith@mail.mcgill.ca" requests <seats_requested> seats on ride <ride_id>
Then an "Already requested" message is issued
And no additional seat request is created

Examples:
| ride_id | existing_status | seats_requested |
| 1       | "Pending"       | 1               |
| 1       | "Accepted"      | 2               |

Scenario Outline: Passenger requests seats on a closed or cancelled ride (Error Flow)

Given ride <ride_id> by driver "marc.lavoie@mail.mcgill.ca" from "Montreal" to "Ottawa" has 4 total seats, 0 already accepted, price per seat 15.0 and status <ride_status>
When passenger "jef.smith@mail.mcgill.ca" requests 1 seats on ride <ride_id>
Then a "Ride is not open for requests" message is issued
And no seat request is created

Examples:
| ride_id | ride_status |
| 1       | "Closed"    |
| 2       | "Cancelled" |
| 3       | "Completed" |

Scenario Outline: Passenger requests zero or negative seats (Error Flow)

Given ride <ride_id> by driver "marc.lavoie@mail.mcgill.ca" from "Montreal" to "Ottawa" has 4 total seats, 0 already accepted, price per seat 15.0 and status "Open"
When passenger "jef.smith@mail.mcgill.ca" requests <seats_requested> seats on ride <ride_id>
Then an "Invalid number of seats requested" message is issued
And no seat request is created

Examples:
| ride_id | seats_requested |
| 1       | 0               |
| 2       | -1              |
