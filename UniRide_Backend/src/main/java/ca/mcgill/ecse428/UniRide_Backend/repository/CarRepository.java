package ca.mcgill.ecse428.UniRide_Backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.mcgill.ecse428.UniRide_Backend.model.Car;

public interface CarRepository extends JpaRepository<Car, Integer> {

    /** The car of the driver with this role id, if they have registered one. */
    Optional<Car> findByDriverId(Integer driverId);

    /** A driver can only have one car. */
    boolean existsByDriverId(Integer driverId);

    /**
     * A license plate is unique, ignoring case, spaces and hyphens.
     *
     * @param key plate from {@code CarValidator.licensePlateKey} (uppercase, no spaces or hyphens)
     */
    @Query("""
            select count(c) > 0 from Car c
            where upper(replace(replace(c.licensePlate, ' ', ''), '-', '')) = :key
            """)
    boolean existsByLicensePlateKey(@Param("key") String key);
}
