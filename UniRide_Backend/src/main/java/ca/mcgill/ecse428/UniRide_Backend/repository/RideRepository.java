package ca.mcgill.ecse428.UniRide_Backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import jakarta.persistence.LockModeType;

/**
 * Ride persistence. {@code save(ride)} is the save_ride operation; ride search runs through
 * {@code findAll(RideSpecifications.openRidesMatching(...), RideSpecifications.BY_DEPARTURE_TIME)}.
 */
public interface RideRepository extends JpaRepository<Ride, Integer>, JpaSpecificationExecutor<Ride> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ride from Ride ride where ride.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") Integer id);
}
