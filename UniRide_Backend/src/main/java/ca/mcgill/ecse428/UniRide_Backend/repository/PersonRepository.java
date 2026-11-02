package ca.mcgill.ecse428.UniRide_Backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.mcgill.ecse428.UniRide_Backend.model.Person;

public interface PersonRepository extends JpaRepository<Person, Integer> {

    Optional<Person> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
