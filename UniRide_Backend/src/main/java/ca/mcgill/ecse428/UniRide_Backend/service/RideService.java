package ca.mcgill.ecse428.UniRide_Backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateRideRequest;
import ca.mcgill.ecse428.UniRide_Backend.exception.InvalidRideDetailsException;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.CarRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;

@Service
public class RideService {

    public static final String USER_NOT_FOUND_MESSAGE = "User not found";
    public static final String ONLY_DRIVERS_MESSAGE = "Only drivers are allowed to create a ride";

    private final RideRepository rideRepository;
    private final PersonRepository personRepository;
    private final DriverRepository driverRepository;
    private final CarRepository carRepository;
    private final RideValidator rideValidator;
    private final Clock clock;

    public RideService(RideRepository rideRepository, PersonRepository personRepository,
            DriverRepository driverRepository, CarRepository carRepository, RideValidator rideValidator, Clock clock) {
        this.rideRepository = rideRepository;
        this.personRepository = personRepository;
        this.driverRepository = driverRepository;
        this.carRepository = carRepository;
        this.rideValidator = rideValidator;
        this.clock = clock;
    }

    /**
     * Creates a ride listing for the user with this email.
     *
     * <p>If the driver has registered a car, the ride cannot offer more seats than the car's
     * passenger seats (its seat capacity minus the driver's seat).
     *
     * @throws UniRideException            404 if the user does not exist, 403 if they are not a driver
     * @throws InvalidRideDetailsException 400 "Invalid Ride Details" if any field breaks a constraint;
     *                                     nothing is saved in that case
     */
    @Transactional
    public Ride createRide(String driverEmail, CreateRideRequest request) {
        if (driverEmail == null || !personRepository.existsByEmailIgnoreCase(driverEmail.trim())) {
            throw new UniRideException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE);
        }
        Driver driver = driverRepository.findByPersonEmail(driverEmail.trim())
                .orElseThrow(() -> new UniRideException(HttpStatus.FORBIDDEN, ONLY_DRIVERS_MESSAGE));

        if (request == null) {
            throw new InvalidRideDetailsException(Map.of("request", "Ride details are required"));
        }
        Map<String, String> errors = rideValidator.validate(request, passengerSeatsInCarOf(driver));
        if (!errors.isEmpty()) {
            throw new InvalidRideDetailsException(errors);
        }

        LocalDateTime departure = rideValidator.parseDepartureDateTime(request.departureDateTime()).orElseThrow();
        BigDecimal price = request.pricePerSeat().setScale(2, RoundingMode.UNNECESSARY);

        Ride ride = new Ride(
                driver,
                request.origin().trim(),
                request.destination().trim(),
                departure,
                request.durationMinutes(),
                request.totalSeats(),
                price);
        return rideRepository.save(ride);
    }

    @Transactional
    public void finishRide(String driverEmail, Integer rideId) {
        Driver driver = driverRepository.findByPersonEmail(driverEmail.trim())
                .orElseThrow(() -> new UniRideException(HttpStatus.FORBIDDEN, "Only the ride owner can finish it"));
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new UniRideException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (!driver.getId().equals(ride.getDriver().getId())) {
            throw new UniRideException(HttpStatus.FORBIDDEN, "Only the ride owner can finish it");
        }
        if (ride.getStatus() != RideStatus.OPEN && ride.getStatus() != RideStatus.FULL) {
            throw new UniRideException(HttpStatus.CONFLICT, "Only open or full rides can be finished");
        }
        LocalDateTime arrival = ride.getDepartureDateTime().plusMinutes(ride.getDurationMinutes());
        if (LocalDateTime.now(clock).isBefore(arrival)) {
            throw new UniRideException(HttpStatus.CONFLICT, "Ride cannot be finished before scheduled arrival");
        }
        ride.setStatus(RideStatus.COMPLETED);
    }

    /**
     * Passenger seats in the driver's car: its seat capacity minus the driver's own seat.
     * Null if the driver has not registered a car, in which case no car limit applies.
     */
    private Integer passengerSeatsInCarOf(Driver driver) {
        return carRepository.findByDriverId(driver.getId())
                .map(car -> car.getSeatCapacity() - 1)
                .orElse(null);
    }
}
