package ca.mcgill.ecse428.UniRide_Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.mcgill.ecse428.UniRide_Backend.model.PersonRole;

public interface PersonRoleRepository extends JpaRepository<PersonRole, Integer> {
}
