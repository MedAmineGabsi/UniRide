package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

class SeatAvailabilityCalculatorTest {

    private SeatRequestRepository seatRequestRepository;
    private SeatAvailabilityCalculator calculator;

    @BeforeEach
    void setUp() {
        seatRequestRepository = mock(SeatRequestRepository.class);
        calculator = new SeatAvailabilityCalculator(seatRequestRepository);
    }

    @Test
    void allSeatsAvailableWhenNoRequests() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(4);
        when(seatRequestRepository.findByRideAndStatus(
                ride, SeatRequestStatus.ACCEPTED)).thenReturn(List.of());

        assertEquals(4, calculator.calculateAvailableSeats(ride));
    }

    @Test
    void acceptedRequestsReduceAvailableSeats() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(5);

        SeatRequest request = mock(SeatRequest.class);
        when(request.getSeatsRequested()).thenReturn(2);
        when(seatRequestRepository.findByRideAndStatus(
                ride, SeatRequestStatus.ACCEPTED)).thenReturn(List.of(request));

        assertEquals(3, calculator.calculateAvailableSeats(ride));
    }

    @Test
    void overbookedRideHasZeroAvailableSeats() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(4);

        SeatRequest accepted = mock(SeatRequest.class);
        when(accepted.getSeatsRequested()).thenReturn(5);
        when(seatRequestRepository.findByRideAndStatus(
                ride, SeatRequestStatus.ACCEPTED)).thenReturn(List.of(accepted));

        assertEquals(0, calculator.calculateAvailableSeats(ride));
    }

    @Test
    void fullyBookedRideHasZeroAvailableSeats() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(2);

        SeatRequest accepted = mock(SeatRequest.class);
        when(accepted.getSeatsRequested()).thenReturn(2);
        when(seatRequestRepository.findByRideAndStatus(
                ride, SeatRequestStatus.ACCEPTED)).thenReturn(List.of(accepted));

        assertEquals(0, calculator.calculateAvailableSeats(ride));
    }

    @Test
    void onlyAcceptedRequestsAreLoaded() {
        Ride ride = mock(Ride.class);
        when(ride.getTotalSeats()).thenReturn(4);
        when(seatRequestRepository.findByRideAndStatus(
                ride, SeatRequestStatus.ACCEPTED)).thenReturn(List.of());

        calculator.calculateAvailableSeats(ride);

        verify(seatRequestRepository).findByRideAndStatus(
                ride, SeatRequestStatus.ACCEPTED);
    }

    @Test
    void nullRideIsRejectedWithoutQueryingRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateAvailableSeats(null));
        verifyNoInteractions(seatRequestRepository);
    }
}
