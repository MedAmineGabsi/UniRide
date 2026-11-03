package ca.mcgill.ecse428.UniRide_Backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ca.mcgill.ecse428.UniRide_Backend.dto.SeatRequestDto;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.service.SeatRequestService;
import ca.mcgill.ecse428.UniRide_Backend.repository.PassengerRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;

@CrossOrigin(origins = "*")
@RestController
public class SeatRequestController {

    @Autowired
    private SeatRequestService seatRequestService;
    
    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private RideRepository rideRepository;

    @PostMapping("/seat-requests")
    public ResponseEntity<?> createSeatRequest(@RequestBody SeatRequestDto.Request requestDto) {
        try {
            Passenger passenger = passengerRepository.findByPersonEmail(requestDto.getPassengerEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));
            
            Ride ride = rideRepository.findById(requestDto.getRideId())
                    .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

            SeatRequest newRequest = seatRequestService.createSeatRequest(
                    passenger, ride, requestDto.getSeatsRequested());

            return new ResponseEntity<>(new SeatRequestDto.Response(newRequest), HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}