package ca.mcgill.ecse428.UniRide_Backend.service;

import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;

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
}