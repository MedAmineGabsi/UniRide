package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ca.mcgill.ecse428.UniRide_Backend.dto.PersonResponse;
import ca.mcgill.ecse428.UniRide_Backend.dto.RegisterUserRequest;
import ca.mcgill.ecse428.UniRide_Backend.dto.RoleType;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Driver;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Person;
import ca.mcgill.ecse428.UniRide_Backend.model.PersonRole;
import ca.mcgill.ecse428.UniRide_Backend.model.PersonRoleAssignment;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRoleAssignmentRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRoleRepository;

@ExtendWith(MockitoExtension.class)
public class PersonServiceTests {

    private static final String PASSWORD = "Str0ngPass!";

    @Mock
    private PersonRepository personRepository;
    @Mock
    private PersonRoleRepository personRoleRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private PersonRoleAssignmentRepository assignmentRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private PersonService personService;

    @BeforeEach
    public void setUp() {
        personService = new PersonService(personRepository, personRoleRepository, driverRepository,
                assignmentRepository, passwordEncoder, new PersonValidator());
    }


    //1. registerUser

    // registerUser -> passenger (success)
    @Test
    public void registerUser_whenValidPassenger_savesPersonAndPassengerRole() {
        stubSaves();

        PersonResponse response = personService.registerUser(passenger("alice.tremblay@mail.mcgill.ca"));

        ArgumentCaptor<Person> savedPerson = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(savedPerson.capture());
        assertEquals("260100001", savedPerson.getValue().getStudentID());
        assertEquals("alice.tremblay@mail.mcgill.ca", savedPerson.getValue().getEmail());
        assertEquals("Alice", savedPerson.getValue().getFirstName());
        assertEquals("Tremblay", savedPerson.getValue().getLastName());
        assertEquals("514-555-0101", savedPerson.getValue().getPhoneNumber());

        ArgumentCaptor<PersonRole> savedRole = ArgumentCaptor.forClass(PersonRole.class);
        verify(personRoleRepository).save(savedRole.capture());
        assertInstanceOf(Passenger.class, savedRole.getValue());
        verify(assignmentRepository).save(any(PersonRoleAssignment.class));

        assertEquals("alice.tremblay@mail.mcgill.ca", response.email());
        assertEquals(List.of(RoleType.PASSENGER), response.roles());
    }

    // registerUser -> driver (success)
    @Test
    public void registerUser_whenValidDriver_savesDriverRoleWithLicenseNumber() {
        stubSaves();

        PersonResponse response = personService.registerUser(
                request("260100002", "bob.chen@mail.mcgill.ca", List.of(RoleType.DRIVER), "D1234-567890-12"));

        ArgumentCaptor<PersonRole> savedRole = ArgumentCaptor.forClass(PersonRole.class);
        verify(personRoleRepository).save(savedRole.capture());
        Driver driver = assertInstanceOf(Driver.class, savedRole.getValue());
        assertEquals("D1234-567890-12", driver.getLicenseNumber());
        assertEquals(List.of(RoleType.DRIVER), response.roles());
    }

    // registerUser -> passenger and driver (success)
    @Test
    public void registerUser_whenPassengerAndDriver_savesBothRoles() {
        stubSaves();

        PersonResponse response = personService.registerUser(request("260100003", "carla.singh@mcgill.ca",
                List.of(RoleType.PASSENGER, RoleType.DRIVER), "S9876-543210-12"));

        ArgumentCaptor<PersonRole> savedRoles = ArgumentCaptor.forClass(PersonRole.class);
        verify(personRoleRepository, times(2)).save(savedRoles.capture());
        assertInstanceOf(Passenger.class, savedRoles.getAllValues().get(0));
        assertInstanceOf(Driver.class, savedRoles.getAllValues().get(1));
        verify(assignmentRepository, times(2)).save(any(PersonRoleAssignment.class));
        assertEquals(List.of(RoleType.PASSENGER, RoleType.DRIVER), response.roles());
    }

