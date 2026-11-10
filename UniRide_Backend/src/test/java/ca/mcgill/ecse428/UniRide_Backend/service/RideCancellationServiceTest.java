package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

class RideCancellationServiceTest {
    private static final String EMAIL = "driver@mail.mcgill.ca";
    private static final int RIDE_ID = 1;
    private static final LocalDateTime NOW = LocalDateTime.parse("2027-05-08T10:00");
    private static final ZoneId MONTREAL = ZoneId.of("America/Toronto");

    private final RideRepository rides = mock(RideRepository.class);
    private final DriverRepository drivers = mock(DriverRepository.class);
    private final SeatRequestRepository seatRequests = mock(SeatRequestRepository.class);
    private final Driver owner = mock(Driver.class);
    private final RideCancellationService service = new RideCancellationService(rides, drivers, seatRequests,
            Clock.fixed(NOW.atZone(MONTREAL).toInstant(), MONTREAL));

    // TID123: successful cancellation
    @ParameterizedTest
    @EnumSource(value = RideStatus.class, names = {"OPEN", "FULL"})
    void ownerCanCancelOpenOrFullRideBeforeDeparture(RideStatus status) {
        Ride ride = givenRide(status, NOW.plusHours(2), owner);
        assertEquals(ride, service.cancelRide(EMAIL, RIDE_ID));
        assertEquals(RideStatus.CANCELLED, ride.getStatus());
    }

    @Test
    void emailIsTrimmedBeforeLookingUpTheDriver() {
        Ride ride = givenRide(RideStatus.OPEN, NOW.plusHours(2), owner);
        service.cancelRide("  " + EMAIL + " ", RIDE_ID);
        assertEquals(RideStatus.CANCELLED, ride.getStatus());
    }

    // TID123: agreed seat-request behavior
    @Test
    void pendingAndAcceptedSeatRequestsAreCancelledWithTheRide() {
        Ride ride = givenRide(RideStatus.OPEN, NOW.plusHours(2), owner);
        SeatRequest pending = seatRequest(ride, SeatRequestStatus.PENDING);
        SeatRequest accepted = seatRequest(ride, SeatRequestStatus.ACCEPTED);
        when(seatRequests.findByRideAndStatusIn(ride, RideCancellationService.ACTIVE_REQUEST_STATUSES))
                .thenReturn(List.of(pending, accepted));

        service.cancelRide(EMAIL, RIDE_ID);

        assertEquals(SeatRequestStatus.CANCELLED, pending.getStatus());
        assertEquals(SeatRequestStatus.CANCELLED, accepted.getStatus());
    }

    @Test
    void onlyPendingAndAcceptedSeatRequestsAreLookedUp() {
        assertEquals(Set.of(SeatRequestStatus.PENDING, SeatRequestStatus.ACCEPTED),
                RideCancellationService.ACTIVE_REQUEST_STATUSES);
    }

    // TID123: unauthorized user
    @Test
    void otherDriverCannotCancelRide() {
        Driver otherDriver = mock(Driver.class);
        when(otherDriver.getId()).thenReturn(2);
        Ride ride = givenRide(RideStatus.OPEN, NOW.plusHours(2), otherDriver);
        assertRejected(HttpStatus.FORBIDDEN, RideCancellationService.ONLY_OWNER_MESSAGE);
        assertEquals(RideStatus.OPEN, ride.getStatus());
        verify(seatRequests, never()).findByRideAndStatusIn(any(), any());
    }

    @Test
    void userWithoutDriverRoleCannotCancelRide() {
        assertRejected(HttpStatus.FORBIDDEN, RideCancellationService.ONLY_OWNER_MESSAGE);
        verify(rides, never()).findByIdForUpdate(any());
    }

    // TID123: missing ride
    @Test
    void missingRideIsNotFound() {
        when(drivers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(owner));
        assertRejected(HttpStatus.NOT_FOUND, RideCancellationService.RIDE_NOT_FOUND_MESSAGE);
    }

    // TID123: invalid status
    @ParameterizedTest
    @EnumSource(value = RideStatus.class, names = {"CANCELLED", "COMPLETED"})
    void cancelledOrCompletedRideCannotBeCancelled(RideStatus status) {
        Ride ride = givenRide(status, NOW.plusHours(2), owner);
        assertRejected(HttpStatus.CONFLICT, RideCancellationService.INVALID_STATUS_MESSAGE);
        assertEquals(status, ride.getStatus());
        verify(seatRequests, never()).findByRideAndStatusIn(any(), any());
    }

    @Test
    void rideCannotBeCancelledAfterDeparture() {
        Ride ride = givenRide(RideStatus.OPEN, NOW.minusMinutes(1), owner);
        assertRejected(HttpStatus.CONFLICT, RideCancellationService.DEPARTED_MESSAGE);
        assertEquals(RideStatus.OPEN, ride.getStatus());
    }

    @Test
    void rideCannotBeCancelledAtItsDepartureTime() {
        Ride ride = givenRide(RideStatus.FULL, NOW, owner);
        assertRejected(HttpStatus.CONFLICT, RideCancellationService.DEPARTED_MESSAGE);
        assertEquals(RideStatus.FULL, ride.getStatus());
    }

    @Test
    void rideCanBeCancelledOneMinuteBeforeDeparture() {
        Ride ride = givenRide(RideStatus.OPEN, NOW.plusMinutes(1), owner);
        service.cancelRide(EMAIL, RIDE_ID);
        assertEquals(RideStatus.CANCELLED, ride.getStatus());
    }

    private Ride givenRide(RideStatus status, LocalDateTime departure, Driver requestingDriver) {
        when(owner.getId()).thenReturn(1);
        when(drivers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(requestingDriver));
        Ride ride = new Ride(owner, "Montreal", "Ottawa", departure, 60, 4, new BigDecimal("15.00"));
        ride.setStatus(status);
        when(rides.findByIdForUpdate(RIDE_ID)).thenReturn(Optional.of(ride));
        return ride;
    }

    private static SeatRequest seatRequest(Ride ride, SeatRequestStatus status) {
        SeatRequest request = new SeatRequest(mock(Passenger.class), ride, 1);
        request.setStatus(status);
        return request;
    }

    private void assertRejected(HttpStatus expected, String message) {
        UniRideException exception = assertThrows(UniRideException.class, () -> service.cancelRide(EMAIL, RIDE_ID));
        assertEquals(expected, exception.getStatus());
        assertEquals(message, exception.getMessage());
    }
}
