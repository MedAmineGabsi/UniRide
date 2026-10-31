package ca.mcgill.ecse428.UniRide_Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;

/** Ride persistence. {@code save(ride)} is the save_ride operation. */
public interface RideRepository extends JpaRepository<Ride, Integer> {
}
