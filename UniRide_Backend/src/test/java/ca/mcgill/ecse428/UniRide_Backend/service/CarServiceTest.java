package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import ca.mcgill.ecse428.UniRide_Backend.dto.CreateCarRequest;
import ca.mcgill.ecse428.UniRide_Backend.exception.InvalidCarDetailsException;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Car;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.repository.CarRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;

/** Car creation for TID086 (#54), including plate uniqueness for TID085 (#53). */
@ExtendWith(MockitoExtension.class)
class CarServiceTest {
    @Mock private CarRepository cars;
    @Mock private PersonRepository people;
    @Mock private DriverRepository drivers;
    @Mock private Driver driver;
    private CarService service;
    private final CreateCarRequest valid = new CreateCarRequest(" Honda ", " Civic ", " Blue ", " abc  123 ", 5);

    @BeforeEach
    void setUp() {
        service = new CarService(cars, people, drivers, new CarValidator());
    }

    @Test
    void createsCarWithNormalizedFieldsAndTheCorrectDriver() {
        when(driver.getId()).thenReturn(7);
        when(cars.saveAndFlush(any(Car.class))).thenAnswer(call -> call.getArgument(0));
        Car car = service.createCar(driver, valid);
        assertAll(() -> assertSame(driver, car.getDriver()),
                () -> assertEquals("Honda", car.getMake()),
                () -> assertEquals("Civic", car.getModel()),
                () -> assertEquals("Blue", car.getColor()),
                () -> assertEquals("ABC 123", car.getLicensePlate()),
                () -> assertEquals(5, car.getSeatCapacity()));
        verify(cars).existsByDriverId(7);
        verify(cars).existsByLicensePlateKey("ABC123");
        verify(cars).saveAndFlush(car);
    }

    @Test
    void missingDriverDoesNotReachTheRepository() {
        InvalidCarDetailsException error = assertThrows(InvalidCarDetailsException.class,
                () -> service.createCar((Driver) null, valid));
        assertEquals(Map.of("driver", "A car must have a driver"), error.getErrors());
        verifyNoInteractions(cars);
    }

    @Test
    void missingRequestDoesNotReachTheRepository() {
        InvalidCarDetailsException error = assertThrows(InvalidCarDetailsException.class,
                () -> service.createCar(driver, null));
        assertEquals(Map.of("request", "Car details are required"), error.getErrors());
        verifyNoInteractions(cars);
    }

    @Test
    void invalidFieldsAreReportedBeforeCheckingOrSavingDuplicates() {
        InvalidCarDetailsException error = assertThrows(InvalidCarDetailsException.class,
                () -> service.createCar(driver, new CreateCarRequest("", "Civic", "Blue", "---", 1)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals(Map.of("make", "Make is required", "licensePlate", "License plate is required",
                "seatCapacity", "Seat capacity must be greater than 1"), error.getErrors());
        verifyNoInteractions(cars);
    }

    @Test
    void driverWithAnExistingCarCannotCreateAnother() {
        when(driver.getId()).thenReturn(7);
        when(cars.existsByDriverId(7)).thenReturn(true);
        UniRideException error = assertThrows(UniRideException.class, () -> service.createCar(driver, valid));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals(CarValidator.ONE_CAR_PER_DRIVER_MESSAGE, error.getMessage());
        verify(cars, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc123", "ABC 123", "abc-123", " aBc - 123 "})
    void equivalentTakenPlatesCannotBeSaved(String plate) {
        when(cars.existsByLicensePlateKey("ABC123")).thenReturn(true);
        UniRideException error = assertThrows(UniRideException.class, () -> service.createCar(driver,
                new CreateCarRequest("Honda", "Civic", "Blue", plate, 5)));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals(CarValidator.LICENSE_PLATE_TAKEN_MESSAGE, error.getMessage());
        verify(cars, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"car_license_plate_key,A car with this license plate is already registered",
            "car_driver_id_key,A driver can only have one car"})
    void databaseDuplicateErrorsBecomeConflicts(String constraint, String message) {
        when(cars.saveAndFlush(any(Car.class))).thenThrow(new DataIntegrityViolationException(
                "Cannot save", new RuntimeException("duplicate key violates " + constraint)));
        UniRideException error = assertThrows(UniRideException.class, () -> service.createCar(driver, valid));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals(message, error.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown@mail.mcgill.ca"})
    void unknownUserCannotRegisterACar(String email) {
        UniRideException error = assertThrows(UniRideException.class, () -> service.createCar(email, valid));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        assertEquals(CarService.USER_NOT_FOUND_MESSAGE, error.getMessage());
        verifyNoInteractions(drivers, cars);
    }

    @Test
    void passengerCannotRegisterACar() {
        when(people.existsByEmailIgnoreCase("passenger@mail.mcgill.ca")).thenReturn(true);
        UniRideException error = assertThrows(UniRideException.class,
                () -> service.createCar("passenger@mail.mcgill.ca", valid));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        assertEquals(CarService.ONLY_DRIVERS_MESSAGE, error.getMessage());
        verifyNoInteractions(cars);
    }

    @Test
    void emailLookupUsesTrimmedEmailAndCreatesForTheFoundDriver() {
        when(people.existsByEmailIgnoreCase("driver@mail.mcgill.ca")).thenReturn(true);
        when(drivers.findByPersonEmail("driver@mail.mcgill.ca")).thenReturn(Optional.of(driver));
        when(cars.saveAndFlush(any(Car.class))).thenAnswer(call -> call.getArgument(0));
        assertSame(driver, service.createCar(" driver@mail.mcgill.ca ", valid).getDriver());
        verify(cars).saveAndFlush(any(Car.class));
    }
}
