package ca.mcgill.ecse428.UniRide_Backend.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateRideRequest;
import ca.mcgill.ecse428.UniRide_Backend.exception.InvalidRideDetailsException;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Person;
import ca.mcgill.ecse428.UniRide_Backend.model.PersonRole;
import ca.mcgill.ecse428.UniRide_Backend.model.PersonRoleAssignment;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;
import ca.mcgill.ecse428.UniRide_Backend.service.RideService;
import ca.mcgill.ecse428.UniRide_Backend.service.RideValidator;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Step definitions for ID03_Create_Ride_Listing.feature (TID040). */
public class CreateRideStepDefinitions {

    /**
     * "Now" in every ID03 scenario. Pinned so the feature file's dates (Nov-Dec 2026) stay
     * in the future, while 2020-01-01 stays in the past.
     */
    private static final LocalDateTime CURRENT_TIME = LocalDateTime.of(2026, 10, 15, 12, 0);

    /** The feature file has no duration column, so rides use this unless a "duration" column is added. */
    private static final int DEFAULT_DURATION_MINUTES = 60;

    @Autowired
    private RideService rideService;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestClock testClock;

    @PersistenceContext
    private EntityManager entityManager;

    private Ride createdRide;
    private UniRideException error;
    private long rideCountBefore;

    // ------------------------------------------------------------------ hooks

    @Before("@ID03")
    public void setUp() {
        clearDatabase();
        testClock.setTo(CURRENT_TIME);
    }

    @After("@ID03")
    public void tearDown() {
        testClock.reset();
        clearDatabase();
    }

    // ------------------------------------------------------------------ Given

    @Given("user email {string} with user id {string} with role {string}")
    public void userWithRole(String email, String studentId, String role) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Person person = new Person(studentId, email, "test-password-hash", "Test", "User", null);
            entityManager.persist(person);

            PersonRole personRole = switch (role.trim().toLowerCase(Locale.ROOT)) {
                case "driver" -> new Driver("LIC-" + studentId);
                case "passenger" -> new Passenger();
                default -> throw new IllegalArgumentException("Unknown role in feature file: " + role);
            };
            entityManager.persist(personRole);
            entityManager.persist(new PersonRoleAssignment(person, personRole));
        });
    }

    // ------------------------------------------------------------------ When

    @When("user {string} requests to create a ride listing with the following details:")
    public void requestsToCreateRideListing(String email, DataTable details) {
        Map<String, String> row = details.asMaps().get(0);
        CreateRideRequest request = new CreateRideRequest(
                row.get("origin"),
                row.get("destination"),
                row.get("departure_date_time"),
                row.containsKey("duration") ? parseInteger(row.get("duration")) : DEFAULT_DURATION_MINUTES,
                parseInteger(row.get("total_seats")),
                parseDecimal(row.get("price_per_seat")));

        rideCountBefore = rideRepository.count();
        try {
            createdRide = rideService.createRide(email, request);
        } catch (UniRideException e) {
            error = e;
        }
    }

    // ------------------------------------------------------------------ Then

    /**
     * @param rideLabel the feature file's ride id (e.g. CRPMG0001). The app generates its own
     *                  numeric ids, so this is only used in failure messages.
     */
    @Then("a new ride {string} should be created with the following details:")
    public void newRideShouldBeCreated(String rideLabel, DataTable details) {
        assertNull(error, () -> "Ride " + rideLabel + " was not created: " + describe(error));
        assertEquals(rideCountBefore + 1, rideRepository.count(), "Exactly one ride should be created");

        Map<String, String> expected = details.asMaps().get(0);
        Ride ride = rideRepository.findById(createdRide.getId()).orElseThrow();

        assertEquals(expected.get("driver_id"), studentIdOfDriver(ride), "driver_id");
        assertEquals(expected.get("origin"), ride.getOrigin(), "origin");
        assertEquals(expected.get("destination"), ride.getDestination(), "destination");
        assertEquals(expected.get("departure_date_time"),
                ride.getDepartureDateTime().format(RideValidator.DEPARTURE_FORMAT), "departure_date_time");
        assertEquals((int) parseInteger(expected.get("total_seats")), ride.getTotalSeats(), "total_seats");
        assertEquals(0, parseDecimal(expected.get("price_per_seat")).compareTo(ride.getPricePerSeat()),
                () -> "price_per_seat: expected " + expected.get("price_per_seat") + " but was "
                        + ride.getPricePerSeat());
        if (expected.containsKey("duration")) {
            assertEquals((int) parseInteger(expected.get("duration")), ride.getDurationMinutes(), "duration");
        }
        assertEquals(RideStatus.OPEN, ride.getStatus(), "A new ride should be OPEN");
    }

    @Then("a {string} message is issued and no ride was created.")
    public void messageIssuedAndNoRideCreated(String expectedMessage) {
        assertNotNull(error, "Expected \"" + expectedMessage + "\" but the ride was created");
        assertEquals(expectedMessage, error.getMessage(), () -> describe(error));
        assertEquals(rideCountBefore, rideRepository.count(), "No ride should be created");
    }

    // ------------------------------------------------------------------ helpers

    private void clearDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE seat_request, ride, person_role_assignment, driver, passenger, "
                + "person_role, person RESTART IDENTITY CASCADE");
    }

    /** Student id of the person who owns the ride's Driver role. */
    private String studentIdOfDriver(Ride ride) {
        return jdbcTemplate.queryForObject("""
                SELECT p.student_id
                FROM ride r
                JOIN person_role_assignment a ON a.role_id = r.driver_id
                JOIN person p ON p.id = a.person_id
                WHERE r.id = ?
                """, String.class, ride.getId());
    }

    /** Empty cell -> null, so the service reports the field as missing. */
    private static Integer parseInteger(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
    }

    private static BigDecimal parseDecimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
    }

    private static String describe(UniRideException e) {
        if (e == null) {
            return "no error";
        }
        if (e instanceof InvalidRideDetailsException invalid) {
            return e.getMessage() + " " + invalid.getErrors();
        }
        return e.getMessage();
    }
}
