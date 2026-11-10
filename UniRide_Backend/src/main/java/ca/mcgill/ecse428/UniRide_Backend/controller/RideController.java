package ca.mcgill.ecse428.UniRide_Backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateRideRequest;
import ca.mcgill.ecse428.UniRide_Backend.dto.RideResponse;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.service.RideCancellationService;
import ca.mcgill.ecse428.UniRide_Backend.service.RideService;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    /** Claims that can hold the user's McGill email in a Microsoft Entra ID token, in order of preference. */
    private static final List<String> EMAIL_CLAIMS = List.of("email", "preferred_username", "upn");

    private final RideService rideService;
    private final RideCancellationService rideCancellationService;

    public RideController(RideService rideService, RideCancellationService rideCancellationService) {
        this.rideService = rideService;
        this.rideCancellationService = rideCancellationService;
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

    @PostMapping("/{rideId}/finish")
    public ResponseEntity<Void> finishRide(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer rideId) {
        rideService.finishRide(emailOf(jwt), rideId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancels the signed-in driver's ride, along with its pending and accepted seat requests.
     *
     * <p>204 on success; 403 if the user is not the ride's driver; 404 if the ride does not exist;
     * 409 if the ride is not Open or Full or has already departed.
     */
    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<Void> cancelRide(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer rideId) {
        rideCancellationService.cancelRide(emailOf(jwt), rideId);
        return ResponseEntity.noContent().build();
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
