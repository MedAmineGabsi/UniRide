// TID069, TID070, TID071, TID072
package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;

class SeatRequestValidatorTest {

    private final SeatRequestValidator validator = new SeatRequestValidator();

    @Test
    void unknownStatusIsRejected() {
        assertThrows(UniRideException.class, () -> validator.parseStatus("DONE"));
    }

    @Test
    void blankStatusIsRejected() {
        assertThrows(UniRideException.class, () -> validator.parseStatus(" "));
    }

    @Test
    void nullStatusIsRejected() {
        assertThrows(UniRideException.class, () -> validator.parseStatus(null));
    }

    @Test
    void statusWithSurroundingWhitespaceIsTrimmed() {
        assertEquals(SeatRequestStatus.PENDING, validator.parseStatus("  pending  "));
    }

    @Test
    void validStatusesAreAccepted() {
        assertEquals(SeatRequestStatus.PENDING, validator.parseStatus("PENDING"));
        assertEquals(SeatRequestStatus.ACCEPTED, validator.parseStatus("accepted"));
        assertEquals(SeatRequestStatus.REJECTED, validator.parseStatus("REJECTED"));
        assertEquals(SeatRequestStatus.CANCELLED, validator.parseStatus("CANCELLED"));
    }

    @Test
    void driverCannotRequestOwnRide() {
        Driver driver = Mockito.mock(Driver.class);
        Ride ride = Mockito.mock(Ride.class);
        when(ride.getDriver()).thenReturn(driver);

        UniRideException e = assertThrows(UniRideException.class,
                () -> validator.validatePassengerConstraints(driver, ride, false));
        assertEquals("A driver cannot request a seat on their own ride", e.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void duplicateSeatRequestIsRejected() {
        Passenger passenger = Mockito.mock(Passenger.class);
        Driver driver = Mockito.mock(Driver.class);
        Ride ride = Mockito.mock(Ride.class);
        when(ride.getDriver()).thenReturn(driver);

        UniRideException e = assertThrows(UniRideException.class,
                () -> validator.validatePassengerConstraints(passenger, ride, true));
        assertEquals("Already requested", e.getMessage());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void validPassengerRequestIsAccepted() {
        Passenger passenger = Mockito.mock(Passenger.class);
        Driver driver = Mockito.mock(Driver.class);
        Ride ride = Mockito.mock(Ride.class);
        when(ride.getDriver()).thenReturn(driver);

        assertDoesNotThrow(() -> validator.validatePassengerConstraints(passenger, ride, false));
    }

    @Test
    void nullPassengerIsRejected() {
        Ride ride = Mockito.mock(Ride.class);
        assertThrows(UniRideException.class,
                () -> validator.validatePassengerConstraints(null, ride, false));
    }

    @Test
    void nullRideIsRejected() {
        Passenger passenger = Mockito.mock(Passenger.class);
        assertThrows(UniRideException.class,
                () -> validator.validatePassengerConstraints(passenger, null, false));
    }

    @Test
    void validRequestedSeatsAreAccepted() {
        assertDoesNotThrow(() -> validator.validateRequestedSeats(2, 4, 1));
    }

    @Test
    void zeroOrNegativeSeatsRejected() {
        UniRideException e1 = assertThrows(UniRideException.class, 
                () -> validator.validateRequestedSeats(0, 4, 0));
        assertEquals("Invalid number of seats requested", e1.getMessage());
        
        UniRideException e2 = assertThrows(UniRideException.class, 
                () -> validator.validateRequestedSeats(-1, 4, 0));
        assertEquals("Invalid number of seats requested", e2.getMessage());
    }

    @Test
    void exceedingAvailableSeatsRejected() {
        UniRideException e = assertThrows(UniRideException.class, 
                () -> validator.validateRequestedSeats(2, 4, 3));
        assertEquals("Not enough seats available", e.getMessage());
    }

    @Test
    void openRideIsAccepted() {
        Ride ride = Mockito.mock(Ride.class);
        when(ride.getStatus()).thenReturn(ca.mcgill.ecse428.UniRide_Backend.model.RideStatus.OPEN); 
        
        assertDoesNotThrow(() -> validator.validateRide(ride));
    }

    @Test
    void nonOpenRideIsRejected() {
        Ride ride = Mockito.mock(Ride.class);
        when(ride.getStatus()).thenReturn(ca.mcgill.ecse428.UniRide_Backend.model.RideStatus.FULL); 
        
        UniRideException e = assertThrows(UniRideException.class, 
                () -> validator.validateRide(ride));
        assertEquals("Ride is not open for requests", e.getMessage());
    }
}