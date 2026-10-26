package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import ca.mcgill.ecse428.UniRide_Backend.dto.PersonResponse;
import ca.mcgill.ecse428.UniRide_Backend.dto.RegisterUserRequest;
import ca.mcgill.ecse428.UniRide_Backend.dto.RoleType;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Person;
import ca.mcgill.ecse428.UniRide_Backend.model.PersonRole;
import ca.mcgill.ecse428.UniRide_Backend.repository.DriverRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRoleAssignmentRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.PersonRoleRepository;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    private static final String PASSWORD = "Str0ngPass!";

    @Mock
    private PersonRepository personRepository;
    @Mock
    private PersonRoleRepository personRoleRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private PersonRoleAssignmentRepository assignmentRepository;

    private PersonService personService;

    @BeforeEach
    void setUp() {
        personService = new PersonService(personRepository, personRoleRepository, driverRepository,
                assignmentRepository, new BCryptPasswordEncoder());
    }

    @Test
    void registersPassengerWithLowercaseEmailAndHashedPassword() {
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personRoleRepository.save(any(PersonRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonResponse response = personService.registerUser(
                request("Alice", "Tremblay", "260100001", "Alice.Tremblay@Mail.McGill.ca", "514-555-0101",
                        PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null));

        ArgumentCaptor<Person> saved = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(saved.capture());
        assertEquals("alice.tremblay@mail.mcgill.ca", saved.getValue().getEmail());
        assertNotEquals(PASSWORD, saved.getValue().getPassword());
        assertEquals("alice.tremblay@mail.mcgill.ca", response.email());
        assertEquals(List.of(RoleType.PASSENGER), response.roles());
    }

    // TID011
    @ParameterizedTest
    @ValueSource(strings = { "fred.smith@gmail.com", "fred.smith@concordia.ca", "fred.smith@mcgill.ca.fake.com" })
    void rejectsNonMcGillEmail(String email) {
        assertRejected(HttpStatus.FORBIDDEN, "Unauthorized request", withEmail(email));
    }

    // TID012
    @ParameterizedTest
    @ValueSource(strings = { "hana.ito", "hana.ito@", "@mail.mcgill.ca", "hana ito@mcgill.ca" })
    void rejectsInvalidEmailFormat(String email) {
        assertRejected(HttpStatus.BAD_REQUEST, "Invalid email format", withEmail(email));
    }

    // TID013
    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        when(personRepository.existsByEmail("bill.jones@mail.mcgill.ca")).thenReturn(true);

        assertRejected(HttpStatus.CONFLICT, "Already registered", withEmail("Bill.Jones@Mail.McGill.ca"));
    }

    // TID014
    @Test
    void rejectsDuplicateStudentId() {
        when(personRepository.existsByStudentID("260100001")).thenReturn(true);

        assertRejected(HttpStatus.CONFLICT, "Already registered",
                request("Gina", "Park", "260100001", "gina.park@mail.mcgill.ca", "514-555-0104",
                        PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null));
    }

    // TID015
    @ParameterizedTest
    @ValueSource(strings = { "abc", "password", "12345678", "Str0ngPass", "str0ngpass!" })
    void rejectsWeakPassword(String password) {
        assertRejected(HttpStatus.BAD_REQUEST, "Password does not meet requirements",
                request("Jade", "Wong", "260100005", "jade.wong@mail.mcgill.ca", "514-555-0105",
                        password, password, List.of(RoleType.PASSENGER), null));
    }

    // TID016
    @Test
    void rejectsMismatchedPasswordConfirmation() {
        assertRejected(HttpStatus.BAD_REQUEST, "Passwords do not match",
                request("Kai", "Lopez", "260100006", "kai.lopez@mail.mcgill.ca", "514-555-0106",
                        PASSWORD, "Str0ngPass?", List.of(RoleType.PASSENGER), null));
    }

    // TID017
    @Test
    void rejectsEachMissingRequiredField() {
        assertRejected(HttpStatus.BAD_REQUEST, "first name is required",
                request(" ", "Petrov", "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107",
                        PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "last name is required",
                request("Ivan", "", "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107",
                        PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "student ID is required",
                request("Ivan", "Petrov", null, "ivan.petrov@mail.mcgill.ca", "514-555-0107",
                        PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "email is required",
                request("Ivan", "Petrov", "260100007", "", "514-555-0107",
                        PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "phone number is required",
                request("Ivan", "Petrov", "260100007", "ivan.petrov@mail.mcgill.ca", "",
                        PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "password is required",
                request("Ivan", "Petrov", "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107",
                        "", PASSWORD, List.of(RoleType.PASSENGER), null));
        assertRejected(HttpStatus.BAD_REQUEST, "role is required",
                request("Ivan", "Petrov", "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107",
                        PASSWORD, PASSWORD, List.of(), null));
    }

    // TID018
    @Test
    void rejectsDriverWithoutLicenseNumber() {
        assertRejected(HttpStatus.BAD_REQUEST, "license number is required",
                request("Ivan", "Petrov", "260100007", "ivan.petrov@mail.mcgill.ca", "514-555-0107",
                        PASSWORD, PASSWORD, List.of(RoleType.DRIVER), " "));
    }

    // TID018
    @Test
    void rejectsDriverWithDuplicateLicenseNumber() {
        when(driverRepository.existsByLicenseNumber("D1234-567890")).thenReturn(true);

        assertRejected(HttpStatus.CONFLICT, "License number already in use",
                request("Omar", "Haddad", "260100008", "omar.haddad@mail.mcgill.ca", "514-555-0108",
                        PASSWORD, PASSWORD, List.of(RoleType.DRIVER), "D1234-567890"));
    }

    private void assertRejected(HttpStatus status, String message, RegisterUserRequest request) {
        UniRideException e = assertThrows(UniRideException.class, () -> personService.registerUser(request));
        assertEquals(message, e.getMessage());
        assertEquals(status, e.getStatus());
        verify(personRepository, never()).save(any(Person.class));
    }

    private static RegisterUserRequest withEmail(String email) {
        return request("Fred", "Smith", "260100009", email, "514-555-0109",
                PASSWORD, PASSWORD, List.of(RoleType.PASSENGER), null);
    }

    private static RegisterUserRequest request(String firstName, String lastName, String studentId, String email,
            String phoneNumber, String password, String confirmation, List<RoleType> roles, String licenseNumber) {
        return new RegisterUserRequest(firstName, lastName, studentId, email, phoneNumber, password, confirmation,
                roles, licenseNumber);
    }
}
