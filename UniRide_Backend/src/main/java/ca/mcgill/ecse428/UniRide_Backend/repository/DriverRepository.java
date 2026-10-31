package ca.mcgill.ecse428.UniRide_Backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.mcgill.ecse428.UniRide_Backend.model.Driver;

public interface DriverRepository extends JpaRepository<Driver, Integer> {

    /** The Driver role assigned to the person with this email, if they have one. */
    @Query("""
            select d from Driver d
            where d.id in (
                select a.role.id from PersonRoleAssignment a
                where lower(a.person.email) = lower(:email)
            )
            """)
    Optional<Driver> findByPersonEmail(@Param("email") String email);
}
