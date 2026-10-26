package ca.mcgill.ecse428.UniRide_Backend.exception;

import org.springframework.http.HttpStatus;

public class UniRideException extends RuntimeException {

    private final HttpStatus status;

    public UniRideException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
