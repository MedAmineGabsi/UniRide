package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;

/** A ride that has already left cannot take seat requests, and seats are checked against what is left. */
class SeatRequestValidatorDepartureTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 10, 0);

    private final SeatRequestValidator validator = new SeatRequestValidator();

    @Test
    void rideThatLeftBeforeNowIsRejected() {
        UniRideException error = assertThrows(UniRideException.class,
                () -> validator.validateNotDeparted(rideDepartingAt(NOW.minusMinutes(1)), NOW));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals(SeatRequestValidator.RIDE_ALREADY_DEPARTED_MESSAGE, error.getMessage());
    }

    @Test
    void rideLeavingRightNowIsRejected() {
        assertThrows(UniRideException.class, () -> validator.validateNotDeparted(rideDepartingAt(NOW), NOW));
    }

    @Test
    void rideLeavingLaterIsAccepted() {
        assertDoesNotThrow(() -> validator.validateNotDeparted(rideDepartingAt(NOW.plusMinutes(1)), NOW));
    }

    @Test
    void seatsAreCheckedAgainstTheAvailableSeats() {
        assertDoesNotThrow(() -> validator.validateRequestedSeats(2, 2));
        UniRideException error = assertThrows(UniRideException.class,
                () -> validator.validateRequestedSeats(3, 2));
        assertEquals("Not enough seats available", error.getMessage());
        assertThrows(UniRideException.class, () -> validator.validateRequestedSeats(1, 0));
    }

    private static Ride rideDepartingAt(LocalDateTime departure) {
        Ride ride = mock(Ride.class);
        when(ride.getDepartureDateTime()).thenReturn(departure);
        return ride;
    }
}
