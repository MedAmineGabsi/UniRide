
package ca.mcgill.ecse428.UniRide_Backend.service;

import java.util.List;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;

public class SeatAvailabilityCalculator {

    public int calculateAvailableSeats(
            Ride ride, List<SeatRequest> requests) {

        if (ride == null || requests == null) {
            throw new IllegalArgumentException(
                    "Ride and requests cannot be null");
        }

        int acceptedSeats = 0;

        for (SeatRequest request : requests) {
            if (request.getStatus() == SeatRequestStatus.ACCEPTED) {
                acceptedSeats += request.getSeatsRequested();
            }
        }

        return ride.getTotalSeats() - acceptedSeats;
    }
}
