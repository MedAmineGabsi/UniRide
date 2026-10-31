package ca.mcgill.ecse428.UniRide_Backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateRideRequest;
import ca.mcgill.ecse428.UniRide_Backend.dto.RideResponse;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.service.RideService;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    /** Claims that can hold the user's McGill email in a Microsoft Entra ID token, in order of preference. */
    private static final List<String> EMAIL_CLAIMS = List.of("email", "preferred_username", "upn");

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    /**
     * Creates a ride listing for the signed-in driver.
     *
     * <p>201 with the ride; 400 "Invalid Ride Details" (with per-field errors); 403 if the
     * user is not a driver; 404 if the user does not exist.
     */
    @PostMapping
    public ResponseEntity<RideResponse> createRide(@AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateRideRequest request) {
        Ride ride = rideService.createRide(emailOf(jwt), request);
        return ResponseEntity.created(URI.create("/api/rides/" + ride.getId()))
                .body(RideResponse.from(ride));
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
}
