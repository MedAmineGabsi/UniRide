package ca.mcgill.ecse428.UniRide_Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;

/**
 * Ride persistence. {@code save(ride)} is the save_ride operation; ride search runs through
 * {@code findAll(RideSpecifications.openRidesMatching(...), RideSpecifications.BY_DEPARTURE_TIME)}.
 */
public interface RideRepository extends JpaRepository<Ride, Integer>, JpaSpecificationExecutor<Ride> {
}
