package ca.mcgill.ecse428.UniRide_Backend.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class PersonService {

    private static final Pattern EMAIL_FORMAT = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final List<String> MCGILL_DOMAINS = List.of("mail.mcgill.ca", "mcgill.ca");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final PersonRepository personRepository;
    private final PersonRoleRepository personRoleRepository;
    private final DriverRepository driverRepository;
    private final PersonRoleAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public PersonService(PersonRepository personRepository, PersonRoleRepository personRoleRepository,
            DriverRepository driverRepository, PersonRoleAssignmentRepository assignmentRepository,
            PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.personRoleRepository = personRoleRepository;
        this.driverRepository = driverRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PersonResponse registerUser(RegisterUserRequest request) {
        String firstName = requireField(request.firstName(), "first name");
        String lastName = requireField(request.lastName(), "last name");
        String studentId = requireField(request.studentId(), "student ID");
        String email = requireField(request.email(), "email").toLowerCase(Locale.ROOT);
        String phoneNumber = requireField(request.phoneNumber(), "phone number");
        String password = requireField(request.password(), "password");

        List<RoleType> roles = request.roles() == null ? List.of() : request.roles().stream().distinct().toList();
        if (roles.isEmpty()) {
            throw badRequest("role is required");
        }
        boolean isDriver = roles.contains(RoleType.DRIVER);
        String licenseNumber = isDriver ? requireField(request.licenseNumber(), "license number") : null;

        if (!EMAIL_FORMAT.matcher(email).matches()) {
            throw badRequest("Invalid email format");
        }
        if (!MCGILL_DOMAINS.contains(email.substring(email.indexOf('@') + 1))) {
            throw new UniRideException(HttpStatus.FORBIDDEN, "Unauthorized request");
        }
        if (!isStrongPassword(password)) {
            throw badRequest("Password does not meet requirements");
        }
        if (!password.equals(request.passwordConfirmation())) {
            throw badRequest("Passwords do not match");
        }

        if (personRepository.existsByEmail(email) || personRepository.existsByStudentID(studentId)) {
            throw new UniRideException(HttpStatus.CONFLICT, "Already registered");
        }
        if (isDriver && driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new UniRideException(HttpStatus.CONFLICT, "License number already in use");
        }

        Person person = personRepository.save(new Person(studentId, email, passwordEncoder.encode(password),
                firstName, lastName, phoneNumber));
        for (RoleType roleType : roles) {
            PersonRole role = roleType == RoleType.DRIVER ? new Driver(licenseNumber) : new Passenger();
            assignmentRepository.save(new PersonRoleAssignment(person, personRoleRepository.save(role)));
        }

        return toResponse(person, roles);
    }

    @Transactional(readOnly = true)
    public List<RoleType> getRoles(Integer personId) {
        return assignmentRepository.findRolesByPersonId(personId).stream()
                .map(role -> role instanceof Driver ? RoleType.DRIVER : RoleType.PASSENGER)
                .toList();
    }

    /** At least 8 characters with an uppercase letter, a lowercase letter, a digit and a symbol. */
    static boolean isStrongPassword(String password) {
        return password.length() >= MIN_PASSWORD_LENGTH
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
    }

    private static String requireField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw badRequest(fieldName + " is required");
        }
        return value.trim();
    }

    private static UniRideException badRequest(String message) {
        return new UniRideException(HttpStatus.BAD_REQUEST, message);
    }

    private static PersonResponse toResponse(Person person, List<RoleType> roles) {
        return new PersonResponse(person.getId(), person.getStudentID(), person.getEmail(), person.getFirstName(),
                person.getLastName(), person.getPhoneNumber(), roles);
    }
}
