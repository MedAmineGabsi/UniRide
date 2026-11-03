package ca.mcgill.ecse428.UniRide_Backend.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;
import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequest;
import ca.mcgill.ecse428.UniRide_Backend.model.SeatRequestStatus;

public interface SeatRequestRepository extends CrudRepository<SeatRequest, Integer> {

    // TID073: Implement the seat_request repository
    boolean existsByPassengerAndRide(Passenger passenger, Ride ride);

    List<SeatRequest> findByRideAndStatus(Ride ride, SeatRequestStatus status);
}   