package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for TID034 (GitHub issue #40). */
class RideValidatorPricePerSeatTest {

    private final RideValidator validator = new RideValidator(Clock.systemUTC());

    @Test
    void missingPriceIsRejected() {
        assertEquals(Optional.of("Price per seat is required"), validator.checkPricePerSeat(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-0.01", "-10.00"})
    void nonPositivePriceIsRejected(String price) {
        assertEquals(Optional.of("Price per seat must be greater than 0"),
                validator.checkPricePerSeat(new BigDecimal(price)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.001", "1.001", "12.3450"})
    void priceWithMoreThanTwoSignificantDecimalPlacesIsRejected(String price) {
        assertEquals(Optional.of("Price per seat can have at most 2 decimal places"),
                validator.checkPricePerSeat(new BigDecimal(price)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1", "10.5", "12.34", "12.3400", "99999999.99"})
    void validPriceIsAccepted(String price) {
        assertEquals(Optional.empty(), validator.checkPricePerSeat(new BigDecimal(price)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100000000.00", "1000000000"})
    void priceAboveDatabaseMaximumIsRejected(String price) {
        assertEquals(Optional.of("Price per seat is too high"),
                validator.checkPricePerSeat(new BigDecimal(price)));
    }
}
