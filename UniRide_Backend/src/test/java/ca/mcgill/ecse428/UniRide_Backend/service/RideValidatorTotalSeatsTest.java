package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ca.mcgill.ecse428.UniRide_Backend.dto.CreateRideRequest;

/** Unit tests for the total_seats constraint (TID033, GitHub issue #39). */
class RideValidatorTotalSeatsTest {

    /** 10:00 in Montréal on 2026-11-03, so the departure used below is always in the future. */
    private final RideValidator validator = new RideValidator(Clock.fixed(
            Instant.parse("2026-11-03T15:00:00Z"), ZoneId.of("America/Toronto")));

    private static final String TOO_MANY_FOR_FOUR_SEATS =
            "Number of seats cannot be more than the 4 passenger seats in your car";

    @Test
    void missingSeatsAreRejected() {
        assertEquals(Optional.of("Number of seats is required"), validator.checkTotalSeats(null, null));
        assertEquals(Optional.of("Number of seats is required"), validator.checkTotalSeats(null, 4));
    }

    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -1, 0 })
    void nonPositiveSeatsAreRejected(int totalSeats) {
        assertEquals(Optional.of("Number of seats must be greater than 0"),
                validator.checkTotalSeats(totalSeats, null));
        assertEquals(Optional.of("Number of seats must be greater than 0"),
                validator.checkTotalSeats(totalSeats, 4));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 3, 4 })
    void seatsUpToTheCarsPassengerSeatsAreAccepted(int totalSeats) {
        assertEquals(Optional.empty(), validator.checkTotalSeats(totalSeats, 4));
    }

    @ParameterizedTest
    @ValueSource(ints = { 5, 100, Integer.MAX_VALUE })
    void moreSeatsThanTheCarHasAreRejected(int totalSeats) {
        assertEquals(Optional.of(TOO_MANY_FOR_FOUR_SEATS), validator.checkTotalSeats(totalSeats, 4));
    }

    @Test
    void oneSeatCarAllowsExactlyOneSeat() {
        assertEquals(Optional.empty(), validator.checkTotalSeats(1, 1));
        assertEquals(Optional.of("Number of seats cannot be more than the 1 passenger seats in your car"),
                validator.checkTotalSeats(2, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 8, Integer.MAX_VALUE })
    void withoutACarOnlyPositiveSeatsAreRequired(int totalSeats) {
        assertEquals(Optional.empty(), validator.checkTotalSeats(totalSeats, null));
    }

    @Test
    void tooManySeatsIsReportedAgainstTheTotalSeatsField() {
        assertEquals(Map.of("totalSeats", TOO_MANY_FOR_FOUR_SEATS), validator.validate(request(5), 4));
        assertEquals(Map.of(), validator.validate(request(4), 4));
    }

    private static CreateRideRequest request(int totalSeats) {
        return new CreateRideRequest("252 Laurier Ave E, Ottawa ON, K1P 5G4",
                "220 Yonge Street, Toronto ON, M5T 1E3", "2026-11-10 09:00", 300, totalSeats,
                new BigDecimal("15.00"));
    }
}
