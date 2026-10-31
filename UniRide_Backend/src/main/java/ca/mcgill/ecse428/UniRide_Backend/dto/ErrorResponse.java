package ca.mcgill.ecse428.UniRide_Backend.dto;

import java.util.Map;

/**
 * Error body returned by the API.
 *
 * @param message general message, e.g. "Invalid Ride Details"
 * @param errors  field name -> what is wrong with it (empty when not field-specific)
 */
public record ErrorResponse(String message, Map<String, String> errors) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, Map.of());
    }
}