    // registerUser -> email is stored in lowercase (success)
    @Test
    public void registerUser_whenEmailHasUppercase_storesLowercase() {
        stubSaves();

        personService.registerUser(passenger("Dana.Roy@Mail.McGill.ca"));

        ArgumentCaptor<Person> savedPerson = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(savedPerson.capture());
        assertEquals("dana.roy@mail.mcgill.ca", savedPerson.getValue().getEmail());
    }

    // registerUser -> password is stored as a BCrypt hash (success)
    @Test
    public void registerUser_whenValid_storesHashedPassword() {
        stubSaves();

        personService.registerUser(passenger("alice.tremblay@mail.mcgill.ca"));

        ArgumentCaptor<Person> savedPerson = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(savedPerson.capture());
        assertNotEquals(PASSWORD, savedPerson.getValue().getPassword());
        assertTrue(passwordEncoder.matches(PASSWORD, savedPerson.getValue().getPassword()));
    }

    // registerUser -> license number is stored in uppercase with dashes (success)
    @Test
    public void registerUser_whenLicenseNumberTypedDifferently_storesNormalized() {
        stubSaves();

        personService.registerUser(
                request("260100002", "bob.chen@mail.mcgill.ca", List.of(RoleType.DRIVER), "d123456789012"));

        verify(driverRepository).existsByLicenseNumber("D1234-567890-12");
        ArgumentCaptor<PersonRole> savedRole = ArgumentCaptor.forClass(PersonRole.class);
        verify(personRoleRepository).save(savedRole.capture());
        assertEquals("D1234-567890-12", ((Driver) savedRole.getValue()).getLicenseNumber());
    }

    // registerUser -> passenger ignores the license number (success)
    @Test
    public void registerUser_whenPassengerSendsLicenseNumber_ignoresIt() {
        stubSaves();

        personService.registerUser(
                request("260100001", "alice.tremblay@mail.mcgill.ca", List.of(RoleType.PASSENGER), "D1234-567890-12"));

        verify(driverRepository, never()).existsByLicenseNumber(any());
    }

    // registerUser -> email already registered, checked in lowercase (failure)
    @Test
    public void registerUser_whenEmailAlreadyRegistered_throwsConflict() {
        when(personRepository.existsByEmail("bill.jones@mail.mcgill.ca")).thenReturn(true);

        assertRejected(HttpStatus.CONFLICT, "Already registered", passenger("Bill.Jones@Mail.McGill.ca"));
    }

    // registerUser -> student ID already registered (failure)
    @Test
    public void registerUser_whenStudentIdAlreadyRegistered_throwsConflict() {
        when(personRepository.existsByStudentID("260100001")).thenReturn(true);

        assertRejected(HttpStatus.CONFLICT, "Already registered", passenger("gina.park@mail.mcgill.ca"));
    }

    // registerUser -> license number already used by another driver (failure)
    @Test
    public void registerUser_whenLicenseNumberAlreadyUsed_throwsConflict() {
        when(driverRepository.existsByLicenseNumber("D1234-567890-12")).thenReturn(true);

        assertRejected(HttpStatus.CONFLICT, "License number already in use",
                request("260100008", "omar.haddad@mail.mcgill.ca", List.of(RoleType.DRIVER), "D1234-567890-12"));
    }

    // registerUser -> invalid field, nothing is checked or saved (failure)
    @Test
    public void registerUser_whenFieldInvalid_doesNotQueryOrSave() {
        assertRejected(HttpStatus.FORBIDDEN, "Unauthorized request", passenger("fred.smith@gmail.com"));
        assertRejected(HttpStatus.BAD_REQUEST, "License number must be in the Quebec format A1234-567890-12",
                request("260100001", "fred.smith@mail.mcgill.ca", List.of(RoleType.DRIVER), "D1234-567890"));
        assertRejected(HttpStatus.BAD_REQUEST, "license number is required",
                request("260100001", "fred.smith@mail.mcgill.ca", List.of(RoleType.DRIVER), " "));
        assertRejected(HttpStatus.BAD_REQUEST, "A user cannot have the same role twice",
                request("260100001", "fred.smith@mail.mcgill.ca", List.of(RoleType.DRIVER, RoleType.DRIVER),
                        "D1234-567890-12"));

        verify(personRepository, never()).existsByEmail(any());
        verify(personRepository, never()).existsByStudentID(any());
        verify(driverRepository, never()).existsByLicenseNumber(any());
    }

