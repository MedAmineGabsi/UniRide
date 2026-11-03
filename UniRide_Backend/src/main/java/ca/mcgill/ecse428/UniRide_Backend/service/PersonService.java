package ca.mcgill.ecse428.UniRide_Backend.service;

import java.util.List;

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

    private final PersonRepository personRepository;
    private final PersonRoleRepository personRoleRepository;
    private final DriverRepository driverRepository;
    private final PersonRoleAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonValidator personValidator;

    public PersonService(PersonRepository personRepository, PersonRoleRepository personRoleRepository,
            DriverRepository driverRepository, PersonRoleAssignmentRepository assignmentRepository,
            PasswordEncoder passwordEncoder, PersonValidator personValidator) {
        this.personRepository = personRepository;
        this.personRoleRepository = personRoleRepository;
        this.driverRepository = driverRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.personValidator = personValidator;
    }

    @Transactional
    public PersonResponse registerUser(RegisterUserRequest request) {
        String firstName = personValidator.requireField(request.firstName(), "first name");
        String lastName = personValidator.requireField(request.lastName(), "last name");
        String studentId = personValidator.requireField(request.studentId(), "student ID");
        String email = personValidator.validateEmail(request.email());
        String phoneNumber = personValidator.requireField(request.phoneNumber(), "phone number");
        personValidator.validatePassword(request.password(), request.passwordConfirmation());
        List<RoleType> roles = personValidator.validateRoles(request.roles());
        boolean isDriver = roles.contains(RoleType.DRIVER);
        String licenseNumber = isDriver ? personValidator.validateLicenseNumber(request.licenseNumber()) : null;

        if (personRepository.existsByEmail(email) || personRepository.existsByStudentID(studentId)) {
            throw new UniRideException(HttpStatus.CONFLICT, "Already registered");
        }
        if (isDriver && driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new UniRideException(HttpStatus.CONFLICT, "License number already in use");
        }

        Person person = personRepository.save(new Person(studentId, email, passwordEncoder.encode(request.password()),
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

    private static PersonResponse toResponse(Person person, List<RoleType> roles) {
        return new PersonResponse(person.getId(), person.getStudentID(), person.getEmail(), person.getFirstName(),
                person.getLastName(), person.getPhoneNumber(), roles);
    }
}
