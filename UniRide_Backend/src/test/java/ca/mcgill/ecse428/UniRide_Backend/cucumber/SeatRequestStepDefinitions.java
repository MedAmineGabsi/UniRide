// TID076
package ca.mcgill.ecse428.UniRide_Backend.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import ca.mcgill.ecse428.UniRide_Backend.controller.SeatRequestController;
import ca.mcgill.ecse428.UniRide_Backend.dto.SeatRequestDto;
import ca.mcgill.ecse428.UniRide_Backend.exception.UniRideException;
import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;
import ca.mcgill.ecse428.UniRide_Backend.repository.PassengerRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.RideRepository;
import ca.mcgill.ecse428.UniRide_Backend.repository.SeatRequestRepository;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class SeatRequestStepDefinitions {

    @Autowired
    private SeatRequestController seatRequestController;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private SeatRequestRepository seatRequestRepository;

    private ResponseEntity<?> response;
    private UniRideException caughtException;

    @Given("the current date is {string}")
    public void the_current_date_is(String date) {
        
    }

    @Given("the following drivers exist:")
    public void the_following_drivers_exist(DataTable dataTable) {
        
    }

    @Given("the following passengers exist:")
    public void the_following_passengers_exist(DataTable dataTable) {
        
    }

    @Given("the following rides exist:")
    public void the_following_rides_exist(DataTable dataTable) {
        
    }

    @When("passenger {string} requests {int} seats on ride {int}")
    public void passenger_requests_seats_on_ride(String email, int seats, int rideId) {
        SeatRequestDto.Request request = new SeatRequestDto.Request();
        request.setRideId(rideId);
        request.setSeatsRequested(seats);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("email")).thenReturn(email);

        try {
            response = seatRequestController.createSeatRequest(jwt, request);
        } catch (UniRideException e) {
            caughtException = e;
        }
    }

    @Then("a seat request is created for passenger {string} on ride {int} for {int} seats with status {string}")
    public void a_seat_request_is_created(String email, int rideId, int seats, String status) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        Passenger passenger = passengerRepository.findByPersonEmail(email).orElseThrow();
        
        Iterable<SeatRequest> requests = seatRequestRepository.findAll();
        boolean found = false;
        for (SeatRequest req : requests) {
            if (req.getPassenger().getId().equals(passenger.getId()) &&
                req.getRide().getId().equals(rideId) &&
                req.getSeatsRequested() == seats &&
                req.getStatus().name().equalsIgnoreCase(status)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Given("passenger {string} already has a seat request on ride {int} with status {string}")
    public void passenger_already_has_seat_request(String email, int rideId, String status) {
        Passenger passenger = passengerRepository.findByPersonEmail(email).orElseThrow();
        Ride ride = rideRepository.findById(rideId).orElseThrow();
        
        SeatRequest request = new SeatRequest(passenger, ride, 1);
        request.setStatus(SeatRequestStatus.valueOf(status.toUpperCase()));
        seatRequestRepository.save(request);
    }

    @Then("no seat request is created")
    public void no_seat_request_is_created() {
        assertTrue(caughtException != null || response.getStatusCode() != HttpStatus.CREATED);
    }
    @Then("no additional seat request is created")
    public void no_additional_seat_request_is_created() {
        assertTrue(caughtException != null || response.getStatusCode() != HttpStatus.CREATED);
    }
}