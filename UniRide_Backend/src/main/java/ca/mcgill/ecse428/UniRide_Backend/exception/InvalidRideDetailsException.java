package ca.mcgill.ecse428.UniRide_Backend.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

/** Thrown when one or more ride fields break a constraint. */
public class InvalidRideDetailsException extends UniRideException {

    public static final String MESSAGE = "Invalid Ride Details";

    /** Field name -> reason, e.g. "totalSeats" -> "Number of seats must be greater than 0". */
    private final Map<String, String> errors;

    public InvalidRideDetailsException(Map<String, String> errors) {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
        // keep field order so the first error shown matches the form order
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
