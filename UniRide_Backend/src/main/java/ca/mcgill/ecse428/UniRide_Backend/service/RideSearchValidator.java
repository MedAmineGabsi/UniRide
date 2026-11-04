package ca.mcgill.ecse428.UniRide_Backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Search criteria constraints (Feature ID04). Each {@code check*} method tests one constraint and
 * returns the error message if it is broken, or {@code Optional.empty()} if the value is valid.
 */
@Component
public class RideSearchValidator {

    /** Format used in the feature file and expected from the search form. */
    public static final DateTimeFormatter DEPARTURE_DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    public static final String INVALID_DEPARTURE_DATE = "Invalid departure date";
    public static final String PAST_DEPARTURE_DATE = "Departure date cannot be in the past";

    private final Clock clock;

    public RideSearchValidator(Clock clock) {
        this.clock = clock;
    }

    // ---------------------------------------------------------------- departure_date

    /**
     * Optional (blank means no date filter); otherwise a real calendar date in yyyy-MM-dd format,
     * today or later.
     */
    public Optional<String> checkDepartureDate(String departureDate) {
        if (departureDate == null || departureDate.isBlank()) {
            return Optional.empty();
        }
        LocalDate date;
        try {
            date = LocalDate.parse(departureDate.trim(), DEPARTURE_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return Optional.of(INVALID_DEPARTURE_DATE);
        }
        if (date.isBefore(LocalDate.now(clock))) {
            return Optional.of(PAST_DEPARTURE_DATE);
        }
        return Optional.empty();
    }

    /**
     * Converts a departure date that passed {@link #checkDepartureDate(String)}.
     *
     * @return the date, or null if no date was given
     */
    public LocalDate toLocalDate(String departureDate) {
        if (departureDate == null || departureDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(departureDate.trim(), DEPARTURE_DATE_FORMAT);
    }
}
