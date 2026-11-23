package ca.mcgill.ecse428.UniRide_Backend.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import ca.mcgill.ecse428.UniRide_Backend.TestcontainersConfiguration;
import ca.mcgill.ecse428.UniRide_Backend.dto.CarResponse;
import ca.mcgill.ecse428.UniRide_Backend.dto.RegisterUserRequest;
import ca.mcgill.ecse428.UniRide_Backend.dto.RoleType;
import ca.mcgill.ecse428.UniRide_Backend.model.Car;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.repository.CarRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.service.PersonService;
import tools.jackson.databind.json.JsonMapper;

/** HTTP contract checks for TID105, using real services and PostgreSQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CarControllerTest {
    private static final String DRIVER_EMAIL = "marc.lavoie@mail.mcgill.ca";
    private static final String OTHER_EMAIL = "sara.nguyen@mail.mcgill.ca";
    private static final String PASSENGER_EMAIL = "jef.smith@mail.mcgill.ca";
    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired private WebApplicationContext context;
    @Autowired private PersonService people;
    @Autowired private DriverRepository drivers;
    @Autowired private CarRepository cars;
    private MockMvc mvc;
    private Driver driver;
    private Car existingCar;
    private long initialCount;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        register(DRIVER_EMAIL, "260100001", RoleType.DRIVER, "L1234-567890-12");
        register(OTHER_EMAIL, "260100002", RoleType.DRIVER, "N6543-210987-65");
        register(PASSENGER_EMAIL, "260200001", RoleType.PASSENGER, null);
        driver = drivers.findByPersonEmail(DRIVER_EMAIL).orElseThrow();
        existingCar = cars.saveAndFlush(new Car(drivers.findByPersonEmail(OTHER_EMAIL).orElseThrow(),
                "Honda", "Civic", "Black", "ABC 123", 5));
        initialCount = cars.count();
    }

    @ParameterizedTest
    @CsvSource({"XYZ 789,XYZ 789,5", "f45-kpl,F45-KPL,4", "4k7   z9p,4K7 Z9P,2", "G12 HJK,G12 HJK,8"})
    void createsVehicleAndReturnsNormalizedPersistedDetails(String plate, String storedPlate, int seats)
            throws Exception {
        Map<String, Object> body = validBody();
        body.put("licensePlate", plate);
        body.put("seatCapacity", seats);
        MvcResult response = send(DRIVER_EMAIL, body)
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.driverId").value(driver.getId()))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Corolla"))
                .andExpect(jsonPath("$.color").value("Silver"))
                .andExpect(jsonPath("$.licensePlate").value(storedPlate))
                .andExpect(jsonPath("$.seatCapacity").value(seats))
                .andExpect(jsonPath("$.driver").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        CarResponse result = json.readValue(response.getResponse().getContentAsString(), CarResponse.class);
        Car saved = cars.findById(result.id()).orElseThrow();
        assertEquals(driver.getId(), saved.getDriver().getId());
        assertEquals(storedPlate, saved.getLicensePlate());
        assertEquals(seats, saved.getSeatCapacity());
        assertEquals("Toyota", saved.getMake());
        assertEquals("Corolla", saved.getModel());
        assertEquals("Silver", saved.getColor());
        assertEquals(initialCount + 1, cars.count());
        assertEquals("/api/cars/" + saved.getId(),
                response.getResponse().getHeader("Location"));
    }

    @Test
    void requiresSignIn() throws Exception {
        mvc.perform(post("/api/cars").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(validBody())))
                .andExpect(status().isUnauthorized());
        assertNoNewCar();
    }

    @Test
    void tokenWithoutEmailIsRejected() throws Exception {
        mvc.perform(post("/api/cars").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(validBody())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token does not contain an email"));
        assertNoNewCar();
    }

    @ParameterizedTest
    @ValueSource(strings = {"preferred_username", "upn"})
    void acceptsFallbackEmailClaims(String claim) throws Exception {
        mvc.perform(post("/api/cars").with(jwt().jwt(token -> token.claim(claim, DRIVER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverId").value(driver.getId()));
        assertTrue(cars.existsByDriverId(driver.getId()));
    }

    @Test
    void passengerCannotRegisterVehicle() throws Exception {
        send(PASSENGER_EMAIL, validBody()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only drivers are allowed to register a car"));
        assertNoNewCar();
    }

    @Test
    void unknownUserCannotRegisterVehicle() throws Exception {
        send("nobody@mail.mcgill.ca", validBody()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
        assertNoNewCar();
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidDetailsReturnFieldErrorsWithoutSaving(String field, Object value, String error) throws Exception {
        Map<String, Object> body = validBody();
        body.put(field, value);
        send(DRIVER_EMAIL, body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Car Details"))
                .andExpect(jsonPath("$.errors." + field).value(error));
        assertNoNewCar();
    }

    static Stream<Arguments> invalidFields() {
        Stream.Builder<Arguments> cases = Stream.builder();
        Map<String, String> labels = Map.of("make", "Make", "model", "Model", "color", "Color",
                "licensePlate", "License plate");
        labels.forEach((field, label) -> {
            for (String value : new String[] {null, "", "   "}) {
                cases.add(Arguments.of(field, value, label + " is required"));
            }
        });
        cases.add(Arguments.of("licensePlate", "-", "License plate is required"));
        cases.add(Arguments.of("seatCapacity", null, "Seat capacity is required"));
        for (int seats : new int[] {1, 0, -3}) {
            cases.add(Arguments.of("seatCapacity", seats, "Seat capacity must be greater than 1"));
        }
        Map.of("make", 50, "model", 50, "color", 30, "licensePlate", 20).forEach((field, limit) ->
                cases.add(Arguments.of(field, "X".repeat(limit + 1),
                        labels.get(field) + " must be at most " + limit + " characters")));
        return cases.build();
    }

    @Test
    void omittedFieldsReturnAllRequiredErrors() throws Exception {
        send(DRIVER_EMAIL, Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Car Details"))
                .andExpect(jsonPath("$.errors.make").value("Make is required"))
                .andExpect(jsonPath("$.errors.model").value("Model is required"))
                .andExpect(jsonPath("$.errors.color").value("Color is required"))
                .andExpect(jsonPath("$.errors.licensePlate").value("License plate is required"))
                .andExpect(jsonPath("$.errors.seatCapacity").value("Seat capacity is required"));
        assertNoNewCar();
    }

    @Test
    void driverCannotReplaceExistingCar() throws Exception {
        send(OTHER_EMAIL, validBody()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A driver can only have one car"));
        assertNoNewCar();
        Car unchanged = cars.findByDriverId(existingCar.getDriver().getId()).orElseThrow();
        assertEquals(existingCar.getId(), unchanged.getId());
        assertEquals("ABC 123", unchanged.getLicensePlate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC 123", "abc 123", "ABC123", "abc-123"})
    void equivalentDuplicatePlatesReturnConflict(String plate) throws Exception {
        Map<String, Object> body = validBody();
        body.put("licensePlate", plate);
        send(DRIVER_EMAIL, body).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A car with this license plate is already registered"));
        assertNoNewCar();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{ not json", "{\"seatCapacity\":\"abc\"}", ""})
    void unreadableBodyReturnsBadRequest(String body) throws Exception {
        mvc.perform(post("/api/cars").with(jwt().jwt(token -> token.claim("email", DRIVER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
        assertNoNewCar();
    }

    private ResultActions send(String email, Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/cars")
                .with(jwt().jwt(token -> token.claim("email", email)))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private static Map<String, Object> validBody() {
        return new LinkedHashMap<>(Map.of("make", "Toyota", "model", "Corolla", "color", "Silver",
                "licensePlate", "XYZ 789", "seatCapacity", 5));
    }

    private void register(String email, String studentId, RoleType role, String license) {
        people.registerUser(new RegisterUserRequest("Test", "User", studentId, email, "514-555-0000",
                "Str0ngPass!", "Str0ngPass!", List.of(role), license));
    }

    private void assertNoNewCar() {
        assertEquals(initialCount, cars.count());
        assertFalse(cars.existsByDriverId(driver.getId()));
    }
}
