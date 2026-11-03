package ca.mcgill.ecse428.UniRide_Backend.service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ca.mcgill.ecse428.UniRide_Backend.dto.RoleType;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;

/**
 * Field constraints for a new user (Feature ID01). Each {@code validate*} method throws a
 * {@link UniRideException} if the value is invalid, and otherwise returns the normalized value to store.
 * Uniqueness needs the database, so it is checked in {@link PersonService}.
 */
@Component
public class PersonValidator {

    public static final Pattern EMAIL_FORMAT = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public static final List<String> MCGILL_DOMAINS = List.of("mail.mcgill.ca", "mcgill.ca");
    public static final int MAX_EMAIL_LENGTH = 255;

    public static final int MIN_PASSWORD_LENGTH = 8;
    /** BCrypt only reads the first 72 bytes of a password. */
    public static final int MAX_PASSWORD_BYTES = 72;

    /** Québec (SAAQ) license: a letter then 12 digits, written A1234-567890-12. The dashes are optional. */
    public static final Pattern LICENSE_NUMBER_FORMAT = Pattern.compile("^([A-Z])(\\d{4})-?(\\d{6})-?(\\d{2})$");

    /** A user is a passenger, a driver, or both. */
    public static final int MAX_ROLES = 2;

    /** Required, valid format, on a McGill domain. Returns the email in lowercase. */
    public String validateEmail(String email) {
        String normalized = requireField(email, "email").toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_EMAIL_LENGTH || !EMAIL_FORMAT.matcher(normalized).matches()) {
            throw badRequest("Invalid email format");
        }
        String domain = normalized.substring(normalized.indexOf('@') + 1);
        if (!MCGILL_DOMAINS.contains(domain)) {
            throw new UniRideException(HttpStatus.FORBIDDEN, "Unauthorized request");
        }
        return normalized;
    }

    /**
     * Required, strong, and equal to the confirmation. Strong means 8+ characters with an uppercase letter,
     * a lowercase letter, a digit and a symbol. The password is not trimmed.
     */
    public void validatePassword(String password, String passwordConfirmation) {
        if (password == null || password.isBlank()) {
            throw badRequest("password is required");
        }
        if (!isStrongPassword(password)) {
            throw badRequest("Password does not meet requirements");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw badRequest("Password is too long");
        }
        if (!password.equals(passwordConfirmation)) {
            throw badRequest("Passwords do not match");
        }
    }

    /**
     * Required and in the Québec format. Returns it in uppercase with dashes (A1234-567890-12), so the same
     * license is found as a duplicate however it was typed.
     */
    public String validateLicenseNumber(String licenseNumber) {
        String trimmed = requireField(licenseNumber, "license number").toUpperCase(Locale.ROOT);
        Matcher matcher = LICENSE_NUMBER_FORMAT.matcher(trimmed);
        if (!matcher.matches()) {
            throw badRequest("License number must be in the Quebec format A1234-567890-12");
        }
        return matcher.group(1) + matcher.group(2) + "-" + matcher.group(3) + "-" + matcher.group(4);
    }

    /** At least 1 role, at most 2, and no role twice. */
    public List<RoleType> validateRoles(List<RoleType> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(Objects::isNull)) {
            throw badRequest("role is required");
        }
        if (new HashSet<>(roles).size() != roles.size()) {
            throw badRequest("A user cannot have the same role twice");
        }
        if (roles.size() > MAX_ROLES) {
            throw badRequest("A user can have at most " + MAX_ROLES + " roles");
        }
        return List.copyOf(roles);
    }

    /** Required. Returns the trimmed value. */
    public String requireField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw badRequest(fieldName + " is required");
        }
        return value.trim();
    }

    static boolean isStrongPassword(String password) {
        return password.length() >= MIN_PASSWORD_LENGTH
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
    }

    private static UniRideException badRequest(String message) {
        return new UniRideException(HttpStatus.BAD_REQUEST, message);
    }
}
