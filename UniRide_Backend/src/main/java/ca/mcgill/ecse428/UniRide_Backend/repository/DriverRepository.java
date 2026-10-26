package ca.mcgill.ecse428.UniRide_Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.mcgill.ecse428.UniRide_Backend.model.Driver;

public interface DriverRepository extends JpaRepository<Driver, Integer> {

    boolean existsByLicenseNumber(String licenseNumber);
}
