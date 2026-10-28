package ca.mcgill.ecse428.UniRide_Backend.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ca.mcgill.ecse428.UniRide_Backend.dto.ErrorResponse;
import ca.mcgill.ecse428.UniRide_Backend.dto.RegisterUserRequest;
import ca.mcgill.ecse428.UniRide_Backend.dto.RoleType;
import ca.mcgill.ecse428.UniRide_Backend.model.Person;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;
import ca.mcgill.ecse428.UniRide_Backend.service.PersonService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import tools.jackson.databind.json.JsonMapper;

public class AddNewUserStepDefinitions {

    private static final String REGISTER_URL = "/api/users/register";
    private static final String STRONG_PASSWORD = "Str0ngPass!";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private PersonService personService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private MockMvc mockMvc;
    private Draft draft;
    private MockHttpServletResponse response;
    private long personCountBeforeRequest;
    private int nextStudentId = 260900000;

    /** Mutable registration form, filled in by the Given steps and adjusted by the When steps. */
    private static class Draft {
        String firstName;
        String lastName;
        String studentId;
        String email;
        String phoneNumber = "514-555-0000";
        String password = STRONG_PASSWORD;
        String passwordConfirmation = STRONG_PASSWORD;
        List<RoleType> roles = List.of(RoleType.PASSENGER);
        String licenseNumber;

        RegisterUserRequest toRequest() {
            return new RegisterUserRequest(firstName, lastName, studentId, email, phoneNumber, password,
                    passwordConfirmation, roles, licenseNumber);
        }
    }

