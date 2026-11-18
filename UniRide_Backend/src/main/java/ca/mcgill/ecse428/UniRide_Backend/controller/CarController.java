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

import ca.mcgill.ecse428.UniRide_Backend.dto.CarResponse;
import ca.mcgill.ecse428.UniRide_Backend.dto.CreateCarRequest;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Car;
import ca.mcgill.ecse428.UniRide_Backend.service.CarService;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private static final List<String> EMAIL_CLAIMS = List.of("email", "preferred_username", "upn");

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    /**
     * Registers the signed-in driver's vehicle.
     * Returns 201 with the car, 400 with field errors for invalid details,
     * 403 for passengers, 404 for unknown users, or 409 for a duplicate car or plate.
     */
    @PostMapping
    public ResponseEntity<CarResponse> createCar(@AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateCarRequest request) {
        Car car = carService.createCar(emailOf(jwt), request);
        return ResponseEntity.created(URI.create("/api/cars/" + car.getId()))
                .body(CarResponse.from(car));
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
