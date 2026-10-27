package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RideSearchValidatorTest {

    private static final ZoneId MONTREAL = ZoneId.of("America/Toronto");

    /** Noon on 2027-05-01 in Montréal, the current date used by the ID04 feature file. */
    private final RideSearchValidator validator = new RideSearchValidator(
            Clock.fixed(Instant.parse("2027-05-01T16:00:00Z"), MONTREAL));

    // TID055: departure_date format constraint unit tests
    @Test
    void impossibleCalendarDateIsRejected() {
        assertEquals(Optional.of(RideSearchValidator.INVALID_DEPARTURE_DATE),
                validator.checkDepartureDate("2027-02-30"));
        assertEquals(Optional.of(RideSearchValidator.INVALID_DEPARTURE_DATE),
                validator.checkDepartureDate("2027-13-01"));
    }

    @Test
    void wrongDateFormatIsRejected() {
        assertEquals(Optional.of(RideSearchValidator.INVALID_DEPARTURE_DATE),
                validator.checkDepartureDate("08/05/2027"));
        assertEquals(Optional.of(RideSearchValidator.INVALID_DEPARTURE_DATE),
                validator.checkDepartureDate("2027-5-8"));
        assertEquals(Optional.of(RideSearchValidator.INVALID_DEPARTURE_DATE),
                validator.checkDepartureDate("2027-05-08 09:00"));
    }

    @Test
    void nonDateTextIsRejected() {
        assertEquals(Optional.of(RideSearchValidator.INVALID_DEPARTURE_DATE),
                validator.checkDepartureDate("tomorrow"));
    }

    @Test
    void validFutureDateIsAccepted() {
        assertTrue(validator.checkDepartureDate("2027-05-08").isEmpty());
        assertTrue(validator.checkDepartureDate(" 2027-05-08 ").isEmpty());
        assertEquals(LocalDate.of(2027, 5, 8), validator.toLocalDate("2027-05-08"));
    }

    @Test
    void blankDateIsAcceptedAsNoFilter() {
        assertTrue(validator.checkDepartureDate(null).isEmpty());
        assertTrue(validator.checkDepartureDate("").isEmpty());
        assertTrue(validator.checkDepartureDate("   ").isEmpty());
        assertNull(validator.toLocalDate(""));
    }

    // TID056: departure_date past-date constraint unit tests
    @Test
    void pastDateIsRejected() {
        assertEquals(Optional.of(RideSearchValidator.PAST_DEPARTURE_DATE),
                validator.checkDepartureDate("2027-04-20"));
    }

    @Test
    void yesterdayIsRejected() {
        assertEquals(Optional.of(RideSearchValidator.PAST_DEPARTURE_DATE),
                validator.checkDepartureDate("2027-04-30"));
    }

    @Test
    void todayIsAccepted() {
        assertTrue(validator.checkDepartureDate("2027-05-01").isEmpty());
    }

    @Test
    void todayIsTheMontrealDateNotTheUtcDate() {
        // 02:00 UTC on May 1 is still April 30 in Montréal, so April 30 is not in the past yet.
        RideSearchValidator lateEvening = new RideSearchValidator(
                Clock.fixed(Instant.parse("2027-05-01T02:00:00Z"), MONTREAL));
        assertTrue(lateEvening.checkDepartureDate("2027-04-30").isEmpty());
    }
}
