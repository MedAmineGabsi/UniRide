// TID074
package ca.mcgill.ecse428.UniRide_Backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.PassengerRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

@Service
public class SeatRequestService {

    private final SeatRequestRepository seatRequestRepository;
    private final PassengerRepository passengerRepository;
    private final RideRepository rideRepository;
    
    private final SeatRequestValidator validator = new SeatRequestValidator();

    public SeatRequestService(SeatRequestRepository seatRequestRepository, PassengerRepository passengerRepository, RideRepository rideRepository) {
        this.seatRequestRepository = seatRequestRepository;
        this.passengerRepository = passengerRepository;
        this.rideRepository = rideRepository;
    }

    @Transactional
    public SeatRequest createSeatRequest(String passengerEmail, Integer rideId, int requestedSeats) {
        
        Passenger passenger = passengerRepository.findByPersonEmail(passengerEmail)
                .orElseThrow(() -> new UniRideException(HttpStatus.NOT_FOUND, "Passenger not found"));
        
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new UniRideException(HttpStatus.NOT_FOUND, "Ride not found"));

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