    // registerUser -> each missing field (failure)
    @Test
    public void registerUser_whenFieldMissing_throwsRequired() {
        assertRejected(HttpStatus.BAD_REQUEST, "first name is required", new RegisterUserRequest(" ", "Petrov",
                "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107", PASSWORD, PASSWORD,
                List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "last name is required", new RegisterUserRequest("Ivan", "",
                "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107", PASSWORD, PASSWORD,
                List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "student ID is required", new RegisterUserRequest("Ivan", "Petrov",
                null, "ivan.petrov@mail.mcgill.ca", "514-555-0107", PASSWORD, PASSWORD,
                List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "email is required", new RegisterUserRequest("Ivan", "Petrov",
                "260100007", "", "514-555-0107", PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "phone number is required", new RegisterUserRequest("Ivan",
                "Petrov", "260100007", "ivan.petrov@mail.mcgill.ca", "", PASSWORD, PASSWORD,
                List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "password is required", new RegisterUserRequest("Ivan", "Petrov",
                "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107", "", "", List.of(RoleType.PASSENGER),
                null));
        assertRejected(HttpStatus.BAD_REQUEST, "role is required", new RegisterUserRequest("Ivan", "Petrov",
                "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107", PASSWORD, PASSWORD, List.of(), null));
    }

    // registerUser -> weak password and mismatched confirmation (failure)
    @Test
    public void registerUser_whenPasswordInvalid_throwsBadRequest() {
        assertRejected(HttpStatus.BAD_REQUEST, "Password does not meet requirements", new RegisterUserRequest(
                "Jade", "Wong", "260100005", "jade.wong@mail.mcgill.ca", "514-555-0105", "password", "password",
                List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "Passwords do not match", new RegisterUserRequest("Kai", "Lopez",
                "260100006", "kai.lopez@mail.mcgill.ca", "514-555-0106", PASSWORD, "Str0ngPass?",
                List.of(RoleType.PASSENGER), null));
    }


    //2. getRoles

    // getRoles -> passenger and driver (success)
    @Test
    public void getRoles_whenPersonHasBothRoles_returnsBoth() {
        when(assignmentRepository.findRolesByPersonId(1)).thenReturn(List.of(new Passenger(), new Driver("D1")));

        assertEquals(List.of(RoleType.PASSENGER, RoleType.DRIVER), personService.getRoles(1));
    }

    // getRoles -> no roles (success)
    @Test
    public void getRoles_whenPersonHasNoRoles_returnsEmpty() {
        when(assignmentRepository.findRolesByPersonId(1)).thenReturn(List.of());

        assertEquals(List.of(), personService.getRoles(1));
    }


    private void stubSaves() {
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personRoleRepository.save(any(PersonRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertRejected(HttpStatus status, String message, RegisterUserRequest request) {
        UniRideException e = assertThrows(UniRideException.class, () -> personService.registerUser(request));
        assertEquals(status, e.getStatus());
        assertEquals(message, e.getMessage());
        verify(personRepository, never()).save(any(Person.class));
        verify(personRoleRepository, never()).save(any(PersonRole.class));
    }

    private static RegisterUserRequest passenger(String email) {
        return request("260100001", email, List.of(RoleType.PASSENGER), null);
    }

    private static RegisterUserRequest request(String studentId, String email, List<RoleType> roles,
            String licenseNumber) {
        return new RegisterUserRequest("Alice", "Tremblay", studentId, email, "514-555-0101", PASSWORD, PASSWORD,
                roles, licenseNumber);
    }
}
