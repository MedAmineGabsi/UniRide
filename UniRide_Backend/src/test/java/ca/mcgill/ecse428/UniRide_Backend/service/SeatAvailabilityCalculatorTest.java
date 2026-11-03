package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;

class SeatAvailabilityCalculatorTest {

    private final SeatAvailabilityCalculator calculator =
            new SeatAvailabilityCalculator();

    @Test
    void allSeatsAvailableWhenNoRequests() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(4);

        assertEquals(4,
                calculator.calculateAvailableSeats(ride, List.of()));
    }

    @Test
    void acceptedRequestsReduceAvailableSeats() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(5);

        SeatRequest request = mock(SeatRequest.class);
        when(request.getStatus()).thenReturn(SeatRequestStatus.ACCEPTED);
        when(request.getSeatsRequested()).thenReturn(2);

        assertEquals(3,
                calculator.calculateAvailableSeats(
                        ride, List.of(request)));
    }

    @Test
    void pendingAndRejectedRequestsDoNotReduceSeats() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(4);

        SeatRequest pending = mock(SeatRequest.class);
        when(pending.getStatus()).thenReturn(SeatRequestStatus.PENDING);

        SeatRequest rejected = mock(SeatRequest.class);
        when(rejected.getStatus()).thenReturn(SeatRequestStatus.REJECTED);

        assertEquals(4,
                calculator.calculateAvailableSeats(
                        ride, List.of(pending, rejected)));
    }

    @Test
    void fullyBookedRideHasZeroAvailableSeats() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(2);

        SeatRequest accepted = mock(SeatRequest.class);
        when(accepted.getStatus()).thenReturn(SeatRequestStatus.ACCEPTED);
        when(accepted.getSeatsRequested()).thenReturn(2);

        assertEquals(0,
                calculator.calculateAvailableSeats(
                        ride, List.of(accepted)));
    }
}
