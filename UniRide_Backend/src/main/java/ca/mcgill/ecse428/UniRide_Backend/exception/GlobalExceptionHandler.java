package ca.mcgill.ecse428.UniRide_Backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ca.mcgill.ecse428.UniRide_Backend.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRideDetailsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRideDetails(InvalidRideDetailsException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getMessage(), e.getErrors()));
    }

    @ExceptionHandler(UniRideException.class)
    public ResponseEntity<ErrorResponse> handleUniRideException(UniRideException e) {
        return ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(e.getMessage()));
    }

    /** Body is not valid JSON, or a field has the wrong type (e.g. "abc" for totalSeats). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of("Malformed request body"));
    }
}
