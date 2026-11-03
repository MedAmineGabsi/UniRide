
package ca.mcgill.ecse428.UniRide_Backend.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.mcgill.ecse428.UniRide_Backend.model.Person;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;

public record SearchRideResponse(

        @JsonProperty("ride_id")
        Integer rideId,

        String driver,

        String origin,

        String destination,

        @JsonProperty("departure_date_time")
        String departureDateTime,

        @JsonProperty("price_per_seat")
        BigDecimal pricePerSeat,

        @JsonProperty("available_seats")
        int availableSeats
) {

    public static SearchRideResponse from(
            Ride ride,
            Person driverPerson,
            int availableSeats) {

        if (ride == null || driverPerson == null) {
            throw new IllegalArgumentException(
                    "Ride and driver cannot be null");
        }

        if (availableSeats < 0 ||
                availableSeats > ride.getTotalSeats()) {
            throw new IllegalArgumentException(
                    "Invalid available seats");
        }

        String driverName =
                driverPerson.getFirstName() + " " +
                driverPerson.getLastName();

        return new SearchRideResponse(
                ride.getId(),
                driverName.trim(),
                ride.getOrigin(),
                ride.getDestination(),
                ride.getDepartureDateTime().toString(),
                ride.getPricePerSeat(),
                availableSeats
        );
    }
}

