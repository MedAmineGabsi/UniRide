package ca.mcgill.ecse428.UniRide_Backend.service;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;

public class SeatRequestValidator {

    /** TID066: status must be one of the SeatRequestStatus values. */
    public SeatRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            return SeatRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    /** TID068: passenger constraints (driver cannot request their own ride, duplicate requests). */
    public void validatePassengerConstraints(Object passenger, Ride ride, boolean hasExistingRequest) {
        if (passenger == null || ride == null) {
            throw new IllegalArgumentException("Passenger and ride must not be null");
        }
        Object driver = ride.getDriver();
        if (driver != null && driver.equals(passenger)) {
            throw new IllegalArgumentException("A driver cannot request a seat on their own ride");
        }
        if (hasExistingRequest) {
            throw new IllegalArgumentException("Passenger has already requested a seat for this ride");
        }
    }

    // TID065: requested_seats constraint
    public void validateRequestedSeats(int requestedSeats, int totalSeats, int acceptedSeats) {
        if (requestedSeats <= 0) {
            throw new IllegalArgumentException("Invalid number of seats requested");
        }
        int availableSeats = totalSeats - acceptedSeats;
        if (requestedSeats > availableSeats) {
            throw new IllegalArgumentException("Not enough seats available");
        }
    }

    // TID067: ride constraints
    public void validateRide(Ride ride) {
        if (ride == null) {
            throw new IllegalArgumentException("Ride does not exist");
        }
        if (ride.getStatus() != ca.mcgill.ecse428.UniRide_Backend.model.RideStatus.OPEN) {
            throw new IllegalArgumentException("Ride is not open for requests");
        }
    }
}