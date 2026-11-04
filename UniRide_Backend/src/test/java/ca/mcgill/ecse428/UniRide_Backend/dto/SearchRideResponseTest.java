
package ca.mcgill.ecse428.UniRide_Backend.dto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import ca.mcgill.ecse428.UniRide_Backend.model.Person;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;

class SearchRideResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void searchResultContainsCorrectRideInformation() {

        Ride ride = mock(Ride.class);
        Person driver = mock(Person.class);

        when(ride.getId()).thenReturn(15);
        when(ride.getOrigin()).thenReturn("Montreal");
        when(ride.getDestination()).thenReturn("Ottawa");
        when(ride.getDepartureDateTime()).thenReturn(
                LocalDateTime.of(2026, 11, 15, 9, 0));
        when(ride.getPricePerSeat()).thenReturn(
                new BigDecimal("20.00"));
        when(ride.getTotalSeats()).thenReturn(4);

        when(driver.getFirstName()).thenReturn("John");
        when(driver.getLastName()).thenReturn("Smith");

        SearchRideResponse response =
                SearchRideResponse.from(ride, driver, 3);

        assertEquals(15, response.rideId());
        assertEquals("John Smith", response.driver());
        assertEquals("Montreal", response.origin());
        assertEquals("Ottawa", response.destination());
        assertEquals("2026-11-15 09:00",
                response.departureDateTime());
        assertEquals(new BigDecimal("20.00"),
                response.pricePerSeat());
        assertEquals(3, response.availableSeats());
    }

    @Test
    void searchResultDoesNotExposePrivateInformation()
            throws Exception {

        Ride ride = mock(Ride.class);
        Person driver = mock(Person.class);

        when(ride.getId()).thenReturn(15);
        when(ride.getOrigin()).thenReturn("Montreal");
        when(ride.getDestination()).thenReturn("Ottawa");
        when(ride.getDepartureDateTime()).thenReturn(
                LocalDateTime.of(2026, 11, 15, 9, 0));
        when(ride.getPricePerSeat()).thenReturn(
                new BigDecimal("20.00"));
        when(ride.getTotalSeats()).thenReturn(4);

        when(driver.getFirstName()).thenReturn("John");
        when(driver.getLastName()).thenReturn("Smith");

        SearchRideResponse response =
                SearchRideResponse.from(ride, driver, 3);

        JsonNode json = mapper.readTree(
                mapper.writeValueAsString(response));

        assertEquals(7, json.size());

        assertEquals(15, json.get("ride_id").asInt());
        assertEquals("John Smith", json.get("driver").asText());
        assertEquals(3, json.get("available_seats").asInt());

        assertFalse(json.has("email"));
        assertFalse(json.has("phoneNumber"));
        assertFalse(json.has("password"));
        assertFalse(json.has("studentID"));
        assertFalse(json.has("licenseNumber"));
        assertFalse(json.has("driverId"));
    }

    @Test
    void invalidAvailableSeatsAreRejected() {

        Ride ride = mock(Ride.class);
        Person driver = mock(Person.class);

        when(ride.getTotalSeats()).thenReturn(4);

        assertThrows(IllegalArgumentException.class,
                () -> SearchRideResponse.from(
                        ride, driver, -1));

        assertThrows(IllegalArgumentException.class,
                () -> SearchRideResponse.from(
                        ride, driver, 5));
    }

    @Test
    void nullRideOrDriverIsRejected() {

        Ride ride = mock(Ride.class);
        Person driver = mock(Person.class);

        assertThrows(IllegalArgumentException.class,
                () -> SearchRideResponse.from(
                        null, driver, 1));

        assertThrows(IllegalArgumentException.class,
                () -> SearchRideResponse.from(
                        ride, null, 1));
    }
}
