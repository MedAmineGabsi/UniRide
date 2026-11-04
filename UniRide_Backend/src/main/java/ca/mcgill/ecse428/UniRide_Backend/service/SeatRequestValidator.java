// TID065, TID066, TID067, TID068
package ca.mcgill.ecse428.UniRide_Backend.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;

public class SeatRequestValidator {

    public static final String RIDE_ALREADY_DEPARTED_MESSAGE = "Ride has already departed";

    public SeatRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, "status is required");
        }
        try {
            return SeatRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
    }

    public void validatePassengerConstraints(Object passenger, Ride ride, boolean hasExistingRequest) {
        if (passenger == null || ride == null) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, "Passenger and ride must not be null");
        }
        Object driver = ride.getDriver();
        if (driver != null && driver.equals(passenger)) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, "A driver cannot request a seat on their own ride");
        }
        if (hasExistingRequest) {
            throw new UniRideException(HttpStatus.CONFLICT, "Already requested");
        }
    }

    public void validateRequestedSeats(int requestedSeats, int totalSeats, int acceptedSeats) {
        validateRequestedSeats(requestedSeats, totalSeats - acceptedSeats);
    }

    /** Positive, and no more than the seats still available on the ride. */
    public void validateRequestedSeats(int requestedSeats, int availableSeats) {
        if (requestedSeats <= 0) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, "Invalid number of seats requested");
        }
        if (requestedSeats > availableSeats) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, "Not enough seats available");
        }
    }

    public void validateRide(Ride ride) {
        if (ride == null) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, "Ride does not exist");
        }
        if (ride.getStatus() != ca.mcgill.ecse428.UniRide_Backend.model.RideStatus.OPEN) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, "Ride is not open for requests");
        }
    }

    /** A ride that has already left cannot take seat requests, even if its status is still OPEN. */
    public void validateNotDeparted(Ride ride, LocalDateTime now) {
        if (!ride.getDepartureDateTime().isAfter(now)) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, RIDE_ALREADY_DEPARTED_MESSAGE);
        }
    }
}
