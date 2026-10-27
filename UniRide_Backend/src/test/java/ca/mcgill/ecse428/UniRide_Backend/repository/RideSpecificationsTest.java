package ca.mcgill.ecse428.UniRide_Backend.repository;

import static ca.mcgill.ecse428.UniRide_Backend.repository.RideSpecifications.BY_DEPARTURE_TIME;
import static ca.mcgill.ecse428.UniRide_Backend.repository.RideSpecifications.openRidesMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse428.UniRide_Backend.TestcontainersConfiguration;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import jakarta.persistence.EntityManager;

/**
 * Runs the search filters against the same rides as the ID04 feature file Background.
 * Each test rolls back, so the rides never reach other tests.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RideSpecificationsTest {

    @Autowired
    private EntityManager entityManager;

    private SimpleJpaRepository<Ride, Integer> rides;

    private Ride ride1;
    private Ride ride2;
    private Ride ride3;
    private Ride ride4;
    private Ride ride5;
    private Ride fullRide;
    private Ride cancelledRide;
    private Ride completedRide;

    @BeforeEach
    void createRides() {
        rides = new SimpleJpaRepository<>(Ride.class, entityManager);

        Driver marc = new Driver("QC123456");
        Driver sara = new Driver("QC654321");
        entityManager.persist(marc);
        entityManager.persist(sara);

        ride1 = ride(marc, "Montreal", "Ottawa", "2027-05-08T09:00", RideStatus.OPEN);
        ride2 = ride(sara, "Montreal", "Ottawa", "2027-05-08T17:30", RideStatus.OPEN);
        ride3 = ride(marc, "Montreal", "Ottawa", "2027-05-09T09:00", RideStatus.OPEN);
        ride4 = ride(sara, "Montreal", "Toronto", "2027-05-08T08:00", RideStatus.OPEN);
        ride5 = ride(marc, "Ottawa", "Montreal", "2027-05-08T18:00", RideStatus.OPEN);
        fullRide = ride(sara, "Montreal", "Ottawa", "2027-05-08T12:00", RideStatus.FULL);
        cancelledRide = ride(marc, "Montreal", "Ottawa", "2027-05-08T14:00", RideStatus.CANCELLED);
        completedRide = ride(sara, "Montreal", "Ottawa", "2027-04-20T09:00", RideStatus.COMPLETED);
        entityManager.flush();
    }

    // TID051: blank search criteria unit tests
    @Test
    void allCriteriaBlankReturnsEveryOpenRide() {
        assertEquals(List.of(ride4, ride1, ride2, ride5, ride3), search("", "", null));
        assertEquals(List.of(ride4, ride1, ride2, ride5, ride3), search(null, null, null));
        assertEquals(List.of(ride4, ride1, ride2, ride5, ride3), search("  ", "  ", null));
    }

    @Test
    void blankOriginOrDestinationIsNotFiltered() {
        assertEquals(List.of(ride4, ride1, ride2, ride3), search("Montreal", "", null));
        assertEquals(List.of(ride1, ride2, ride3), search("", "Ottawa", null));
        assertEquals(List.of(ride4, ride1, ride2, ride5), search("", "", LocalDate.of(2027, 5, 8)));
    }

    @Test
    void allCriteriaGivenReturnsOnlyMatchingRides() {
        assertEquals(List.of(ride1, ride2), search("Montreal", "Ottawa", LocalDate.of(2027, 5, 8)));
        assertEquals(List.of(ride3), search("Montreal", "Ottawa", LocalDate.of(2027, 5, 9)));
        assertEquals(List.of(), search("Montreal", "Quebec City", LocalDate.of(2027, 5, 8)));
        assertEquals(List.of(), search("Montreal", "Ottawa", LocalDate.of(2027, 5, 20)));
    }

    // TID052: case-insensitive matching unit tests
    @Test
    void originAndDestinationIgnoreCase() {
        LocalDate may8 = LocalDate.of(2027, 5, 8);
        assertEquals(List.of(ride1, ride2), search("montreal", "ottawa", may8));
        assertEquals(List.of(ride1, ride2), search("MONTREAL", "OTTAWA", may8));
        assertEquals(List.of(ride1, ride2), search("MontReal", "OtTawa", may8));
    }

    @Test
    void originAndDestinationIgnoreSurroundingSpaces() {
        assertEquals(List.of(ride1, ride2), search("  Montreal ", " Ottawa  ", LocalDate.of(2027, 5, 8)));
    }

    // TID053: status filter unit tests
    @Test
    void fullCancelledAndCompletedRidesAreExcluded() {
        List<Ride> results = search("Montreal", "Ottawa", null);
        assertEquals(List.of(ride1, ride2, ride3), results);
        assertEquals(false, results.contains(fullRide));
        assertEquals(false, results.contains(cancelledRide));
        assertEquals(false, results.contains(completedRide));
    }

    @Test
    void rideBecomesSearchableWhenItIsOpenAgain() {
        fullRide.setStatus(RideStatus.OPEN);
        entityManager.flush();
        assertEquals(List.of(ride1, fullRide, ride2), search("Montreal", "Ottawa", LocalDate.of(2027, 5, 8)));
    }

    // TID054: sort order unit tests
    @Test
    void resultsAreSortedByDepartureTimeEarliestFirst() {
        assertEquals(List.of(ride4, ride1, ride2, ride5, ride3), search(null, null, null));
    }

    @Test
    void sortOrderDoesNotDependOnCreationOrder() {
        Driver marc = (Driver) ride1.getDriver();
        Ride earliest = ride(marc, "Montreal", "Ottawa", "2027-05-08T06:00", RideStatus.OPEN);
        entityManager.flush();
        assertEquals(List.of(earliest, ride1, ride2), search("Montreal", "Ottawa", LocalDate.of(2027, 5, 8)));
    }

    @Test
    void dateFilterIncludesTheWholeDayOnly() {
        Driver marc = (Driver) ride1.getDriver();
        Ride justBeforeMidnight = ride(marc, "Montreal", "Ottawa", "2027-05-08T23:59", RideStatus.OPEN);
        Ride atMidnight = ride(marc, "Montreal", "Ottawa", "2027-05-09T00:00", RideStatus.OPEN);
        entityManager.flush();
        assertEquals(List.of(ride1, ride2, justBeforeMidnight),
                search("Montreal", "Ottawa", LocalDate.of(2027, 5, 8)));
        assertEquals(List.of(atMidnight, ride3), search("Montreal", "Ottawa", LocalDate.of(2027, 5, 9)));
    }

    private List<Ride> search(String origin, String destination, LocalDate departureDate) {
        return rides.findAll(openRidesMatching(origin, destination, departureDate), BY_DEPARTURE_TIME);
    }

    private Ride ride(Driver driver, String origin, String destination, String departure, RideStatus status) {
        Ride ride = new Ride(driver, origin, destination, LocalDateTime.parse(departure), 4, new BigDecimal("15.00"));
        ride.setStatus(status);
        entityManager.persist(ride);
        return ride;
    }
}
