package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.CarRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;

class RideServiceFinishTest {
    private static final String EMAIL = "driver@mail.mcgill.ca";
    private static final int RIDE_ID = 1;
    private static final LocalDateTime NOW = LocalDateTime.parse("2027-05-08T13:00");
    private static final ZoneId MONTREAL = ZoneId.of("America/Toronto");
    private static final Instant CURRENT_INSTANT = NOW.atZone(MONTREAL).toInstant();

    private final RideRepository rides = mock(RideRepository.class);
    private final DriverRepository drivers = mock(DriverRepository.class);
    private final Driver owner = mock(Driver.class);
    private final RideService service = new RideService(rides, mock(PersonRepository.class), drivers,
            mock(CarRepository.class), mock(RideValidator.class), Clock.fixed(CURRENT_INSTANT, MONTREAL));

    @ParameterizedTest
    @EnumSource(value = RideStatus.class, names = {"OPEN", "FULL"})
    void ownerCanFinishAtScheduledArrival(RideStatus status) {
        Ride ride = givenRide(status, NOW.minusMinutes(60), owner);
        service.finishRide(EMAIL, RIDE_ID);
        assertEquals(RideStatus.COMPLETED, ride.getStatus());
    }

    @Test
    void otherDriverCannotFinishRide() {
        Driver otherDriver = mock(Driver.class);
        when(otherDriver.getId()).thenReturn(2);
        Ride ride = givenRide(RideStatus.FULL, NOW.minusHours(2), otherDriver);
        assertRejected(HttpStatus.FORBIDDEN);
        assertEquals(RideStatus.FULL, ride.getStatus());
    }

    @Test
    void userWithoutDriverRoleCannotFinishRide() {
        assertRejected(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingRideIsNotFound() {
        when(drivers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(owner));
        assertRejected(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = RideStatus.class, names = {"CANCELLED", "COMPLETED"})
    void terminalRideCannotBeFinished(RideStatus status) {
        Ride ride = givenRide(status, NOW.minusHours(2), owner);
        assertRejected(HttpStatus.CONFLICT);
        assertEquals(status, ride.getStatus());
    }

    @Test
    void rideCannotBeFinishedBeforeScheduledArrival() {
        Ride ride = givenRide(RideStatus.OPEN, NOW.minusMinutes(59), owner);
        assertRejected(HttpStatus.CONFLICT);
        assertEquals(RideStatus.OPEN, ride.getStatus());
    }

    private Ride givenRide(RideStatus status, LocalDateTime departure, Driver requestingDriver) {
        when(owner.getId()).thenReturn(1);
        when(drivers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(requestingDriver));
        Ride ride = new Ride(owner, "Montreal", "Ottawa", departure, 60, 4, new BigDecimal("15.00"));
        ride.setStatus(status);
        when(rides.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));
        return ride;
    }

    private void assertRejected(HttpStatus expected) {
        UniRideException exception = assertThrows(UniRideException.class, () -> service.finishRide(EMAIL, RIDE_ID));
        assertEquals(expected, exception.getStatus());
    }
}
