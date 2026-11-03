package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import ca.mcgill.ecse428.UniRide_Backend.dto.RoleType;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;

public class PersonValidatorTests {

    private static final String PASSWORD = "Str0ngPass!";

    private final PersonValidator personValidator = new PersonValidator();


    //1. validateEmail (TID005 / TID010)

    // validateEmail -> mail.mcgill.ca and mcgill.ca (success)
    @ParameterizedTest
    @ValueSource(strings = { "alice.tremblay@mail.mcgill.ca", "carla.singh@mcgill.ca" })
    public void validateEmail_whenMcGillDomain_returnsEmail(String email) {
        assertEquals(email, personValidator.validateEmail(email));
    }

    // validateEmail -> mixed case is stored in lowercase (success)
    @Test
    public void validateEmail_whenMixedCase_returnsLowercase() {
        assertEquals("dana.roy@mail.mcgill.ca", personValidator.validateEmail("Dana.Roy@Mail.McGill.ca"));
    }

    // validateEmail -> surrounding spaces are removed (success)
    @Test
    public void validateEmail_whenSurroundedBySpaces_returnsTrimmed() {
        assertEquals("dana.roy@mail.mcgill.ca", personValidator.validateEmail("  dana.roy@mail.mcgill.ca "));
    }

    // validateEmail -> missing (failure)
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    public void validateEmail_whenMissing_throwsRequired(String email) {
        assertRejected(HttpStatus.BAD_REQUEST, "email is required", () -> personValidator.validateEmail(email));
    }

    // validateEmail -> bad format (failure)
    @ParameterizedTest
    @ValueSource(strings = { "hana.ito", "hana.ito@", "@mail.mcgill.ca", "hana ito@mcgill.ca", "hana.ito@mcgill",
            "hana@ito@mcgill.ca" })
    public void validateEmail_whenInvalidFormat_throwsInvalidFormat(String email) {
        assertRejected(HttpStatus.BAD_REQUEST, "Invalid email format", () -> personValidator.validateEmail(email));
    }

    // validateEmail -> longer than the 255 character column (failure)
    @Test
    public void validateEmail_whenTooLong_throwsInvalidFormat() {
        String email = "a".repeat(250) + "@mcgill.ca";

        assertRejected(HttpStatus.BAD_REQUEST, "Invalid email format", () -> personValidator.validateEmail(email));
    }

    // validateEmail -> not a McGill domain (failure)
    @ParameterizedTest
    @ValueSource(strings = { "fred.smith@gmail.com", "fred.smith@concordia.ca", "fred.smith@mcgill.ca.fake.com",
            "fred.smith@fakemcgill.ca", "fred.smith@students.mcgill.ca" })
    public void validateEmail_whenNotMcGillDomain_throwsUnauthorized(String email) {
        assertRejected(HttpStatus.FORBIDDEN, "Unauthorized request", () -> personValidator.validateEmail(email));
    }


    //2. validatePassword (TID007 / TID015)

    // validatePassword -> strong and confirmed (success)
    @ParameterizedTest
    @ValueSource(strings = { PASSWORD, "Abcdef1!", "C0mplex pass phrase", "Pässw0rd!" })
    public void validatePassword_whenStrongAndConfirmed_doesNotThrow(String password) {
        assertDoesNotThrow(() -> personValidator.validatePassword(password, password));
    }

