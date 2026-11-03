package ca.mcgill.ecse428.UniRide_Backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ca.mcgill.ecse428.UniRide_Backend.dto.CreateCarRequest;

/** Car field constraints for TID086 (issue #54). */
class CarValidatorTest {
    private final CarValidator validator = new CarValidator();

    static Stream<Arguments> invalidTextFields() {
        return Stream.of("make", "model", "color").flatMap(field -> {
            int limit = field.equals("color") ? 30 : 50;
            String label = Character.toUpperCase(field.charAt(0)) + field.substring(1);
            return Stream.of(
                    Arguments.of(field, null, label + " is required"),
                    Arguments.of(field, "", label + " is required"),
                    Arguments.of(field, " \t ", label + " is required"),
                    Arguments.of(field, "a".repeat(limit + 1), label + " must be at most " + limit + " characters"));
        });
    }

    @ParameterizedTest
    @MethodSource("invalidTextFields")
    void invalidTextIsReportedAgainstItsField(String field, String value, String message) {
        CreateCarRequest request = new CreateCarRequest(
                field.equals("make") ? value : "Honda",
                field.equals("model") ? value : "Civic",
                field.equals("color") ? value : "Blue", "ABC123", 5);
        assertEquals(Map.of(field, message), validator.validate(request));
    }

    @Test
    void maximumTextLengthsAreAcceptedAfterTrimming() {
        assertEquals(Map.of(), validator.validate(new CreateCarRequest(
                " " + "a".repeat(50) + " ", "b".repeat(50), "c".repeat(30), "ABC123", 2)));
    }

    @Test
    void missingSeatCapacityIsRejected() {
        assertEquals(Optional.of("Seat capacity is required"), validator.checkSeatCapacity(null));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 1})
    void fewerThanTwoSeatsIsRejected(int seats) {
        assertEquals(Optional.of("Seat capacity must be greater than 1"), validator.checkSeatCapacity(seats));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5, Integer.MAX_VALUE})
    void atLeastTwoSeatsIsAccepted(int seats) {
        assertEquals(Optional.empty(), validator.checkSeatCapacity(seats));
    }

    @Test
    void allMissingFieldsAreReportedTogether() {
        assertEquals(Map.of("make", "Make is required", "model", "Model is required",
                "color", "Color is required", "licensePlate", "License plate is required",
                "seatCapacity", "Seat capacity is required"),
                validator.validate(new CreateCarRequest(null, null, null, null, null)));
    }
}
