package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateRideRequest;
import ca.mcgill.ecse428.UniRide_Backend.exception.InvalidRideDetailsException;
import ca.mcgill.ecse428.UniRide_Backend.model.Car;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.repository.CarRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;

/** Ride creation uses the seats of the driver's registered car (seat capacity minus the driver). */
@ExtendWith(MockitoExtension.class)
class RideServiceCarSeatsTest {

    private static final String EMAIL = "driver@mail.mcgill.ca";

    @Mock private RideRepository rides;
    @Mock private PersonRepository people;
    @Mock private DriverRepository drivers;
    @Mock private CarRepository cars;
    @Mock private Driver driver;
    @Mock private Car car;

    private RideService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-03T15:00:00Z"), ZoneId.of("America/Toronto"));
        RideValidator validator = new RideValidator(clock);
        service = new RideService(rides, people, drivers, cars, validator, clock);
        when(people.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);
        when(drivers.findByPersonEmail(EMAIL)).thenReturn(Optional.of(driver));
        when(driver.getId()).thenReturn(7);
    }

    @Test
    void fiveSeatCarAllowsFourPassengerSeats() {
        when(cars.findByDriverId(7)).thenReturn(Optional.of(car));
        when(car.getSeatCapacity()).thenReturn(5);
        when(rides.save(any(Ride.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(4, service.createRide(EMAIL, request(4)).getTotalSeats());
    }

    @Test
    void moreSeatsThanTheCarHasAreRejectedAndNotSaved() {
        when(cars.findByDriverId(7)).thenReturn(Optional.of(car));
        when(car.getSeatCapacity()).thenReturn(5);

        InvalidRideDetailsException error = assertThrows(InvalidRideDetailsException.class,
                () -> service.createRide(EMAIL, request(5)));
        assertEquals(Map.of("totalSeats", "Number of seats cannot be more than the 4 passenger seats in your car"),
                error.getErrors());
        verify(rides, never()).save(any());
    }

    @Test
    void driverWithoutACarHasNoCarLimit() {
        when(cars.findByDriverId(7)).thenReturn(Optional.empty());
        when(rides.save(any(Ride.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(8, service.createRide(EMAIL, request(8)).getTotalSeats());
    }

    private static CreateRideRequest request(int totalSeats) {
        return new CreateRideRequest("252 Laurier Ave E, Ottawa ON, K1P 5G4",
                "220 Yonge Street, Toronto ON, M5T 1E3", "2026-11-10 09:00", 300, totalSeats,
                new BigDecimal("15.00"));
    }
}
