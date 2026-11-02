package ca.mcgill.ecse428.UniRide_Backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

@Service
public class SeatRequestService {

    @Autowired
    private SeatRequestRepository seatRequestRepository;

    private final SeatRequestValidator validator = new SeatRequestValidator();

    // TID074: Implement the request_seat service
    @Transactional
    public SeatRequest createSeatRequest(Passenger passenger, Ride ride, int requestedSeats) {
        
        boolean hasExistingRequest = seatRequestRepository.existsByPassengerAndRide(passenger, ride);
        validator.validatePassengerConstraints(passenger, ride, hasExistingRequest);
        validator.validateRide(ride);
        
        List<SeatRequest> acceptedRequests = seatRequestRepository.findByRideAndStatus(ride, SeatRequestStatus.ACCEPTED);
        int acceptedSeats = 0;
        for (SeatRequest request : acceptedRequests) {
            acceptedSeats += request.getSeatsRequested();
        }
        
        validator.validateRequestedSeats(requestedSeats, ride.getTotalSeats(), acceptedSeats);

        SeatRequest seatRequest = new SeatRequest(passenger, ride, requestedSeats);
        return seatRequestRepository.save(seatRequest);
    }
}