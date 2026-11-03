package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for TID036 (GitHub issue #42). */
class RideDurationValidatorTest {

    private final RideValidator validator = new RideValidator(Clock.systemUTC());

    @Test
    void missingDurationIsRejected() {
        assertEquals(Optional.of("Duration is required"), validator.checkDuration(null));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -60, -1, 0})
    void nonPositiveDurationIsRejected(int durationMinutes) {
        assertEquals(Optional.of("Duration must be greater than 0 minutes"),
                validator.checkDuration(durationMinutes));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 60, Integer.MAX_VALUE})
    void positiveDurationIsAccepted(int durationMinutes) {
        assertEquals(Optional.empty(), validator.checkDuration(durationMinutes));
    }
}
