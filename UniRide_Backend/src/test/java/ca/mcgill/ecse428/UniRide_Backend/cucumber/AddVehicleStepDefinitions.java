package ca.mcgill.ecse428.UniRide_Backend.cucumber;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ca.mcgill.ecse428.UniRide_Backend.dto.CarResponse;
import ca.mcgill.ecse428.UniRide_Backend.dto.CreateCarRequest;
import ca.mcgill.ecse428.UniRide_Backend.dto.ErrorResponse;
import ca.mcgill.ecse428.UniRide_Backend.model.Car;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.repository.CarRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import tools.jackson.databind.json.JsonMapper;

/** ID15 steps reuse the existing driver/passenger fixtures and shared message assertion. */
public class AddVehicleStepDefinitions {
    @Autowired private WebApplicationContext context;
    @Autowired private CarRepository cars;
    @Autowired private DriverRepository drivers;
    @Autowired private LastResponse lastResponse;

    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;
    private long carCountBefore;

    @Before("@ID15")
    public void setUp() {
        // Existing global hooks reset the database before each scenario.
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Given("the following cars exist:")
    public void theFollowingCarsExist(DataTable details) {
        for (Map<String, String> row : details.asMaps()) {
            CreateCarRequest request = requestFrom(row);
            cars.saveAndFlush(new Car(driverOf(row.get("driver")), request.make(), request.model(),
                    request.color(), request.licensePlate(), request.seatCapacity()));
        }
    }

    @When("driver {string} adds a vehicle with the following details:")
    public void driverAddsVehicle(String email, DataTable details) throws Exception {
        carCountBefore = cars.count();
        lastResponse.set(mvc.perform(post("/api/cars")
                .with(jwt().jwt(token -> token.claim("email", email)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(requestFrom(details.asMaps().get(0)))))
                .andReturn().getResponse());
    }

    @Then("a vehicle is registered for driver {string} with the following details:")
    public void vehicleIsRegistered(String email, DataTable details) throws Exception {
        var response = lastResponse.get();
        assertEquals(HttpStatus.CREATED.value(), response.getStatus(), response.getContentAsString());
        assertEquals(carCountBefore + 1, cars.count(), "Exactly one vehicle should be saved");
        Map<String, String> expected = details.asMaps().get(0);
        Driver driver = driverOf(email);
        Car saved = cars.findByDriverId(driver.getId()).orElseThrow();
        assertEquals(expected.get("make"), saved.getMake());
        assertEquals(expected.get("model"), saved.getModel());
        assertEquals(expected.get("color"), saved.getColor());
        assertEquals(expected.get("license_plate"), saved.getLicensePlate());
        assertEquals(Integer.parseInt(expected.get("seat_capacity")), saved.getSeatCapacity());
        CarResponse returned = json.readValue(response.getContentAsString(), CarResponse.class);
        assertEquals(new CarResponse(saved.getId(), driver.getId(), saved.getMake(), saved.getModel(),
                saved.getColor(), saved.getLicensePlate(), saved.getSeatCapacity()), returned);
        assertEquals("/api/cars/" + saved.getId(), response.getHeader("Location"));
    }

    @Then("the {string} field error is {string}")
    public void fieldErrorIs(String field, String message) throws Exception {
        var response = lastResponse.get();
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), response.getContentAsString());
        ErrorResponse error = json.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(message, error.errors().get(field));
    }

    @Then("no vehicle is registered for driver {string}")
    public void noVehicleForDriver(String email) throws Exception {
        noNewVehicle();
        assertFalse(cars.existsByDriverId(driverOf(email).getId()));
    }

    @Then("no new vehicle is registered")
    public void noNewVehicle() throws Exception {
        var response = lastResponse.get();
        assertTrue(response.getStatus() >= 400 && response.getStatus() < 500,
                response.getContentAsString());
        assertEquals(carCountBefore, cars.count(), "No vehicle should be saved");
    }

    @Then("driver {string} still has the vehicle with license plate {string}")
    public void driverStillHasVehicle(String email, String plate) throws Exception {
        noNewVehicle();
        assertEquals(HttpStatus.CONFLICT.value(), lastResponse.get().getStatus());
        assertEquals(plate, cars.findByDriverId(driverOf(email).getId()).orElseThrow().getLicensePlate());
    }

    private Driver driverOf(String email) {
        return drivers.findByPersonEmail(email).orElseThrow();
    }

    /** Empty cells become null so validation can report a missing seat capacity. */
    private static CreateCarRequest requestFrom(Map<String, String> row) {
        String seats = row.get("seat_capacity");
        return new CreateCarRequest(row.get("make"), row.get("model"), row.get("color"),
                row.get("license_plate"), seats == null || seats.isBlank() ? null : Integer.valueOf(seats.trim()));
    }
}
