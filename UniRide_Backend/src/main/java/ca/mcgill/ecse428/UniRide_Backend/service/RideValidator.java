package ca.mcgill.ecse428.UniRide_Backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateRideRequest;

/**
 * Ride constraints (Feature ID03). Each {@code check*} method tests one constraint and
 * returns the error message if it is broken, or {@code Optional.empty()} if the value is valid.
 */
@Component
public class RideValidator {

    /** Format used in the feature file and returned by the API. */
    public static final DateTimeFormatter DEPARTURE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Same format with a 'T' separator, as sent by most date pickers / JSON clients. */
    private static final DateTimeFormatter DEPARTURE_FORMAT_ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final List<DateTimeFormatter> ACCEPTED_DEPARTURE_FORMATS = List.of(DEPARTURE_FORMAT,
            DEPARTURE_FORMAT_ISO);

    /** A ride must depart at least this long after it is created. */
    public static final Duration MIN_TIME_BEFORE_DEPARTURE = Duration.ofHours(1);

    public static final int MIN_ADDRESS_LENGTH = 5;
    public static final int MAX_ADDRESS_LENGTH = 255;

    /** Largest value that fits in NUMERIC(10, 2). */
    public static final BigDecimal MAX_PRICE_PER_SEAT = new BigDecimal("99999999.99");

    private final Clock clock;

    public RideValidator(Clock clock) {
        this.clock = clock;
    }

    /**
     * Runs every constraint.
     *
     * @param carSeats passenger seats in the driver's car, or null if unknown (then only the
     *                 positive-value rule applies to totalSeats)
     * @return field name -> error message, in form order; empty if the ride is valid
     */
    public Map<String, String> validate(CreateRideRequest request, Integer carSeats) {
        Map<String, String> errors = new LinkedHashMap<>();

        checkAddress("Origin", request.origin()).ifPresent(e -> errors.put("origin", e));
        checkAddress("Destination", request.destination()).ifPresent(e -> errors.put("destination", e));
        if (errors.isEmpty()) {
            checkDifferentLocations(request.origin(), request.destination())
                    .ifPresent(e -> errors.put("destination", e));
        }
        checkDepartureDateTime(request.departureDateTime()).ifPresent(e -> errors.put("departureDateTime", e));
        checkDuration(request.durationMinutes()).ifPresent(e -> errors.put("durationMinutes", e));
        checkTotalSeats(request.totalSeats(), carSeats).ifPresent(e -> errors.put("totalSeats", e));
        checkPricePerSeat(request.pricePerSeat()).ifPresent(e -> errors.put("pricePerSeat", e));

        return errors;
    }

    // ---------------------------------------------------------------- total_seats

    /** Positive, and no more than the passenger seats in the driver's car (when known). */
    public Optional<String> checkTotalSeats(Integer totalSeats, Integer carSeats) {
        if (totalSeats == null) {
            return Optional.of("Number of seats is required");
        }
        if (totalSeats <= 0) {
            return Optional.of("Number of seats must be greater than 0");
        }
        if (carSeats != null && totalSeats > carSeats) {
            return Optional.of("Number of seats cannot be more than the " + carSeats + " passenger seats in your car");
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- price_per_seat

    /** Positive, at most 2 decimal places. */
    public Optional<String> checkPricePerSeat(BigDecimal pricePerSeat) {
        if (pricePerSeat == null) {
            return Optional.of("Price per seat is required");
        }
        if (pricePerSeat.signum() <= 0) {
            return Optional.of("Price per seat must be greater than 0");
        }
        if (pricePerSeat.stripTrailingZeros().scale() > 2) {
            return Optional.of("Price per seat can have at most 2 decimal places");
        }
        if (pricePerSeat.compareTo(MAX_PRICE_PER_SEAT) > 0) {
            return Optional.of("Price per seat is too high");
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- departure_date_time

    /** Required, format yyyy-MM-dd HH:mm, a real date, at least 1 hour from now. */
    public Optional<String> checkDepartureDateTime(String departureDateTime) {
        if (isBlank(departureDateTime)) {
            return Optional.of("Departure date and time is required");
        }
        Optional<LocalDateTime> parsed = parseDepartureDateTime(departureDateTime);
        if (parsed.isEmpty()) {
            return Optional.of("Departure date and time must be a valid date in the format yyyy-MM-dd HH:mm");
        }
        LocalDateTime earliestAllowed = LocalDateTime.now(clock).plus(MIN_TIME_BEFORE_DEPARTURE);
        if (parsed.get().isBefore(earliestAllowed)) {
            return Optional.of("Departure must be at least 1 hour from now");
        }
        return Optional.empty();
    }

    /** Parses "yyyy-MM-dd HH:mm" or "yyyy-MM-ddTHH:mm"; empty if blank or not a real date/time. */
    public Optional<LocalDateTime> parseDepartureDateTime(String departureDateTime) {
        if (isBlank(departureDateTime)) {
            return Optional.empty();
        }
        String value = departureDateTime.trim();
        for (DateTimeFormatter format : ACCEPTED_DEPARTURE_FORMATS) {
            try {
                return Optional.of(LocalDateTime.parse(value, format));
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- duration

    /** Required, positive number of minutes. */
    public Optional<String> checkDuration(Integer durationMinutes) {
        if (durationMinutes == null) {
            return Optional.of("Duration is required");
        }
        if (durationMinutes <= 0) {
            return Optional.of("Duration must be greater than 0 minutes");
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- origin / destination

    /**
     * Required, and must look like a street address: at least {@value #MIN_ADDRESS_LENGTH}
     * characters with both letters and a number (rejects "asdfghjkl" and "12345").
     *
     * @param label "Origin" or "Destination", used in the message
     */
    public Optional<String> checkAddress(String label, String address) {
        if (isBlank(address)) {
            return Optional.of(label + " is required");
        }
        String value = address.trim();
        if (value.length() > MAX_ADDRESS_LENGTH) {
            return Optional.of(label + " must be at most " + MAX_ADDRESS_LENGTH + " characters");
        }
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        if (value.length() < MIN_ADDRESS_LENGTH || !hasLetter || !hasDigit) {
            return Optional.of(label + " must be a valid street address");
        }
        return Optional.empty();
    }

    /** Origin and destination must not be the same place (ignoring case and extra spaces). */
    public Optional<String> checkDifferentLocations(String origin, String destination) {
        if (isBlank(origin) || isBlank(destination)) {
            return Optional.empty(); // reported by checkAddress
        }
        if (normalize(origin).equals(normalize(destination))) {
            return Optional.of("Origin and destination must be different");
        }
        return Optional.empty();
    }

    private static String normalize(String address) {
        return address.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
