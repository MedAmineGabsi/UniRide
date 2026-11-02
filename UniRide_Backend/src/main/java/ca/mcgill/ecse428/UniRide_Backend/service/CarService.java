package ca.mcgill.ecse428.UniRide_Backend.service;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateCarRequest;
import ca.mcgill.ecse428.UniRide_Backend.exception.InvalidCarDetailsException;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Car;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.repository.CarRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;

@Service
public class CarService {

    public static final String USER_NOT_FOUND_MESSAGE = "User not found";
    public static final String ONLY_DRIVERS_MESSAGE = "Only drivers are allowed to register a car";

    /** Name of the unique index on the license plate (V5 migration). */
    private static final String LICENSE_PLATE_UNIQUE_INDEX = "car_license_plate_key";

    private final CarRepository carRepository;
    private final PersonRepository personRepository;
    private final DriverRepository driverRepository;
    private final CarValidator carValidator;

    public CarService(CarRepository carRepository, PersonRepository personRepository,
            DriverRepository driverRepository, CarValidator carValidator) {
        this.carRepository = carRepository;
        this.personRepository = personRepository;
        this.driverRepository = driverRepository;
        this.carValidator = carValidator;
    }

    /**
     * Registers the car of the driver with this email.
     *
     * @throws UniRideException            404 if the user does not exist, 403 if they are not a driver,
     *                                     409 if the driver already has a car or the plate is taken
     * @throws InvalidCarDetailsException  400 "Invalid Car Details" if any field breaks a constraint;
     *                                     nothing is saved in that case
     */
    @Transactional
    public Car createCar(String driverEmail, CreateCarRequest request) {
        if (driverEmail == null || !personRepository.existsByEmailIgnoreCase(driverEmail.trim())) {
            throw new UniRideException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE);
        }
        Driver driver = driverRepository.findByPersonEmail(driverEmail.trim())
                .orElseThrow(() -> new UniRideException(HttpStatus.FORBIDDEN, ONLY_DRIVERS_MESSAGE));
        return createCar(driver, request);
    }

    /**
     * Registers a car for this driver.
     *
     * @throws InvalidCarDetailsException 400 if the driver is missing or any field breaks a constraint
     * @throws UniRideException           409 if the driver already has a car or the plate is taken
     */
    @Transactional
    public Car createCar(Driver driver, CreateCarRequest request) {
        var missingDriver = carValidator.checkDriver(driver);
        if (missingDriver.isPresent()) {
            throw new InvalidCarDetailsException(Map.of("driver", missingDriver.get()));
        }
        if (request == null) {
            throw new InvalidCarDetailsException(Map.of("request", "Car details are required"));
        }
        Map<String, String> errors = carValidator.validate(request);
        if (!errors.isEmpty()) {
            throw new InvalidCarDetailsException(errors);
        }
        carValidator.checkDriverHasNoCar(carRepository.existsByDriverId(driver.getId()))
                .ifPresent(message -> {
                    throw new UniRideException(HttpStatus.CONFLICT, message);
                });
        String licensePlateKey = carValidator.licensePlateKey(request.licensePlate());
        carValidator.checkLicensePlateNotTaken(carRepository.existsByLicensePlateKey(licensePlateKey))
                .ifPresent(message -> {
                    throw new UniRideException(HttpStatus.CONFLICT, message);
                });

        Car car = new Car(
                driver,
                request.make().trim(),
                request.model().trim(),
                request.color().trim(),
                carValidator.normalizeLicensePlate(request.licensePlate()),
                request.seatCapacity());
        try {
            // flush now so a concurrent duplicate (same driver or same plate) fails here, on a unique index
            return carRepository.saveAndFlush(car);
        } catch (DataIntegrityViolationException e) {
            throw new UniRideException(HttpStatus.CONFLICT, duplicateMessage(e));
        }
    }

    /** Which unique rule the database rejected the car for. */
    private static String duplicateMessage(DataIntegrityViolationException e) {
        String cause = String.valueOf(e.getMostSpecificCause().getMessage());
        if (cause.contains(LICENSE_PLATE_UNIQUE_INDEX)) {
            return CarValidator.LICENSE_PLATE_TAKEN_MESSAGE;
        }
        return CarValidator.ONE_CAR_PER_DRIVER_MESSAGE;
    }
}
