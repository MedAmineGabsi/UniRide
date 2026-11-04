
package ca.mcgill.ecse428.UniRide_Backend.service;

import org.springframework.stereotype.Service;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

@Service
public class SeatAvailabilityCalculator {

    private final SeatRequestRepository seatRequestRepository;

    public SeatAvailabilityCalculator(SeatRequestRepository seatRequestRepository) {
        this.seatRequestRepository = seatRequestRepository;
    }

    public int calculateAvailableSeats(Ride ride) {
        if (ride == null) {
            throw new IllegalArgumentException("Ride cannot be null");
        }

        long acceptedSeats = 0;
        for (SeatRequest request : seatRequestRepository.findByRideAndStatus(
                ride, SeatRequestStatus.ACCEPTED)) {
            acceptedSeats += request.getSeatsRequested();
        }

        return (int) Math.max(0, ride.getTotalSeats() - acceptedSeats);
    }
}
