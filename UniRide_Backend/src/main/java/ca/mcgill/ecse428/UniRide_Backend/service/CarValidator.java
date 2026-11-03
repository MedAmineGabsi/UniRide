package ca.mcgill.ecse428.UniRide_Backend.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateCarRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.Car;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;

/**
 * Car creation constraints (TID083, TID084). Each {@code check*} method tests one constraint and
 * returns the error message if it is broken, or {@code Optional.empty()} if the value is valid.
 */
@Component
public class CarValidator {

    /** The driver plus at least one passenger. */
    public static final int MIN_SEAT_CAPACITY = 2;

    public static final int MAX_MAKE_LENGTH = 50;
    public static final int MAX_MODEL_LENGTH = 50;
    public static final int MAX_COLOR_LENGTH = 30;
    public static final int MAX_LICENSE_PLATE_LENGTH = 20;

    public static final String ONE_CAR_PER_DRIVER_MESSAGE = "A driver can only have one car";
    public static final String LICENSE_PLATE_TAKEN_MESSAGE = "A car with this license plate is already registered";

    /**
     * Runs every field constraint.
     *
     * @return field name -> error message, in form order; empty if the car is valid
     */
    public Map<String, String> validate(CreateCarRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        checkRequiredText("Make", request.make(), MAX_MAKE_LENGTH).ifPresent(e -> errors.put("make", e));
        checkRequiredText("Model", request.model(), MAX_MODEL_LENGTH).ifPresent(e -> errors.put("model", e));
        checkRequiredText("Color", request.color(), MAX_COLOR_LENGTH).ifPresent(e -> errors.put("color", e));
        checkLicensePlate(request.licensePlate()).ifPresent(e -> errors.put("licensePlate", e));
        checkSeatCapacity(request.seatCapacity()).ifPresent(e -> errors.put("seatCapacity", e));

        return errors;
    }

    // ---------------------------------------------------------------- driver

    /** A car always has a driver. */
    public Optional<String> checkDriver(Driver driver) {
        if (driver == null) {
            return Optional.of(Car.DRIVER_REQUIRED_MESSAGE);
        }
        return Optional.empty();
    }

    /** A driver can only have one car. */
    public Optional<String> checkDriverHasNoCar(boolean driverAlreadyHasCar) {
        if (driverAlreadyHasCar) {
            return Optional.of(ONE_CAR_PER_DRIVER_MESSAGE);
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- license_plate

    /** Required: not empty, and not only spaces or hyphens; short enough for its column. */
    public Optional<String> checkLicensePlate(String licensePlate) {
        Optional<String> error = checkRequiredText("License plate", licensePlate, MAX_LICENSE_PLATE_LENGTH);
        if (error.isPresent()) {
            return error;
        }
        if (licensePlateKey(licensePlate).isEmpty()) {
            return Optional.of("License plate is required");
        }
        return Optional.empty();
    }

    /** A license plate is unique. */
    public Optional<String> checkLicensePlateNotTaken(boolean licensePlateTaken) {
        if (licensePlateTaken) {
            return Optional.of(LICENSE_PLATE_TAKEN_MESSAGE);
        }
        return Optional.empty();
    }

    /** Plate as stored: trimmed, uppercase, single spaces ("  abc   123 " -> "ABC 123"). */
    public String normalizeLicensePlate(String licensePlate) {
        return licensePlate.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /**
     * Plate as compared for uniqueness: uppercase without spaces or hyphens, so "abc 123",
     * "ABC123" and "ABC-123" are the same plate. Matches the {@code car_license_plate_key} index.
     */
    public String licensePlateKey(String licensePlate) {
        return licensePlate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- seat_capacity

    /** Required, and greater than 1 (driver's seat included). */
    public Optional<String> checkSeatCapacity(Integer seatCapacity) {
        if (seatCapacity == null) {
            return Optional.of("Seat capacity is required");
        }
        if (seatCapacity < MIN_SEAT_CAPACITY) {
            return Optional.of("Seat capacity must be greater than 1");
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- make / model / color / license_plate

    /**
     * Required (not empty or only spaces), and short enough for its column.
     *
     * @param label "Make", "Model", "Color" or "License plate", used in the message
     */
    public Optional<String> checkRequiredText(String label, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return Optional.of(label + " is required");
        }
        if (value.trim().length() > maxLength) {
            return Optional.of(label + " must be at most " + maxLength + " characters");
        }
        return Optional.empty();
    }
}
