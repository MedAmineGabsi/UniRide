package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;

class SeatRequestValidatorTest {

    private final SeatRequestValidator validator = new SeatRequestValidator();

    // TID070: status constraint unit tests
    @Test
    void unknownStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.parseStatus("DONE"));
    }

    @Test
    void blankStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.parseStatus(" "));
    }

    @Test
    void validStatusesAreAccepted() {
        assertEquals(SeatRequestStatus.PENDING, validator.parseStatus("PENDING"));
        assertEquals(SeatRequestStatus.ACCEPTED, validator.parseStatus("accepted"));
        assertEquals(SeatRequestStatus.REJECTED, validator.parseStatus("REJECTED"));
        assertEquals(SeatRequestStatus.CANCELLED, validator.parseStatus("CANCELLED"));
    }

    // TID072: passenger constraint unit tests (driver requesting own ride & duplicate request)
    @Test
    void driverCannotRequestOwnRide() {
        Driver driver = Mockito.mock(Driver.class);
        Ride ride = Mockito.mock(Ride.class);
        when(ride.getDriver()).thenReturn(driver);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> validator.validatePassengerConstraints(driver, ride, false));
        assertEquals("A driver cannot request a seat on their own ride", e.getMessage());
    }

    @Test
    void duplicateSeatRequestIsRejected() {
        Passenger passenger = Mockito.mock(Passenger.class);
        Driver driver = Mockito.mock(Driver.class);
        Ride ride = Mockito.mock(Ride.class);
        when(ride.getDriver()).thenReturn(driver);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> validator.validatePassengerConstraints(passenger, ride, true));
        assertEquals("Passenger has already requested a seat for this ride", e.getMessage());
    }

    @Test
    void validPassengerRequestIsAccepted() {
        Passenger passenger = Mockito.mock(Passenger.class);
        Driver driver = Mockito.mock(Driver.class);
        Ride ride = Mockito.mock(Ride.class);
        when(ride.getDriver()).thenReturn(driver);

        assertDoesNotThrow(() -> validator.validatePassengerConstraints(passenger, ride, false));
    }
}