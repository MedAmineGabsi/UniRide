package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** License plate constraints for TID085 (issue #53), tested on CarValidator. */
class CarValidatorLicensePlateTest {
    private final CarValidator validator = new CarValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "---", " - - "})
    void missingPlateIsRejected(String plate) {
        assertEquals(Optional.of("License plate is required"), validator.checkLicensePlate(plate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC123", "abc-123", " ABC 123 ", "12345678901234567890"})
    void validPlateIsAccepted(String plate) {
        assertEquals(Optional.empty(), validator.checkLicensePlate(plate));
    }

    @Test
    void plateBeyondColumnLengthIsRejected() {
        assertEquals(Optional.of("License plate must be at most 20 characters"),
                validator.checkLicensePlate("123456789012345678901"));
    }

    @Test
    void surroundingSpacesDoNotCountTowardLength() {
        assertEquals(Optional.empty(), validator.checkLicensePlate("  12345678901234567890  "));
    }

    @Test
    void storedPlateIsTrimmedUppercaseWithSingleSpaces() {
        assertEquals("ABC 123", validator.normalizeLicensePlate("  abc \t 123  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc123", "ABC 123", "abc-123", " aBc - 123 "})
    void equivalentPlatesHaveTheSameUniquenessKey(String plate) {
        assertEquals("ABC123", validator.licensePlateKey(plate));
    }

    @Test
    void takenPlateIsRejected() {
        assertEquals(Optional.of("A car with this license plate is already registered"),
                validator.checkLicensePlateNotTaken(true));
        assertEquals(Optional.empty(), validator.checkLicensePlateNotTaken(false));
    }
}
