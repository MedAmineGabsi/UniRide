// TID075
package ca.mcgill.ecse428.UniRide_Backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import ca.mcgill.ecse428.UniRide_Backend.dto.SeatRequestDto;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.service.SeatRequestService;

@CrossOrigin(origins = "*")
@RestController
public class SeatRequestController {

    private static final List<String> EMAIL_CLAIMS = List.of("email", "preferred_username", "upn");

    private final SeatRequestService seatRequestService;
    
    public SeatRequestController(SeatRequestService seatRequestService) {
        this.seatRequestService = seatRequestService;
    }

    @PostMapping("/seat-requests")
    public ResponseEntity<SeatRequestDto.Response> createSeatRequest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody SeatRequestDto.Request requestDto) {
        
        String passengerEmail = emailOf(jwt);

        SeatRequest newRequest = seatRequestService.createSeatRequest(
                passengerEmail, requestDto.getRideId(), requestDto.getSeatsRequested());

        return new ResponseEntity<>(new SeatRequestDto.Response(newRequest), HttpStatus.CREATED);
    }

    private static String emailOf(Jwt jwt) {
        if (jwt == null) {
            throw new UniRideException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        for (String claim : EMAIL_CLAIMS) {
            String value = jwt.getClaimAsString(claim);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new UniRideException(HttpStatus.UNAUTHORIZED, "Token does not contain an email");
    }

    @PatchMapping("/seat-requests/{id}")
    public ResponseEntity<SeatRequestDto.Response> respondToSeatRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @RequestBody SeatRequestDto.Request requestDto) {
        
        String driverEmail = emailOf(jwt);
        SeatRequestStatus newStatus = SeatRequestStatus.valueOf(requestDto.getStatus().toUpperCase());

        SeatRequest updatedRequest = seatRequestService.respondToSeatRequest(id, driverEmail, newStatus);

        return new ResponseEntity<>(new SeatRequestDto.Response(updatedRequest), HttpStatus.OK);
    }
}