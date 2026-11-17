package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PassengerRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

@ExtendWith(MockitoExtension.class)
class SeatRequestServiceTest {

    private static final String EMAIL = "jef.smith@mail.mcgill.ca";

    /** 10:00 in Montréal on 2026-11-03. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T15:00:00Z"),
            ZoneId.of("America/Toronto"));

    @Mock private SeatRequestRepository seatRequests;
    @Mock private PassengerRepository passengers;
    @Mock private RideRepository rides;
    @Mock private DriverRepository drivers;
    @Mock private SeatAvailabilityCalculator seatAvailability;
    @Mock private Passenger passenger;
    @Mock private Ride ride;

    private SeatRequestService service;

    @BeforeEach
    void setUp() {
        service = new SeatRequestService(seatRequests, passengers, rides, drivers, seatAvailability, CLOCK);
    }

    @Test
    void missingRideIdIsABadRequestAndNothingIsLookedUp() {
        UniRideException error = assertThrows(UniRideException.class,
                () -> service.createSeatRequest(EMAIL, null, 1));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals(SeatRequestService.RIDE_ID_REQUIRED_MESSAGE, error.getMessage());
        verifyNoInteractions(passengers, rides, drivers, seatRequests, seatAvailability);
    }

    @Test
    void rideThatHasAlreadyLeftIsRejectedEvenIfStillOpen() {
        openRideDepartingAt(LocalDateTime.of(2026, 11, 3, 9, 30));

        UniRideException error = assertThrows(UniRideException.class,
                () -> service.createSeatRequest(EMAIL, 1, 1));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals(SeatRequestValidator.RIDE_ALREADY_DEPARTED_MESSAGE, error.getMessage());
        verify(seatRequests, never()).save(any());
    }

    @Test
    void availableSeatsComeFromTheSeatAvailabilityCalculator() {
        openRideDepartingAt(LocalDateTime.of(2026, 11, 10, 9, 0));
        when(seatAvailability.calculateAvailableSeats(ride)).thenReturn(1);

        UniRideException error = assertThrows(UniRideException.class,
                () -> service.createSeatRequest(EMAIL, 1, 2));
        assertEquals("Not enough seats available", error.getMessage());
        verify(seatRequests, never()).save(any());
    }

    @Test
    void requestWithinAvailableSeatsIsSaved() {
        openRideDepartingAt(LocalDateTime.of(2026, 11, 10, 9, 0));
        when(seatAvailability.calculateAvailableSeats(ride)).thenReturn(1);
        when(seatRequests.save(any(SeatRequest.class))).thenAnswer(call -> call.getArgument(0));

        SeatRequest saved = service.createSeatRequest(EMAIL, 1, 1);
        assertEquals(1, saved.getSeatsRequested());
        verify(seatRequests).save(any(SeatRequest.class));
    }

    @Test
    void driverCannotRequestASeatOnTheirOwnRide() {
        when(passengers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(passenger));
        when(rides.findById(1)).thenReturn(Optional.of(ride));
        Driver rideDriver = mock(Driver.class);
        when(rideDriver.getId()).thenReturn(7);
        when(ride.getDriver()).thenReturn(rideDriver);
        Driver requesterAsDriver = mock(Driver.class);
        when(requesterAsDriver.getId()).thenReturn(7);
        when(drivers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(requesterAsDriver));

        UniRideException error = assertThrows(UniRideException.class,
                () -> service.createSeatRequest(EMAIL, 1, 1));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals(SeatRequestValidator.OWN_RIDE_MESSAGE, error.getMessage());
        verify(seatRequests, never()).save(any());
    }

    @Test
    void driverCanRequestASeatOnSomeoneElsesRide() {
        openRideDepartingAt(LocalDateTime.of(2026, 11, 10, 9, 0));
        Driver rideDriver = mock(Driver.class);
        when(rideDriver.getId()).thenReturn(7);
        when(ride.getDriver()).thenReturn(rideDriver);
        Driver requesterAsDriver = mock(Driver.class);
        when(requesterAsDriver.getId()).thenReturn(8);
        when(drivers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(requesterAsDriver));
        when(seatAvailability.calculateAvailableSeats(ride)).thenReturn(4);
        when(seatRequests.save(any(SeatRequest.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(1, service.createSeatRequest(EMAIL, 1, 1).getSeatsRequested());
    }

    @Test
    void acceptingLastAvailableSeatMarksRideFull() {
        SeatRequest request = pendingRequestOnRide(ride);
        when(ride.getStatus()).thenReturn(RideStatus.OPEN);
        when(ride.getDepartureDateTime()).thenReturn(LocalDateTime.of(2026, 11, 10, 9, 0));
        when(request.getSeatsRequested()).thenReturn(1);
        when(seatAvailability.calculateAvailableSeats(ride)).thenReturn(1);
        when(seatRequests.save(request)).thenReturn(request);

        service.respondToSeatRequest(1, EMAIL, "ACCEPTED");

        verify(ride).setStatus(RideStatus.FULL);
        verify(rides).save(ride);
        verify(request).setStatus(SeatRequestStatus.ACCEPTED);
    }

    @Test
    void rejectingRequestOnFullRideDoesNotRequireAvailableSeats() {
        SeatRequest request = pendingRequestOnRide(ride);
        when(seatRequests.save(request)).thenReturn(request);

        service.respondToSeatRequest(1, EMAIL, "REJECTED");

        verify(request).setStatus(SeatRequestStatus.REJECTED);
        verify(seatAvailability, never()).calculateAvailableSeats(ride);
        verify(rides, never()).save(ride);
    }

    @Test
    void acceptingRequestOnCancelledRideIsRejectedWithoutUpdatingRequest() {
        SeatRequest request = pendingRequestOnRide(ride);
        when(ride.getStatus()).thenReturn(RideStatus.CANCELLED);

        UniRideException error = assertThrows(UniRideException.class,
                () -> service.respondToSeatRequest(1, EMAIL, "ACCEPTED"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("Ride is not open for requests", error.getMessage());
        verify(seatRequests, never()).save(request);
        verify(seatAvailability, never()).calculateAvailableSeats(ride);
    }

    @Test
    void acceptingRequestOnDepartedRideIsRejectedWithoutUpdatingRequest() {
        SeatRequest request = pendingRequestOnRide(ride);
        when(ride.getStatus()).thenReturn(RideStatus.OPEN);
        when(ride.getDepartureDateTime()).thenReturn(LocalDateTime.of(2026, 11, 3, 9, 30));

        UniRideException error = assertThrows(UniRideException.class,
                () -> service.respondToSeatRequest(1, EMAIL, "ACCEPTED"));

        assertEquals(SeatRequestValidator.RIDE_ALREADY_DEPARTED_MESSAGE, error.getMessage());
        verify(seatRequests, never()).save(request);
        verify(seatAvailability, never()).calculateAvailableSeats(ride);
    }

    @Test
    void driverCannotSetRequestToPending() {
        UniRideException error = assertThrows(UniRideException.class,
                () -> service.respondToSeatRequest(1, EMAIL, "PENDING"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("Only ACCEPTED or REJECTED statuses are allowed", error.getMessage());
        verifyNoInteractions(seatRequests, rides, drivers, seatAvailability);
    }

    private SeatRequest pendingRequestOnRide(Ride rideForRequest) {
        SeatRequest request = mock(SeatRequest.class);
        Driver rideDriver = mock(Driver.class);
        Driver requester = mock(Driver.class);
        when(rideDriver.getId()).thenReturn(7);
        when(requester.getId()).thenReturn(7);
        when(rideForRequest.getDriver()).thenReturn(rideDriver);
        when(drivers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(requester));
        when(seatRequests.findById(1)).thenReturn(Optional.of(request));
        when(request.getRide()).thenReturn(rideForRequest);
        when(request.getStatus()).thenReturn(SeatRequestStatus.PENDING);
        return request;
    }

    private void openRideDepartingAt(LocalDateTime departure) {
        when(passengers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(passenger));
        when(rides.findById(1)).thenReturn(Optional.of(ride));
        when(ride.getStatus()).thenReturn(RideStatus.OPEN);
        when(ride.getDepartureDateTime()).thenReturn(departure);
    }
}