    @Before
    public void resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE seat_request, ride, person_role_assignment, driver, passenger, "
                + "person_role, person RESTART IDENTITY CASCADE");
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- Given ----------

    @Given("^a McGill student with first name (.+), last name (.+), student ID (.+), email (.+) and phone number (.+) is in good standing$")
    public void aMcGillStudentWithDetails(String firstName, String lastName, String studentId, String email,
            String phoneNumber) {
        draft = newDraft(firstName, lastName);
        draft.studentId = studentId;
        draft.email = email;
        draft.phoneNumber = phoneNumber;
    }

    @Given("^no UniRide user exists with email (.+)$")
    public void noUserExistsWithEmail(String email) {
        assertTrue(personRepository.findByEmail(email).isEmpty());
    }

    @Given("^(\\w+) (\\w+) uses email (.+) to request UniRide user access$")
    public void studentUsesEmail(String firstName, String lastName, String email) {
        draft = newDraft(firstName, lastName);
        draft.email = email;
    }

    @Given("^(\\w+) (\\w+) is already a registered user of UniRide with email (.+)$")
    public void studentIsAlreadyRegistered(String firstName, String lastName, String email) {
        Draft existing = newDraft(firstName, lastName);
        existing.email = email;
        personService.registerUser(existing.toRequest());
    }

    @Given("^a UniRide user exists with student ID (.+)$")
    public void userExistsWithStudentId(String studentId) {
        Draft existing = newDraft("Existing", "User");
        existing.studentId = studentId;
        personService.registerUser(existing.toRequest());
    }

    @Given("^a UniRide driver exists with license number (.+)$")
    public void driverExistsWithLicenseNumber(String licenseNumber) {
        Draft existing = newDraft("Existing", "Driver");
        existing.roles = List.of(RoleType.DRIVER);
        existing.licenseNumber = licenseNumber;
        personService.registerUser(existing.toRequest());
    }

    @Given("^student (\\w+) (\\w+) is a McGill student in good standing$")
    public void studentIsInGoodStanding(String firstName, String lastName) {
        draft = newDraft(firstName, lastName);
    }

    // ---------- When ----------

    @When("^the student requests to register as a UniRide user with password (.+) and role (.+) and license number ?(.*)$")
    public void theStudentRegistersWithRole(String password, String role, String licenseNumber) throws Exception {
        draft.password = password;
        draft.passwordConfirmation = password;
        draft.roles = parseRoles(role);
        draft.licenseNumber = licenseNumber.isBlank() ? null : licenseNumber;
        register();
    }

    @When("^student (\\w+) (\\w+) registers with email (.+)$")
    public void studentRegistersWithEmail(String firstName, String lastName, String email) throws Exception {
        draft = newDraft(firstName, lastName);
        draft.email = email;
        register();
    }

    @When("^(\\w+) (\\w+) requests to register as a UniRide user$")
    public void studentRequestsToRegister(String firstName, String lastName) throws Exception {
        register();
    }

    @When("^(\\w+) (\\w+) requests to register as a UniRide user with email (?!left blank$)(.+)$")
    public void studentRequestsToRegisterWithEmail(String firstName, String lastName, String email)
            throws Exception {
        if (draft == null) {
            draft = newDraft(firstName, lastName);
        }
        draft.email = email;
        register();
    }

    @When("^student (\\w+) (\\w+) with student ID (.+) requests to register as a UniRide user$")
    public void studentWithStudentIdRequestsToRegister(String firstName, String lastName, String studentId)
            throws Exception {
        draft = newDraft(firstName, lastName);
        draft.studentId = studentId;
        register();
    }

    @When("^student (\\w+) (\\w+) requests to register as a UniRide user with role Driver and license number (.+)$")
    public void studentRequestsToRegisterAsDriver(String firstName, String lastName, String licenseNumber)
            throws Exception {
        draft = newDraft(firstName, lastName);
        draft.roles = List.of(RoleType.DRIVER);
        draft.licenseNumber = licenseNumber;
        register();
    }

    @When("^(\\w+) (\\w+) requests to register as a UniRide user with (first name|last name|email|password|student ID|phone number) left blank$")
    public void studentRequestsToRegisterWithBlankField(String firstName, String lastName, String field)
            throws Exception {
        switch (field) {
            case "first name" -> draft.firstName = "";
            case "last name" -> draft.lastName = "";
            case "email" -> draft.email = "";
            case "password" -> draft.password = "";
            case "student ID" -> draft.studentId = "";
            case "phone number" -> draft.phoneNumber = "";
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        }
        register();
    }

    @When("^(\\w+) (\\w+) requests to register as a UniRide user with role Driver and license number left blank$")
    public void studentRequestsToRegisterAsDriverWithoutLicense(String firstName, String lastName)
            throws Exception {
        draft.roles = List.of(RoleType.DRIVER);
        draft.licenseNumber = "";
        register();
    }

    @When("^(\\w+) (\\w+) requests to register as a UniRide user with password (\\S+)$")
    public void studentRequestsToRegisterWithPassword(String firstName, String lastName, String password)
            throws Exception {
        draft.password = password;
        draft.passwordConfirmation = password;
        register();
    }

    @When("^(\\w+) (\\w+) requests to register with password (.+) and confirmation (.+)$")
    public void studentRequestsToRegisterWithConfirmation(String firstName, String lastName, String password,
            String confirmation) throws Exception {
        draft.password = password;
        draft.passwordConfirmation = confirmation;
        register();
    }

    // ---------- Then ----------

    @Then("^a new UniRide user is created with student ID (.+) and email (.+)$")
    public void userIsCreatedWithStudentIdAndEmail(String studentId, String email) throws Exception {
        assertEquals(HttpStatus.CREATED.value(), response.getStatus(), response.getContentAsString());
        Person person = personRepository.findByStudentID(studentId).orElseThrow();
        assertEquals(email, person.getEmail());
    }

    @Then("^the user is assigned the role (.+)$")
    public void userIsAssignedRole(String role) {
        Person person = personRepository.findByStudentID(draft.studentId).orElseThrow();
        List<RoleType> actual = personService.getRoles(person.getId());
        List<RoleType> expected = parseRoles(role);
        assertEquals(expected.size(), actual.size());
        assertTrue(actual.containsAll(expected));
    }

    @Then("^a new UniRide user is created with email (.+)$")
    public void userIsCreatedWithEmail(String email) throws Exception {
        assertEquals(HttpStatus.CREATED.value(), response.getStatus(), response.getContentAsString());
        assertTrue(personRepository.findByEmail(email).isPresent());
    }

    @Then("^an? \"(.*)\" message is issued$")
    public void messageIsIssued(String message) throws Exception {
        ErrorResponse error = jsonMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(message, error.error());
    }

    @Then("^no UniRide user is created with email (.+)$")
    public void noUserIsCreatedWithEmail(String email) {
        assertTrue(personRepository.findByEmail(email.toLowerCase(Locale.ROOT)).isEmpty());
        assertEquals(personCountBeforeRequest, personRepository.count());
    }

    @Then("^no (?:additional )?UniRide user is created$")
    public void noUserIsCreated() {
        assertFalse(response.getStatus() == HttpStatus.CREATED.value());
        assertEquals(personCountBeforeRequest, personRepository.count());
    }

    // ---------- Helpers ----------

    private void register() throws Exception {
        personCountBeforeRequest = personRepository.count();
        response = mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(draft.toRequest())))
                .andReturn()
                .getResponse();
    }

    /** A valid passenger registration with a unique student ID and an email derived from the name. */
    private Draft newDraft(String firstName, String lastName) {
        Draft d = new Draft();
        d.firstName = firstName;
        d.lastName = lastName;
        d.studentId = String.valueOf(nextStudentId++);
        d.email = (firstName + "." + lastName + "@mail.mcgill.ca").toLowerCase(Locale.ROOT);
        return d;
    }

    private static List<RoleType> parseRoles(String role) {
        List<RoleType> roles = new ArrayList<>();
        for (String part : role.split(" and ")) {
            roles.add(RoleType.valueOf(part.trim().toUpperCase(Locale.ROOT)));
        }
        return roles;
    }
}
