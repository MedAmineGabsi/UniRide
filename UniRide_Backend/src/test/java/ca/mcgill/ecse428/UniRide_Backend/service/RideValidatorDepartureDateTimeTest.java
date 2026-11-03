package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for TID035 (GitHub issue #41). */
class RideValidatorDepartureDateTimeTest {

    // 10:00 in Toronto; the earliest permitted departure is 11:00.
    private final RideValidator validator = new RideValidator(Clock.fixed(
            Instant.parse("2026-11-03T15:00:00Z"), ZoneId.of("America/Toronto")));

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void missingDepartureIsRejected(String departure) {
        assertEquals(Optional.of("Departure date and time is required"),
                validator.checkDepartureDateTime(departure));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-date", "2026-11-03", "03/11/2026 11:00",
            "2026-11-03 11:00:00", "2026-11-03 24:00", "2026-11-03 11:60",
            "2026-13-03 11:00", "2026-11-31 11:00", "2027-02-29 11:00"})
    void invalidFormatOrImpossibleDateIsRejected(String departure) {
        assertEquals(Optional.of(
                "Departure date and time must be a valid date in the format yyyy-MM-dd HH:mm"),
                validator.checkDepartureDateTime(departure));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-02 12:00", "2026-11-03 10:00",
            "2026-11-03 10:59", "2026-11-03T10:59"})
    void departureBeforeOneHourBoundaryIsRejected(String departure) {
        assertEquals(Optional.of("Departure must be at least 1 hour from now"),
                validator.checkDepartureDateTime(departure));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-03 11:00", "2026-11-03T11:00",
            "2026-11-03 11:01", "2026-11-04 00:00", " 2026-11-03 11:00 ",
            "2028-02-29 11:00"})
    void validDepartureAtOrAfterOneHourBoundaryIsAccepted(String departure) {
        assertEquals(Optional.empty(), validator.checkDepartureDateTime(departure));
    }

    @Test
    void oneHourBoundaryCanCrossMidnight() {
        RideValidator lateNightValidator = new RideValidator(Clock.fixed(
                Instant.parse("2026-11-04T04:30:00Z"), ZoneId.of("America/Toronto")));

        assertEquals(Optional.of("Departure must be at least 1 hour from now"),
                lateNightValidator.checkDepartureDateTime("2026-11-04 00:29"));
        assertEquals(Optional.empty(),
                lateNightValidator.checkDepartureDateTime("2026-11-04 00:30"));
    }
}
