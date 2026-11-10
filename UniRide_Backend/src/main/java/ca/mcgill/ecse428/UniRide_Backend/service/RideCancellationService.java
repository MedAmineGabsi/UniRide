package ca.mcgill.ecse428.UniRide_Backend.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

/** Cancelling a ride (Feature ID21). */
@Service
public class RideCancellationService {

    public static final String ONLY_OWNER_MESSAGE = "Only the ride owner can cancel it";
    public static final String RIDE_NOT_FOUND_MESSAGE = "Ride not found";
    public static final String INVALID_STATUS_MESSAGE = "Only open or full rides can be cancelled";
    public static final String DEPARTED_MESSAGE = "Ride cannot be cancelled after departure";

    /** Ride statuses that can still be cancelled. */
    static final Set<RideStatus> CANCELLABLE_STATUSES = EnumSet.of(RideStatus.OPEN, RideStatus.FULL);

    /** Seat requests that still hold or wait for a seat; they are cancelled with the ride. */
    static final Set<SeatRequestStatus> ACTIVE_REQUEST_STATUSES =
            EnumSet.of(SeatRequestStatus.PENDING, SeatRequestStatus.ACCEPTED);

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final SeatRequestRepository seatRequestRepository;
    private final Clock clock;

    public RideCancellationService(RideRepository rideRepository, DriverRepository driverRepository,
            SeatRequestRepository seatRequestRepository, Clock clock) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.seatRequestRepository = seatRequestRepository;
        this.clock = clock;
    }

    /**
     * Cancels the ride and its pending and accepted seat requests. Rejected and already
     * cancelled requests are left as they are.
     *
     * @throws UniRideException 403 if the user is not the ride's driver, 404 if the ride does not
     *                          exist, 409 if the ride is not Open or Full or has already departed;
     *                          nothing is changed in those cases
     */
    @Transactional
    public Ride cancelRide(String driverEmail, Integer rideId) {
        Driver driver = driverRepository.findByPersonEmail(driverEmail.trim())
                .orElseThrow(() -> new UniRideException(HttpStatus.FORBIDDEN, ONLY_OWNER_MESSAGE));
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new UniRideException(HttpStatus.NOT_FOUND, RIDE_NOT_FOUND_MESSAGE));

        checkOwner(driver, ride);
        checkStatus(ride);
        checkNotDeparted(ride);

        ride.setStatus(RideStatus.CANCELLED);
        for (SeatRequest request : seatRequestRepository.findByRideAndStatusIn(ride, ACTIVE_REQUEST_STATUSES)) {
            request.setStatus(SeatRequestStatus.CANCELLED);
        }
        return ride;
    }

    private static void checkOwner(Driver driver, Ride ride) {
        if (!driver.getId().equals(ride.getDriver().getId())) {
            throw new UniRideException(HttpStatus.FORBIDDEN, ONLY_OWNER_MESSAGE);
        }
    }

    private static void checkStatus(Ride ride) {
        if (!CANCELLABLE_STATUSES.contains(ride.getStatus())) {
            throw new UniRideException(HttpStatus.CONFLICT, INVALID_STATUS_MESSAGE);
        }
    }

    /** A ride that has left (departure time reached) has to be finished instead. */
    private void checkNotDeparted(Ride ride) {
        if (!LocalDateTime.now(clock).isBefore(ride.getDepartureDateTime())) {
            throw new UniRideException(HttpStatus.CONFLICT, DEPARTED_MESSAGE);
        }
    }
}
