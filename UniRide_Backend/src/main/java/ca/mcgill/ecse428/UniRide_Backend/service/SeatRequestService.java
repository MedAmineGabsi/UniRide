// TID074
package ca.mcgill.ecse428.UniRide_Backend.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.repository.PassengerRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

@Service
public class SeatRequestService {

    public static final String RIDE_ID_REQUIRED_MESSAGE = "Ride id is required";

    private final SeatRequestRepository seatRequestRepository;
    private final PassengerRepository passengerRepository;
    private final RideRepository rideRepository;
    private final SeatAvailabilityCalculator seatAvailabilityCalculator;
    private final Clock clock;

    private final SeatRequestValidator validator = new SeatRequestValidator();

    public SeatRequestService(SeatRequestRepository seatRequestRepository, PassengerRepository passengerRepository,
            RideRepository rideRepository, SeatAvailabilityCalculator seatAvailabilityCalculator, Clock clock) {
        this.seatRequestRepository = seatRequestRepository;
        this.passengerRepository = passengerRepository;
        this.rideRepository = rideRepository;
        this.seatAvailabilityCalculator = seatAvailabilityCalculator;
        this.clock = clock;
    }

    @Transactional
    public SeatRequest createSeatRequest(String passengerEmail, Integer rideId, int requestedSeats) {
        if (rideId == null) {
            throw new UniRideException(HttpStatus.BAD_REQUEST, RIDE_ID_REQUIRED_MESSAGE);
        }

        Passenger passenger = passengerRepository.findByPersonEmail(passengerEmail)
                .orElseThrow(() -> new UniRideException(HttpStatus.NOT_FOUND, "Passenger not found"));

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new UniRideException(HttpStatus.NOT_FOUND, "Ride not found"));

        boolean hasExistingRequest = seatRequestRepository.existsByPassengerAndRide(passenger, ride);
        validator.validatePassengerConstraints(passenger, ride, hasExistingRequest);
        validator.validateRide(ride);
        validator.validateNotDeparted(ride, LocalDateTime.now(clock));
        validator.validateRequestedSeats(requestedSeats, seatAvailabilityCalculator.calculateAvailableSeats(ride));

        SeatRequest seatRequest = new SeatRequest(passenger, ride, requestedSeats);
        return seatRequestRepository.save(seatRequest);
    }
}
