package ca.mcgill.ecse428.UniRide_Backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.mcgill.ecse428.UniRide_Backend.model.Passenger;

public interface PassengerRepository extends JpaRepository<Passenger, Integer> {

    /** The Passenger role assigned to the person with this email. */
    @Query("""
            select p from Passenger p
            where p.id in (
                select a.role.id from PersonRoleAssignment a
                where lower(a.person.email) = lower(:email)
            )
            """)
    Optional<Passenger> findByPersonEmail(@Param("email") String email);
}