    // validatePassword -> missing (failure)
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "        " })
    public void validatePassword_whenMissing_throwsRequired(String password) {
        assertRejected(HttpStatus.BAD_REQUEST, "password is required",
                () -> personValidator.validatePassword(password, password));
    }

    // validatePassword -> too short, or missing an uppercase, lowercase, digit or symbol (failure)
    @ParameterizedTest
    @ValueSource(strings = { "abc", "Ab1!", "Abcde1!", "password", "12345678", "Str0ngPass", "str0ngpass!",
            "STR0NGPASS!", "StrongPass!" })
    public void validatePassword_whenWeak_throwsRequirements(String password) {
        assertRejected(HttpStatus.BAD_REQUEST, "Password does not meet requirements",
                () -> personValidator.validatePassword(password, password));
    }

    // validatePassword -> more than 72 bytes, which BCrypt cannot hash (failure)
    @Test
    public void validatePassword_whenLongerThan72Bytes_throwsTooLong() {
        String password = PASSWORD + "a".repeat(62);

        assertRejected(HttpStatus.BAD_REQUEST, "Password is too long",
                () -> personValidator.validatePassword(password, password));
    }

    // validatePassword -> exactly 72 bytes (success)
    @Test
    public void validatePassword_whenExactly72Bytes_doesNotThrow() {
        String password = PASSWORD + "a".repeat(61);

        assertDoesNotThrow(() -> personValidator.validatePassword(password, password));
    }

    // validatePassword -> confirmation differs (failure)
    @ParameterizedTest
    @ValueSource(strings = { "Str0ngPass?", "str0ngpass!", "Str0ngPass! " })
    public void validatePassword_whenConfirmationDiffers_throwsMismatch(String confirmation) {
        assertRejected(HttpStatus.BAD_REQUEST, "Passwords do not match",
                () -> personValidator.validatePassword(PASSWORD, confirmation));
    }

    // validatePassword -> confirmation missing (failure)
    @Test
    public void validatePassword_whenConfirmationMissing_throwsMismatch() {
        assertRejected(HttpStatus.BAD_REQUEST, "Passwords do not match",
                () -> personValidator.validatePassword(PASSWORD, null));
    }

    // validatePassword -> spaces are kept, so the same padded value still matches (success)
    @Test
    public void validatePassword_whenPaddedWithSpaces_isNotTrimmed() {
        assertDoesNotThrow(() -> personValidator.validatePassword(" " + PASSWORD + " ", " " + PASSWORD + " "));
    }


    //3. validateLicenseNumber (TID009)

    // validateLicenseNumber -> Quebec format (success)
    @Test
    public void validateLicenseNumber_whenQuebecFormat_returnsLicenseNumber() {
        assertEquals("D1234-567890-12", personValidator.validateLicenseNumber("D1234-567890-12"));
    }

    // validateLicenseNumber -> lowercase, spaces or no dashes are stored the same way (success)
    @ParameterizedTest
    @ValueSource(strings = { " d1234-567890-12 ", "D123456789012", "d123456789012" })
    public void validateLicenseNumber_whenTypedDifferently_returnsNormalized(String licenseNumber) {
        assertEquals("D1234-567890-12", personValidator.validateLicenseNumber(licenseNumber));
    }

    // validateLicenseNumber -> missing (failure)
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    public void validateLicenseNumber_whenMissing_throwsRequired(String licenseNumber) {
        assertRejected(HttpStatus.BAD_REQUEST, "license number is required",
                () -> personValidator.validateLicenseNumber(licenseNumber));
    }

    // validateLicenseNumber -> not Quebec format (failure)
    @ParameterizedTest
    @ValueSource(strings = { "D1234-567890", "1234-567890-12", "DD234-567890-12", "D1234-567890-123",
            "D1234-56789A-12", "D1234 567890 12", "D-1234-567890-12" })
    public void validateLicenseNumber_whenNotQuebecFormat_throwsFormat(String licenseNumber) {
        assertRejected(HttpStatus.BAD_REQUEST, "License number must be in the Quebec format A1234-567890-12",
                () -> personValidator.validateLicenseNumber(licenseNumber));
    }


    //4. validateRoles (TID002)

    // validateRoles -> one role or both roles (success)
    @Test
    public void validateRoles_whenOneOrTwoDifferentRoles_returnsRoles() {
        assertEquals(List.of(RoleType.PASSENGER), personValidator.validateRoles(List.of(RoleType.PASSENGER)));
        assertEquals(List.of(RoleType.DRIVER), personValidator.validateRoles(List.of(RoleType.DRIVER)));
        assertEquals(List.of(RoleType.PASSENGER, RoleType.DRIVER),
                personValidator.validateRoles(List.of(RoleType.PASSENGER, RoleType.DRIVER)));
    }

    // validateRoles -> missing (failure)
    @Test
    public void validateRoles_whenMissing_throwsRequired() {
        assertRejected(HttpStatus.BAD_REQUEST, "role is required", () -> personValidator.validateRoles(null));
        assertRejected(HttpStatus.BAD_REQUEST, "role is required", () -> personValidator.validateRoles(List.of()));
        assertRejected(HttpStatus.BAD_REQUEST, "role is required",
                () -> personValidator.validateRoles(Arrays.asList(RoleType.PASSENGER, null)));
    }

    // validateRoles -> same role twice (failure)
    @Test
    public void validateRoles_whenSameRoleTwice_throwsDuplicate() {
        assertRejected(HttpStatus.BAD_REQUEST, "A user cannot have the same role twice",
                () -> personValidator.validateRoles(List.of(RoleType.DRIVER, RoleType.DRIVER)));
        assertRejected(HttpStatus.BAD_REQUEST, "A user cannot have the same role twice",
                () -> personValidator.validateRoles(List.of(RoleType.PASSENGER, RoleType.DRIVER, RoleType.PASSENGER)));
    }

    // validateRoles -> returned list cannot be changed by the caller afterwards (success)
    @Test
    public void validateRoles_returnsCopy() {
        List<RoleType> roles = new ArrayList<>(List.of(RoleType.PASSENGER));

        List<RoleType> result = personValidator.validateRoles(roles);
        roles.add(RoleType.DRIVER);

        assertEquals(List.of(RoleType.PASSENGER), result);
    }


    //5. requireField

    // requireField -> present (success)
    @Test
    public void requireField_whenPresent_returnsTrimmed() {
        assertEquals("Alice", personValidator.requireField(" Alice ", "first name"));
    }

    // requireField -> missing (failure)
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "\t" })
    public void requireField_whenMissing_throwsRequired(String value) {
        assertRejected(HttpStatus.BAD_REQUEST, "first name is required",
                () -> personValidator.requireField(value, "first name"));
    }


    private static void assertRejected(HttpStatus status, String message, Executable executable) {
        UniRideException e = assertThrows(UniRideException.class, executable);
        assertEquals(status, e.getStatus());
        assertEquals(message, e.getMessage());
    }
}
