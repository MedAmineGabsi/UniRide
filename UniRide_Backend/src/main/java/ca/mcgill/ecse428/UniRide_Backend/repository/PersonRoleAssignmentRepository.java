package ca.mcgill.ecse428.UniRide_Backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.mcgill.ecse428.UniRide_Backend.model.PersonRole;
import ca.mcgill.ecse428.UniRide_Backend.model.PersonRoleAssignment;

public interface PersonRoleAssignmentRepository extends JpaRepository<PersonRoleAssignment, Integer> {

    @Query("select a.role from PersonRoleAssignment a where a.person.id = :personId")
    List<PersonRole> findRolesByPersonId(@Param("personId") Integer personId);
}
